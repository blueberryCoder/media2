//
// Created by bytedance on 2022/5/1.
//
#include <jni.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaFormat.h>
#include <media/NdkMediaCodec.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include "MediaPlayerController.h"
#include "logger.h"
#include <unistd.h>

void
initMediaCodec(MediaPlayerController *mediaPlayerController, off_t start, off_t length, int fd);

void initOpenGl(JNIEnv *env, jobject surface, MediaPlayerController *mediaPlayerController);

extern "C"
JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_MediaPlayerController_initialize(JNIEnv *env, jobject thiz,
                                                                jobject surface,
                                                                jobject asset_manager,
                                                                jstring path) {
    const char *cPath = env->GetStringUTFChars(path, NULL);
    auto *mediaPlayerController = new MediaPlayerController();
    // https://developer.android.com/ndk/reference/group/asset
    auto assetManager = AAssetManager_fromJava(env, asset_manager);
    mediaPlayerController->audio_asset_ = AAssetManager_open(assetManager, cPath, AASSET_MODE_RANDOM);
    mediaPlayerController->video_asset_ = AAssetManager_open(assetManager, cPath, AASSET_MODE_RANDOM);
    off_t audio_start = 0, audio_length = 0;
    auto audio_fd = AAsset_openFileDescriptor(mediaPlayerController->audio_asset_, &audio_start, &audio_length);
    if (audio_fd < 0) {
        LOGE("input file is invalid.");
        assert("assure not here");
    }
    off_t video_fd_start = 0,video_fd_len = 0;
    auto video_fd = AAsset_openFileDescriptor(mediaPlayerController->video_asset_, &video_fd_start, &video_fd_len);

    mediaPlayerController->audioExtractor = AMediaExtractor_new();
    mediaPlayerController->videoExtractor = AMediaExtractor_new();

    AMediaExtractor_setDataSourceFd(mediaPlayerController->audioExtractor, audio_fd, audio_start, audio_length);
    AMediaExtractor_setDataSourceFd(mediaPlayerController->videoExtractor, video_fd, video_fd_start, video_fd_len);

    initMediaCodec(mediaPlayerController, audio_start, audio_length, audio_fd);
    initOpenGl(env, surface, mediaPlayerController);
    // init open sl
    slCreateEngine(&mediaPlayerController->sl_engine_obj_, 0, nullptr,
                   0, nullptr, 0);
    (*mediaPlayerController->sl_engine_obj_)->Realize(mediaPlayerController->sl_engine_obj_, false);
    (*mediaPlayerController->sl_engine_obj_)->GetInterface(
            mediaPlayerController->sl_engine_obj_, SL_IID_ENGINE,
            &mediaPlayerController->sl_engine_itf_);


    SLDataLocator_AndroidSimpleBufferQueue slDataLocatorAndroidSimpleBufferQueue{
            .locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            .numBuffers = 4
    };

    SLAndroidDataFormat_PCM_EX_ pcmDataFormat{

            .formatType = SL_DATAFORMAT_PCM,
            .numChannels = static_cast<SLuint32>(mediaPlayerController->audioFormat.channel_count_),
            // millherzt
            .sampleRate = static_cast<SLuint32>(mediaPlayerController->audioFormat.sample_rate_) *
                          1000,
            .bitsPerSample =SL_PCMSAMPLEFORMAT_FIXED_16,
            .containerSize = SL_PCMSAMPLEFORMAT_FIXED_16,
            .channelMask =  SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            .endianness = SL_BYTEORDER_LITTLEENDIAN,
            .representation = SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT
    };

    SLDataSource slDataSource{
            .pLocator  = &slDataLocatorAndroidSimpleBufferQueue,
            .pFormat = &pcmDataFormat
    };

    SLObjectItf sl_output_mix;
    (*mediaPlayerController->sl_engine_itf_)->CreateOutputMix(mediaPlayerController->sl_engine_itf_,
                                                              &sl_output_mix, 0, nullptr, 0);
    (*sl_output_mix)->Realize(sl_output_mix, false);


    SLDataLocator_OutputMix slDataLocatorOutputMix{
            .locatorType = SL_DATALOCATOR_OUTPUTMIX,
            .outputMix = sl_output_mix
    };
    SLDataSink slDataSink{
            .pLocator = &slDataLocatorOutputMix,
            .pFormat = nullptr
    };
    SLInterfaceID requiredInterfaces[]{
            SL_IID_PLAY,
            SL_IID_BUFFERQUEUE
    };
    SLboolean isRequired[]{
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE
    };
    auto slResult = (*mediaPlayerController->sl_engine_itf_)->CreateAudioPlayer(
            mediaPlayerController->sl_engine_itf_, &mediaPlayerController->sl_player_obj_,
            &slDataSource, &slDataSink,
            sizeof(requiredInterfaces) / sizeof(SLInterfaceID),
            requiredInterfaces, isRequired);
    logSLresult(slResult, "create audio player ");

    assert(slResult == SL_RESULT_SUCCESS);
    slResult = (*mediaPlayerController->sl_player_obj_)->Realize(
            mediaPlayerController->sl_player_obj_, false);
    logSLresult(slResult, "realize player obj");
    assert(slResult == SL_RESULT_SUCCESS);


    (*mediaPlayerController->sl_player_obj_)->GetInterface(
            mediaPlayerController->sl_player_obj_, SL_IID_PLAY,
            &mediaPlayerController->sl_player_itf_);

    (*mediaPlayerController->sl_player_obj_)->GetInterface(mediaPlayerController->sl_player_obj_,
                                                           SL_IID_BUFFERQUEUE,
                                                           &mediaPlayerController->sl_android_simple_buffer_queue_itf_);

    (*mediaPlayerController->sl_android_simple_buffer_queue_itf_)->RegisterCallback(
            mediaPlayerController->sl_android_simple_buffer_queue_itf_,
            &MediaPlayerController::wrapperAndroidSimpleBufferQueueCallback,
            (void *) mediaPlayerController
    );
    (*mediaPlayerController->sl_player_itf_)->SetPlayState(mediaPlayerController->sl_player_itf_,
                                                           SL_PLAYSTATE_STOPPED);
    env->GetJavaVM(&mediaPlayerController->javaVm_);
    env->ReleaseStringUTFChars(path, cPath);
    return reinterpret_cast<long>(mediaPlayerController);
}

void initOpenGl(JNIEnv *env, jobject surface, MediaPlayerController *mediaPlayerController) {
    auto nativeWindow = ANativeWindow_fromSurface(env, surface);
    mediaPlayerController->eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (mediaPlayerController->eglDisplay == EGL_NO_DISPLAY) {
        assert("no display.");
    }
    if (eglInitialize(mediaPlayerController->eglDisplay, 0, 0) != EGL_TRUE) {
        assert("initialize fail.");
    }
    const EGLint atrribs[]{
            EGL_BUFFER_SIZE, 32,
            EGL_ALPHA_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_NONE
    };
    EGLConfig eglConfig;
    EGLint numOfConfigs;

    if (eglChooseConfig(mediaPlayerController->eglDisplay, atrribs, &eglConfig, 1, &numOfConfigs) !=
        EGL_TRUE) {
        assert("egl choose config fail.");
    }
    EGLint atrributes[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    mediaPlayerController->eglContext = eglCreateContext(mediaPlayerController->eglDisplay,
                                                         eglConfig, nullptr, atrributes);
    ANativeWindow_acquire(nativeWindow);
    ANativeWindow_setBuffersGeometry(nativeWindow, 0, 0, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);
    mediaPlayerController->eglSurface = eglCreateWindowSurface(mediaPlayerController->eglDisplay,
                                                               eglConfig, nativeWindow, 0);
    if (!mediaPlayerController->eglSurface) {
        assert("create eglsurface fail.");
    }
}

void
initMediaCodec(MediaPlayerController *mediaPlayerController, off_t start, off_t length, int fd) {

    auto trackCount = AMediaExtractor_getTrackCount(mediaPlayerController->audioExtractor);
    LOGD("track count is %d", trackCount);
    auto videoMediaFormat = AMediaExtractor_getTrackFormat(
            mediaPlayerController->audioExtractor, 0);
    LOGD("track idx1  format is %s", AMediaFormat_toString(videoMediaFormat));
    auto audioMediaFormat = AMediaExtractor_getTrackFormat(
            mediaPlayerController->audioExtractor, 1);
    LOGD("track idx 0 format is %s", AMediaFormat_toString(audioMediaFormat));
    const char *audioMime = nullptr, *videoMime = nullptr;
    auto result = AMediaFormat_getString(audioMediaFormat, AMEDIAFORMAT_KEY_MIME, &audioMime);
    result = AMediaFormat_getString(videoMediaFormat, AMEDIAFORMAT_KEY_MIME, &videoMime);


    mediaPlayerController->audioFormat.mime_ = audioMime;
    AMediaFormat_getInt32(audioMediaFormat, AMEDIAFORMAT_KEY_CHANNEL_COUNT,
                          &mediaPlayerController->audioFormat.channel_count_);
    AMediaFormat_getInt32(audioMediaFormat, AMEDIAFORMAT_KEY_SAMPLE_RATE,
                          &mediaPlayerController->audioFormat.sample_rate_);
    AMediaFormat_getInt64(audioMediaFormat, AMEDIAFORMAT_KEY_DURATION,
                          &mediaPlayerController->audioFormat.duration_);

    mediaPlayerController->videoFormat.mime_ = videoMime;
    AMediaFormat_getInt32(videoMediaFormat, AMEDIAFORMAT_KEY_WIDTH,
                          &mediaPlayerController->videoFormat.width_);
    AMediaFormat_getInt32(videoMediaFormat, AMEDIAFORMAT_KEY_HEIGHT,
                          &mediaPlayerController->videoFormat.height_);
    AMediaFormat_getInt64(videoMediaFormat, AMEDIAFORMAT_KEY_DURATION,
                          &mediaPlayerController->videoFormat.duration_);


    AMediaFormat_getBuffer(videoMediaFormat, "csd-0",
                           reinterpret_cast<void **>(&(mediaPlayerController->videoFormat.sps_)),
                           &mediaPlayerController->videoFormat.sps_size_);

    AMediaFormat_getBuffer(videoMediaFormat, "csd-1",
                           reinterpret_cast<void **>(&(mediaPlayerController->videoFormat.pps_)),
                           &mediaPlayerController->videoFormat.pps_size_);

    if(mediaPlayerController->videoFormat.sps_size_){
        LOGI("video sps:%s", bytesToHex(mediaPlayerController->videoFormat.sps_,mediaPlayerController->videoFormat.sps_size_).c_str());
    }
    if(mediaPlayerController->videoFormat.pps_size_){
        LOGI("video pps:%s", bytesToHex(mediaPlayerController->videoFormat.pps_,mediaPlayerController->videoFormat.pps_size_).c_str());
    }
    mediaPlayerController->audioCodec = AMediaCodec_createDecoderByType(audioMime);
    mediaPlayerController->videoCodec = AMediaCodec_createDecoderByType(videoMime);
    AMediaCodec_configure(mediaPlayerController->audioCodec, audioMediaFormat, nullptr, nullptr, 0);
    AMediaCodec_configure(mediaPlayerController->videoCodec, videoMediaFormat, nullptr, nullptr, 0);

//    constexpr const char *yuv_output_file = "/sdcard/Android/data/com.blueberry.videoplayer/files/captain_women.yuv";
//    constexpr const char *h264_output_file = "/sdcard/Android/data/com.blueberry.videoplayer/files/captain_women.h264";
//    mediaPlayerController->yuv_file_.open(yuv_output_file, std::ios::ate | std::ios::out);
//    if (!mediaPlayerController->yuv_file_.is_open()) {
//        mediaPlayerController->yuv_file_.close();
//        assert(false && "open yuv file is error code ");
//    }
//    mediaPlayerController->h264_file_.open(h264_output_file, std::ios::ate | std::ios::out);
//    if (!mediaPlayerController->h264_file_.is_open()) {
//        mediaPlayerController->h264_file_.close();
//        assert(false && "open h264 file is error code");
//    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_MediaPlayerController_start(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(c_ptr);
    mediaPlayerController->start();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_MediaPlayerController_pause(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(c_ptr);
    mediaPlayerController->pause();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_MediaPlayerController_stop(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(c_ptr);
    mediaPlayerController->stop();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_MediaPlayerController_destroy(JNIEnv *env, jobject thiz,
                                                             jlong c_ptr) {
    auto mediaPlayerController = reinterpret_cast<MediaPlayerController *>(c_ptr);
    delete mediaPlayerController;
}