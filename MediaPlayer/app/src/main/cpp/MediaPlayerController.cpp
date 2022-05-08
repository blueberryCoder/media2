//
// Created by bytedance on 2022/5/1.
//

#include "MediaPlayerController.h"
#include <pthread.h>
#include "AudioFrame.h"
#include "logger.h"
#include "GLRender.h"
#include <chrono>
#include <thread>

MediaPlayerController::MediaPlayerController() {
    silent_buf_[10] = {};
}

MediaPlayerController::~MediaPlayerController() {
    AAsset_close(audio_asset_);
    AAsset_close(video_asset_);
    this->status_ = DESTROYED;
    AMediaCodec_delete(audioCodec);
    AMediaCodec_delete(videoCodec);
    AMediaExtractor_delete(audioExtractor);
    AMediaExtractor_delete(videoExtractor);
}

void MediaPlayerController::start() {
    SLuint32 playState;
    (*sl_player_itf_)->GetPlayState(sl_player_itf_, &playState);

    if (playState == SL_PLAYSTATE_PLAYING) {
        return;
    } else if (playState == SL_PLAYSTATE_STOPPED) {
        pthread_create(&this->video_render_thread_, nullptr,
                       &MediaPlayerController::wrapperVideoRenderThreadRun, this);

        pthread_create(&this->video_decoder_thread_, nullptr,
                       &MediaPlayerController::wrapperVideoDecoderThreadRun, this);


        pthread_create(&this->audio_decoder_thread_, nullptr,
                       &MediaPlayerController::wrapperAudioDecoderThreadRun, this);
        (*sl_player_itf_)->SetPlayState(sl_player_itf_, SL_PLAYSTATE_PLAYING);
        (*sl_android_simple_buffer_queue_itf_)->Enqueue(sl_android_simple_buffer_queue_itf_,
                                                        silent_buf_, 10);

    } else if (playState == SL_PLAYSTATE_PAUSED) {
        (*sl_player_itf_)->SetPlayState(sl_player_itf_, SL_PLAYSTATE_PLAYING);
    }
}

void MediaPlayerController::pause() {
    SLuint32 playState;
    (*sl_player_itf_)->GetPlayState(sl_player_itf_, &playState);
    if (playState != SL_PLAYSTATE_PLAYING) {
        return;
    }
    (*sl_player_itf_)->SetPlayState(sl_player_itf_, SL_PLAYSTATE_PAUSED);
}

void MediaPlayerController::stop() {
    SLuint32 playState;
    (*sl_player_itf_)->GetPlayState(sl_player_itf_, &playState);
    if (playState == SL_PLAYSTATE_STOPPED) {
        return;
    }
    (*sl_player_itf_)->SetPlayState(sl_player_itf_, SL_PLAYSTATE_STOPPED);
}

void MediaPlayerController::androidSimpleBufferQueueCallback(
        SLAndroidSimpleBufferQueueItf caller) {

    LOGI("audio opensl callback invoke.");
    auto audio_frame = avSync_.dequeueAudioFrame();
    (*caller)->Enqueue(
            caller,
            audio_frame.getBuf(),
            audio_frame.size_);
    static uint8_t *last_buf = nullptr;
    if (last_buf) {
        delete last_buf;
    }
    last_buf = audio_frame.getBuf();
}

void *MediaPlayerController::audioDecoderThreadRun() {
    attachEnv();
    AMediaCodec_start(audioCodec);
    AMediaExtractor_selectTrack(audioExtractor, 1);
    while (this->status_) {
        decodeAudioPacket();
    }
    detachEnv();
    return 0;
}

void MediaPlayerController::decodeAudioPacket() {
    if (!audio_decode_input_end_) {
        auto index = AMediaCodec_dequeueInputBuffer(audioCodec, TIMEOUT);
        if (index >= 0) {
            size_t size = 0;
            auto input_buffer = AMediaCodec_getInputBuffer(audioCodec, index, &size);
            auto sample_size = AMediaExtractor_readSampleData(audioExtractor, input_buffer, size);
            auto trackIndex = AMediaExtractor_getSampleTrackIndex(audioExtractor);
            if (trackIndex == 1) {
                if (sample_size <= 0) {
                    LOGE("audio read complete: index = %d,sample_size=%d", index, sample_size);
                    // input is end
                    sample_size = 0;
                    audio_decode_input_end_ = true;
                }
                auto pts = AMediaExtractor_getSampleTime(audioExtractor);
                AMediaCodec_queueInputBuffer(audioCodec,
                                             index,
                                             0,
                                             sample_size,
                                             pts,
                                             audio_decode_input_end_
                                             ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM : 0
                );
                AMediaExtractor_advance(audioExtractor);
            } else {
                LOGI("audio read sample track index is wrong track index is %ld", trackIndex);
            }
        }
    }
    // decode
    if (!audio_decode_output_end_) {
        AMediaCodecBufferInfo output_buffer_info_;
        auto idx = AMediaCodec_dequeueOutputBuffer(audioCodec, &output_buffer_info_, TIMEOUT);
        if (idx >= 0) {
            if (output_buffer_info_.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                audio_decode_output_end_ = true;
            }
            size_t size = 0;
            auto output_buffer = AMediaCodec_getOutputBuffer(audioCodec, idx, &size);
            if (output_buffer_info_.size > 0 && output_buffer) {
                auto audioFrame = AudioFrame(output_buffer_info_.presentationTimeUs,
                                             output_buffer_info_.size,
                                             &output_buffer[output_buffer_info_.offset]);
                avSync_.enqueueAudioFrame(audioFrame);
            }
            AMediaCodec_releaseOutputBuffer(audioCodec, idx, false);
        } else {
            switch (idx) {
                case AMEDIACODEC_INFO_TRY_AGAIN_LATER: {
                    break;
                }
                case AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED  : {
                    auto format = AMediaCodec_getOutputFormat(audioCodec);
                    const char *strFormat = AMediaFormat_toString(format);
                    AMediaFormat_delete(format);
                    break;
                }
                case AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED: {
                    LOGI("audio decoder buffers changed.");
                    break;
                }
                default: {
                    LOGI("audio decoder default error .");
                    break;
                }
            }
        }
    }
}

void *MediaPlayerController::videoDecoderThreadRun() {
    attachEnv();
    AMediaCodec_start(videoCodec);
    AMediaExtractor_selectTrack(videoExtractor, 0);
    while (this->status_) {
        decodeVideoPacket();
    }
    detachEnv();
    return 0;
}

void MediaPlayerController::decodeVideoPacket() {
    if (!video_decode_input_end_) {
        auto index = AMediaCodec_dequeueInputBuffer(videoCodec, TIMEOUT);
        if (index >= 0) {
            size_t size = 0;
            auto input_buffer = AMediaCodec_getInputBuffer(videoCodec, index, &size);
            // 1. send sps
            if (!is_send_pps && videoFormat.sps_size_ > 0) {
                memcpy(input_buffer, videoFormat.sps_, videoFormat.sps_size_);
                AMediaCodec_queueInputBuffer(videoCodec,
                                             index,
                                             0,
                                             videoFormat.sps_size_,
                                             0,
                                             AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG
                );
                is_send_pps = true;
            } else if (!is_send_sps && videoFormat.pps_size_ > 0) {
                // 2. send pps
                memcpy(input_buffer, videoFormat.pps_, videoFormat.pps_size_);
                AMediaCodec_queueInputBuffer(videoCodec,
                                             index,
                                             0,
                                             videoFormat.pps_size_,
                                             0,
                                             AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG
                );
                is_send_sps = true;
            } else {
                auto sample_size = AMediaExtractor_readSampleData(videoExtractor, input_buffer,
                                                                  size);
                auto pts = AMediaExtractor_getSampleTime(videoExtractor);
                if (sample_size <= 0) {
                    LOGE("video read complete: index = %d,sample_size=%d,buffer size=%zu,pts=%lld",
                         index, sample_size, size, pts);
                    sample_size = 0;
                    video_decode_input_end_ = true;
                }
                AMediaCodec_queueInputBuffer(videoCodec,
                                             index,
                                             0,
                                             sample_size,
                                             pts,
                                             video_decode_input_end_
                                             ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM :
                                             0
                );
                if (video_decode_input_end_) {
                    AMediaCodec_flush(videoCodec);
                } else {
                    auto more = AMediaExtractor_advance(videoExtractor);
                    if (!more) {
                        LOGI("video decoder advance no more.");
                    }
                }
            }
        }
    }
    // decode
    if (!video_decode_out_end_) {
        AMediaCodecBufferInfo output_buffer_info_;
        auto idx = AMediaCodec_dequeueOutputBuffer(videoCodec, &output_buffer_info_, TIMEOUT);
        if (idx >= 0) {
            if (output_buffer_info_.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                video_decode_out_end_ = true;
            }
            size_t size = 0;
            auto output_buffer = AMediaCodec_getOutputBuffer(videoCodec, idx, &size);
            if (output_buffer_info_.size >= 0) {
                // pts
                auto video_frame = VideoFrame(output_buffer_info_.presentationTimeUs,
                                              output_buffer_info_.size,
                                              &output_buffer[output_buffer_info_.offset],
                                              videoFormat.width_,
                                              videoFormat.height_,
                                              videoFormat.color_format_
                );
                avSync_.enqueueVideoFrame(video_frame);
            }
            AMediaCodec_releaseOutputBuffer(videoCodec, idx, false);
        } else {
            switch (idx) {
                case AMEDIACODEC_INFO_TRY_AGAIN_LATER: {
                    break;
                }
                case AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED  : {
                    auto format = AMediaCodec_getOutputFormat(videoCodec);
                    const char *strFormat = AMediaFormat_toString(format);
                    // https://developer.android.com/reference/android/media/MediaFormat#KEY_COLOR_FORMAT
                    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT,
                                          &this->videoFormat.color_format_);

                    AMediaFormat_getBuffer(format, "csd-0",
                                           reinterpret_cast<void **>(&(videoFormat.sps_)),
                                           &videoFormat.sps_size_);

                    AMediaFormat_getBuffer(format, "csd-1",
                                           reinterpret_cast<void **>(&(videoFormat.pps_)),
                                           &videoFormat.pps_size_);

                    AMediaFormat_delete(format);
                    break;
                }
                case AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED: {
                    LOGI("video decoder : AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                }
                default: {
                    LOGE("video decoder get output %d", idx);
                    break;
                }
            }
        }
    }
}

void *MediaPlayerController::videoRenderThreadRun() {
    attachEnv();
    eglMakeCurrent(this->eglDisplay, this->eglSurface, this->eglSurface, this->eglContext);
    GLRender render;
    GLuint glProgram = render.createGlProgram();
    glClearColor(0, 0, 0, 1.0);

    render.checkGlError("clearColor");
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    render.checkGlError("clearGLError");
    glUseProgram(glProgram);  // 激活渲染程序

    render.checkGlError("useProgram");
    GLuint glPosition = glGetAttribLocation(glProgram, "vPosition");
    glEnableVertexAttribArray(glPosition);
    glVertexAttribPointer(glPosition, 2, GL_FLOAT, GL_FALSE, 0, render.getData());

    // 纹理坐标
    static float fragment[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };

    GLuint aTex = glGetAttribLocation(glProgram, "aTextCoord");
    glEnableVertexAttribArray(aTex);
    glVertexAttribPointer(aTex, 2, GL_FLOAT, GL_FALSE, 0, fragment);

    render.checkGlError("enable aTextCoord");
    int width = 1000;
    int height = 416;
    glViewport(0, 200, width, height);

    glUniform1i(glGetUniformLocation(glProgram, "yTexture"), 0);
    glUniform1i(glGetUniformLocation(glProgram, "uTexture"), 1);
    glUniform1i(glGetUniformLocation(glProgram, "vTexture"), 2);
    GLuint texts[3] = {0};
    glGenTextures(3, texts);

    // y
    glBindTexture(GL_TEXTURE_2D, texts[0]);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE,
                 NULL);
    // u
    glBindTexture(GL_TEXTURE_2D, texts[1]);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width / 2, height / 2, 0, GL_LUMINANCE,
                 GL_UNSIGNED_BYTE, NULL);
    // v
    glBindTexture(GL_TEXTURE_2D, texts[2]);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width / 2, height / 2, 0, GL_LUMINANCE,
                 GL_UNSIGNED_BYTE, NULL);

    while (status_) {
        auto videoFrame = this->avSync_.dequeueVideoFrame();
        glActiveTexture(GL_TEXTURE0);
        render.checkGlError("glActiveTexture Y");
        glBindTexture(GL_TEXTURE_2D, texts[0]);
        glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0,//相对原来的纹理的offset
                        width, height,//加载的纹理宽度、高度。最好为2的次幂
                        GL_LUMINANCE, GL_UNSIGNED_BYTE,
                        &videoFrame.buf_[0]);


        // u
        glActiveTexture(GL_TEXTURE1);
        render.checkGlError("glActiveTexture U");
        glBindTexture(GL_TEXTURE_2D, texts[1]);
        //替换纹理，比重新使用glTexImage2D性能高多
        glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0,//相对原来的纹理的offset
                        width / 2, height / 2,//加载的纹理宽度、高度。最好为2的次幂
                        GL_LUMINANCE, GL_UNSIGNED_BYTE,
                        &videoFrame.buf_[width * height]);

        // v
        glActiveTexture(GL_TEXTURE2);
        render.checkGlError("glActiveTexture v");
        glBindTexture(GL_TEXTURE_2D, texts[2]);
        //替换纹理，比重新使用glTexImage2D性能高多
        glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0,//相对原来的纹理的offset
                        width / 2, height / 2,//加载的纹理宽度、高度。最好为2的次幂
                        GL_LUMINANCE, GL_UNSIGNED_BYTE,
                        &videoFrame.buf_[width * height + width * height / 4]);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        eglSwapBuffers(this->eglDisplay, this->eglSurface);
        // delete [] buf
        delete[] videoFrame.buf_;
    }
    detachEnv();
    return 0;
}

void MediaPlayerController::detachEnv() {
    javaVm_->DetachCurrentThread();
}

void MediaPlayerController::attachEnv() {
    JNIEnv *env = nullptr;
    javaVm_->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    javaVm_->AttachCurrentThread(&env, nullptr);
}
