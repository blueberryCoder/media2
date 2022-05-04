//
// Created by bytedance on 2022/4/10.
//
#include <jni.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaFormat.h>
#include <media/NdkMediaCodec.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES.h>
#include <pthread.h>
#include <unistd.h>
#include <chrono>
#include <iostream>
#include <fstream>
#include <ios>
#include <string.h>
#include <cassert>
#include <time.h>

#include "logger.h"
#include "safe_queue.h"

extern "C" {

#define TIMEOUT_US 2000

void bufferQueueCallback(SLAndroidSimpleBufferQueueItf androidSimpleBufferQueueItf, void *context);


struct MetaData {
    int64_t duration_;
    int32_t channelCount_;
    int32_t sampleRate_;
    int32_t bitrate_;

};
enum Status {

    STARTED = 0,
    PAUSED = 1,
    STOPPED = 2,
    DESTROYED = 3
};

struct MyBuffer {
    uint8_t *buf_ = nullptr;
    size_t len_ = 0;
};

struct AudioPlayerEngine {
    JavaVM *javaVm;
    AMediaExtractor *mediaExtractor_ = nullptr;
    AMediaCodec *codec_ = nullptr;
    AAsset *asset_ = nullptr;
    SLObjectItf slEngineObj_;
    SLEngineItf slEngineItf_;
    SLObjectItf slPlayerObj_;
    SLPlayItf slPlayItf_;
    SLAndroidSimpleBufferQueueItf androidSimpleBufferQueueItf_;

    MetaData metaData_;
    pthread_t thread_;
    int status_ = STOPPED;
    bool inputEnd = false;
    bool outputEnd = false;
    std::ofstream pcmFile_;

    uint8_t *silent_buf_ = nullptr;
    size_t silent_buf_size_ = 0;
    SafeQueue<MyBuffer> *buffer_queue_ = nullptr;


    ~AudioPlayerEngine() {
        AAsset_close(asset_);
        AMediaExtractor_delete(mediaExtractor_);
    }
};

long currentTime() {
    time_t current_time;
    time(&current_time);
    return current_time;
}

void *threadRun(void *engine_) {
    auto engine = (AudioPlayerEngine *) engine_;
    JNIEnv *env;
    engine->javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    engine->javaVm->AttachCurrentThread(&env, nullptr);
    while (engine->status_ != DESTROYED) {
        if (engine->status_ == Status::STARTED) {
            AMediaCodec_start(engine->codec_);
            while (engine->status_ == STARTED) {
                if (!engine->inputEnd) {
                    auto index = AMediaCodec_dequeueInputBuffer(engine->codec_, TIMEOUT_US);
                    if (index >= 0) {
                        size_t output_size = 0;
                        auto buffer = AMediaCodec_getInputBuffer(engine->codec_, index,
                                                                 &output_size);
                        auto sampleSize = AMediaExtractor_readSampleData(engine->mediaExtractor_,
                                                                         buffer,
                                                                         output_size);
                        if (sampleSize <= 0) {
                            sampleSize = 0;
                            engine->inputEnd = true;
                        }
                        auto presentationTimeUs = AMediaExtractor_getSampleTime(
                                engine->mediaExtractor_);

                        auto status = AMediaCodec_queueInputBuffer(engine->codec_,
                                                                   index,
                                                                   0,
                                                                   sampleSize,
                                                                   presentationTimeUs,
                                                                   engine->inputEnd
                                                                   ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM
                                                                   : 0);

                        if (status != AMEDIA_OK) {
                            LOGD("status:%d", status);
                            break;
                        }
                        AMediaExtractor_advance(engine->mediaExtractor_);
                    }
                }

                // decode
                while (!engine->outputEnd) {
                    AMediaCodecBufferInfo bufferInfo;
                    auto index = AMediaCodec_dequeueOutputBuffer(engine->codec_, &bufferInfo,
                                                                 TIMEOUT_US);
                    if (index >= 0) {
                        LOGD("process output index >=0,%d", index);
                        if (bufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                            engine->outputEnd = true;
                        }
                        size_t buffer_size = 0;
                        auto buffer = AMediaCodec_getOutputBuffer(engine->codec_, index,
                                                                  &buffer_size);

                        if (bufferInfo.size > 0) {
                            auto buf = new uint8_t[bufferInfo.size];
                            memcpy(buf, &buffer[bufferInfo.offset], bufferInfo.size);
                            MyBuffer myBuffer;
                            myBuffer.len_ = bufferInfo.size;
                            myBuffer.buf_ = buf;
                            engine->buffer_queue_->enqueue(myBuffer);
                        }
                        AMediaCodec_releaseOutputBuffer(engine->codec_, index, false);
                        LOGD("bufferQueueCallback:index >=0 ");
                        break;
                    } else {
                        switch (index) {
                            case AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED  : {
                                auto format = AMediaCodec_getOutputFormat(engine->codec_);
                                const char *strFormat = AMediaFormat_toString(format);
                                const char *pcmEncoding = nullptr;
                                AMediaFormat_delete(format);
                                break;
                            }
                            case AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED: {
                                break;
                            }
                            case AMEDIACODEC_INFO_TRY_AGAIN_LATER: {
                            }
                            default: {
                                break;
                            }
                        }
                    }
                } // output process end .othing
            }
        } else if (engine->status_ == Status::PAUSED) {
        } else if (engine->status_ == Status::STOPPED) {
        }
        // wait for changed;
    }
    engine->javaVm->DetachCurrentThread();
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLMediaCodecAudio_initialize(JNIEnv *env, jobject thiz,
                                                            jobject assetManager,
                                                            jstring res_path) {

    auto engine = new AudioPlayerEngine();
    env->GetJavaVM(&engine->javaVm);
    engine->buffer_queue_ = new SafeQueue<MyBuffer>();
    const char *fileName = env->GetStringUTFChars(res_path, NULL);
    auto cAssetManager = AAssetManager_fromJava(env, assetManager);
    engine->asset_ = AAssetManager_open(cAssetManager, fileName, AASSET_MODE_RANDOM);
    off_t start = 0, len = 0;
    auto fd = AAsset_openFileDescriptor(engine->asset_, &start, &len);

    engine->mediaExtractor_ = AMediaExtractor_new();
    AMediaExtractor_setDataSourceFd(engine->mediaExtractor_, fd, start, len);
    AMediaExtractor_getTrackCount(engine->mediaExtractor_);

    // because i am sure that my resource is only one track.
    auto format = AMediaExtractor_getTrackFormat(engine->mediaExtractor_, 0);
    const char *mime = nullptr;
    AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, reinterpret_cast<const char **>(&mime));
    AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &engine->metaData_.duration_);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &engine->metaData_.channelCount_);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &engine->metaData_.sampleRate_);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, &engine->metaData_.bitrate_);

    const char *formatStr = AMediaFormat_toString(format);

    LOGD("the file format is %s", formatStr);
    AMediaExtractor_selectTrack(engine->mediaExtractor_, 0);

    // create decode codec .
    engine->codec_ = AMediaCodec_createDecoderByType(mime);
    AMediaCodec_configure(engine->codec_, format, nullptr, nullptr, 0);

    // create opengl es.
    SLresult result = slCreateEngine(&engine->slEngineObj_, 0, nullptr, 0, nullptr, nullptr);
    assert(result == SL_RESULT_SUCCESS);

    result = (*engine->slEngineObj_)->Realize(engine->slEngineObj_, false);
    assert(result == SL_RESULT_SUCCESS);
    result = (*engine->slEngineObj_)->GetInterface(engine->slEngineObj_, SL_IID_ENGINE,
                                                   &engine->slEngineItf_);
    assert(result == SL_RESULT_SUCCESS);

    SLAndroidDataFormat_PCM_EX_ sourceFormat{
            .formatType = SL_DATAFORMAT_PCM,
            .numChannels = static_cast<SLuint32>(engine->metaData_.channelCount_),
            // millherzt
            .sampleRate = static_cast<SLuint32>(engine->metaData_.sampleRate_) * 1000,
            .bitsPerSample =SL_PCMSAMPLEFORMAT_FIXED_16,
            .containerSize = SL_PCMSAMPLEFORMAT_FIXED_16,
            .channelMask =  SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            .endianness = SL_BYTEORDER_LITTLEENDIAN,
            .representation = SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT
    };

    engine->silent_buf_ = new uint8_t[2 * 2 * 80]; // 80 frame per buf.
    engine->silent_buf_size_ = 2 * 2 * 80;
    SLDataLocator_AndroidSimpleBufferQueue androidSimpleBufferQueueLocator{
            .locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            .numBuffers = 4
    };

    SLDataSource slDataSource{
            .pLocator = &androidSimpleBufferQueueLocator,
            .pFormat = &sourceFormat,
    };

    SLObjectItf outputMixObj;
    result = (*engine->slEngineItf_)->CreateOutputMix(engine->slEngineItf_,
                                                      &outputMixObj,
                                                      0,
                                                      nullptr,
                                                      nullptr);
    assert(result == SL_RESULT_SUCCESS);
    result = (*outputMixObj)->Realize(outputMixObj, false);
    assert(result == SL_RESULT_SUCCESS);

    SLDataLocator_OutputMix outputMixLocator{
            .locatorType = SL_DATALOCATOR_OUTPUTMIX,
            .outputMix = outputMixObj,
    };
    SLDataSink slDataSink{
            .pLocator = &outputMixLocator,
            .pFormat =  nullptr,
    };

    SLInterfaceID interfaces[2]{
            SL_IID_VOLUME,
            SL_IID_BUFFERQUEUE
    };
    SLboolean isRequired[2]{
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE
    };
    result = (*engine->slEngineItf_)->CreateAudioPlayer(engine->slEngineItf_,
                                                        &engine->slPlayerObj_,
                                                        &slDataSource, &slDataSink,
                                                        sizeof(interfaces) /
                                                        sizeof(interfaces[0]),
                                                        interfaces, isRequired);

    logSLresult(result, "CreateAudioPlayer");
    assert(result == SL_RESULT_SUCCESS);

    result = (*engine->slPlayerObj_)->Realize(engine->slPlayerObj_, SL_BOOLEAN_FALSE);

    result = (*engine->slPlayerObj_)->GetInterface(engine->slPlayerObj_, SL_IID_PLAY,
                                                   &engine->slPlayItf_);

    assert(result == SL_RESULT_SUCCESS);
    result = (*engine->slPlayerObj_)->GetInterface(engine->slPlayerObj_,
                                                   SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                                   &engine->androidSimpleBufferQueueItf_);
    assert(result == SL_RESULT_SUCCESS);

    result = (*engine->androidSimpleBufferQueueItf_)->RegisterCallback(
            engine->androidSimpleBufferQueueItf_, bufferQueueCallback, engine);

    assert(result == SL_RESULT_SUCCESS);
    result = (*engine->slPlayItf_)->SetPlayState(engine->slPlayItf_, SL_PLAYSTATE_STOPPED);

    assert(result == SL_RESULT_SUCCESS);
    const char *outputFile = "/sdcard/Android/data/com.blueberry.videoplayer/files/mojito.pcm";
    engine->pcmFile_.open(outputFile, std::ios::ate | std::ios::out);
    if (!engine->pcmFile_.is_open()) {
        engine->pcmFile_.close();
        LOGE("open file error:%s", strerror(errno));
        assert(false && "file is not open .");
    }

    // create thread .
    pthread_create(&engine->thread_, nullptr, threadRun, engine);
    env->ReleaseStringUTFChars(res_path, fileName);
    return reinterpret_cast<long>(engine);
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLMediaCodecAudio_start(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto engine = reinterpret_cast<AudioPlayerEngine * > (c_ptr);
    engine->status_ = Status::STARTED;
    SLuint32 playState;
    (*engine->slPlayItf_)->GetPlayState(engine->slPlayItf_, &playState);
    if (playState != SL_PLAYSTATE_PLAYING) {
        (*engine->slPlayItf_)->SetPlayState(engine->slPlayItf_, SL_PLAYSTATE_PLAYING);
        (*engine->androidSimpleBufferQueueItf_)->Enqueue(engine->androidSimpleBufferQueueItf_,
                                                         engine->silent_buf_,
                                                         engine->silent_buf_size_);
    }
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLMediaCodecAudio_pause(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto engine = reinterpret_cast<AudioPlayerEngine * > (c_ptr);
    engine->status_ = Status::PAUSED;
    SLuint32 playState;
    (*engine->slPlayItf_)->GetPlayState(engine->slPlayItf_, &playState);
    if (playState == SL_PLAYSTATE_STOPPED) {
        return;
    }
    if (playState == SL_PLAYSTATE_PAUSED) {
        (*engine->slPlayItf_)->SetPlayState(engine->slPlayItf_, SL_PLAYSTATE_PLAYING);
    } else {
        (*engine->slPlayItf_)->SetPlayState(engine->slPlayItf_, SL_PLAYSTATE_PAUSED);
    }

}
JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLMediaCodecAudio_stop(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto engine = reinterpret_cast<AudioPlayerEngine * > (c_ptr);
    engine->status_ = Status::STOPPED;
    SLuint32 playState;
    (*engine->slPlayItf_)->GetPlayState(engine->slPlayItf_, &playState);
    if (playState == SL_PLAYSTATE_STOPPED) {
        return;
    }
    (*engine->slPlayItf_)->SetPlayState(engine->slPlayItf_, SL_PLAYSTATE_STOPPED);
}

void bufferQueueCallback(SLAndroidSimpleBufferQueueItf androidSimpleBufferQueueItf, void *context) {
    auto engine = reinterpret_cast<AudioPlayerEngine * >(context);

    auto queue = engine->buffer_queue_;
    auto myBuffer = queue->dequeue();
    if (myBuffer.buf_ != nullptr) {
        LOGD("enqueue buffer to opensl ");
        (*engine->androidSimpleBufferQueueItf_)->Enqueue(
                engine->androidSimpleBufferQueueItf_,
                myBuffer.buf_,
                myBuffer.len_);
    } else {
        (*engine->androidSimpleBufferQueueItf_)->Enqueue(
                engine->androidSimpleBufferQueueItf_,
                engine->silent_buf_,
                1);
    }
    return;
}

}