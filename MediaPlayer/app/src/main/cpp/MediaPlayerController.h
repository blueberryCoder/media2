//
// Created by bytedance on 2022/5/1.
//

#ifndef MEDIAPLAYER_MEDIAPLAYERCONTROLLER_H
#define MEDIAPLAYER_MEDIAPLAYERCONTROLLER_H

#include <jni.h>
#include <android/asset_manager.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES.h>
#include <pthread.h>
#include <fstream>

#include "MyFormat.h"
#include "safe_queue.h"
#include "AudioFrame.h"
#include "VideoFrame.h"
#include "AVSync.h"

class MediaPlayerController {

public:
    AAsset *audio_asset_ = nullptr;
    AAsset *video_asset_ = nullptr;
    AMediaExtractor *audioExtractor = nullptr;
    AMediaExtractor *videoExtractor = nullptr;
    MyFormat audioFormat;
    MyFormat videoFormat;
    AMediaCodec *audioCodec = nullptr;
    AMediaCodec *videoCodec = nullptr;
    EGLDisplay eglDisplay = nullptr;
    EGLSurface eglSurface = nullptr;
    EGLContext eglContext = nullptr;

    SLObjectItf sl_engine_obj_ = nullptr;
    SLEngineItf sl_engine_itf_ = nullptr;
    SLObjectItf sl_player_obj_ = nullptr;
    SLPlayItf sl_player_itf_ = nullptr;
    SLAndroidSimpleBufferQueueItf sl_android_simple_buffer_queue_itf_ = nullptr;

    AVSync avSync_;
    JavaVM *javaVm_;

    std::ofstream yuv_file_ ;
    std::ofstream h264_file_;

    bool is_send_sps = false, is_send_pps = false;

    static void
    wrapperAndroidSimpleBufferQueueCallback(SLAndroidSimpleBufferQueueItf caller, void *pContext) {
        auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(pContext);
        return mediaPlayerController->androidSimpleBufferQueueCallback(caller);
    }

    static void *wrapperVideoRenderThreadRun(void *context) {
        auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(context);
        return mediaPlayerController->videoRenderThreadRun();
    }

    static void *wrapperVideoDecoderThreadRun(void *context) {
        auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(context);
        return mediaPlayerController->videoDecoderThreadRun();
    }

    static void *wrapperAudioDecoderThreadRun(void *context) {
        auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(context);
        return mediaPlayerController->audioDecoderThreadRun();
    }

    SLAPIENTRY void androidSimpleBufferQueueCallback(SLAndroidSimpleBufferQueueItf caller);

    void *videoRenderThreadRun();

    void *videoDecoderThreadRun();

    void *audioDecoderThreadRun();

    void start();

    void pause();

    void stop();

    MediaPlayerController();

    virtual ~MediaPlayerController();


private:
    pthread_t video_render_thread_;
    pthread_t video_decoder_thread_;
    pthread_t audio_decoder_thread_;

    uint8_t silent_buf_[10];
    bool audio_decode_input_end_ = false;
    bool audio_decode_output_end_ = false;
    bool video_decode_input_end_ = false;
    bool video_decode_out_end_ = false;

    int32_t  status_ = 1;

    static const auto DESTROYED = 0;
    static const auto TIMEOUT = 2000;


    void attachEnv();

    void detachEnv();

    void decodeAudioPacket();

    void decodeVideoPacket();
};


#endif //MEDIAPLAYER_MEDIAPLAYERCONTROLLER_H
