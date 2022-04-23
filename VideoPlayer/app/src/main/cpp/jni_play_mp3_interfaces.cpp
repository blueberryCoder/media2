//
// Created by bytedance on 2022/4/9.
//

#include "jni_play_mp3_interfaces.h"
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES.h>

#include <media/NdkMediaCodec.h>
#include <media/NdkMediaExtractor.h>

#include <cassert>
#include <cstring>
// http://supercurio.project-voodoo.org/ndk-docs/docs/opensles/

extern "C" {

struct Mp3Engine {

    SLObjectItf slObjectItf_;
    SLEngineItf slEngineItf_;

    SLObjectItf slAudioPlayer_;
    SLPlayItf slPlayItf_;

    SLSeekItf slSeekItf_;

    char uri_[200];

};

JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_initialize(JNIEnv *env, jobject thiz,
                                                          jstring file_path) {
    const char *cFilePath = env->GetStringUTFChars(file_path, 0);
    const char *fileUriPrefix = "file://";

    LOGD("mp3 file initialize.");
    auto mp3Engine = new Mp3Engine();

    strncat(mp3Engine->uri_, fileUriPrefix, strlen(fileUriPrefix));
    strncat(mp3Engine->uri_, cFilePath, strlen(cFilePath));

    SLresult result = slCreateEngine(&mp3Engine->slObjectItf_,
                                     0, nullptr,
                                     0, nullptr,
                                     0);
    assert(result == SL_RESULT_SUCCESS);
    result = (*mp3Engine->slObjectItf_)->Realize(mp3Engine->slObjectItf_, SL_BOOLEAN_FALSE);
    assert(result == SL_RESULT_SUCCESS);

    result = (*mp3Engine->slObjectItf_)->GetInterface(mp3Engine->slObjectItf_, SL_IID_ENGINE,
                                                      &mp3Engine->slEngineItf_);
    assert(result == SL_RESULT_SUCCESS);

    SLDataLocator_URI_ dataSourceUrl = {
            .locatorType = SL_DATALOCATOR_URI,
            .URI = (SLchar *) mp3Engine->uri_
    };
    SLDataFormat_MIME inputFormat = {
            .formatType = SL_DATAFORMAT_MIME,
            .mimeType = (SLchar *) "audio/mpeg",
            .containerType = SL_CONTAINERTYPE_MP3
    };
    SLDataSource slDataSource{
            .pLocator = &dataSourceUrl,
            .pFormat = &inputFormat,
    };

    SLObjectItf outputMix;
    (*mp3Engine->slEngineItf_)->CreateOutputMix(mp3Engine->slEngineItf_,
                                                &outputMix,
                                                0,
                                                nullptr,
                                                0);
    (*outputMix)->Realize(outputMix, SL_BOOLEAN_FALSE);

    SLDataLocator_OutputMix outputMixLocator = {
            .locatorType = SL_DATALOCATOR_OUTPUTMIX,
            .outputMix = outputMix,
    };
    SLDataSink slDataSink = {
            .pLocator = &outputMixLocator,
            .pFormat = nullptr
    };
    SLInterfaceID interfaces[2]{
            SL_IID_PLAY,
            SL_IID_SEEK,
    };
    SLboolean itfResult[2]{
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE,
    };

    result = (*mp3Engine->slEngineItf_)->CreateAudioPlayer(mp3Engine->slEngineItf_,
                                                           &mp3Engine->slAudioPlayer_,
                                                           &slDataSource,
                                                           &slDataSink,
                                                           2,
                                                           interfaces,
                                                           itfResult);
    logSLresult(result, "createAudioPlayer.");
    assert(result == SL_RESULT_SUCCESS);
    assert(itfResult[0] == SL_BOOLEAN_TRUE);
    (*mp3Engine->slAudioPlayer_)->Realize(mp3Engine->slAudioPlayer_, SL_BOOLEAN_FALSE);
    assert(result == SL_RESULT_SUCCESS);
    (*mp3Engine->slAudioPlayer_)->GetInterface(mp3Engine->slAudioPlayer_, SL_IID_PLAY,
                                               &mp3Engine->slPlayItf_);

    result = (*mp3Engine->slAudioPlayer_)->GetInterface(mp3Engine->slAudioPlayer_,
                                                        SL_IID_SEEK, &mp3Engine->slSeekItf_);
    logSLresult(result, "get seek Itf");

    assert(result == SL_RESULT_SUCCESS);
    return reinterpret_cast<long>(mp3Engine);
}

JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_start(JNIEnv *env, jobject thiz, jlong cptr) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(cptr);
    auto mp3Engine = *mp3EnginePtr;
    SLuint32 state;
    (*mp3Engine.slPlayItf_)->GetPlayState(mp3Engine.slPlayItf_, &state);
    if (state == SL_PLAYSTATE_PLAYING) {
        return 0;
    }
    (*mp3Engine.slPlayItf_)->SetPlayState(mp3Engine.slPlayItf_, SL_PLAYSTATE_PLAYING);

    return 1;
}

JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_pause(JNIEnv *env, jobject thiz, jlong cptr) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(cptr);
    auto mp3Engine = *mp3EnginePtr;
    SLuint32 state;
    (*mp3Engine.slPlayItf_)->GetPlayState(mp3Engine.slPlayItf_, &state);
    if (state == SL_PLAYSTATE_STOPPED) {
        return 0;
    }
    if (state == SL_PLAYSTATE_PAUSED) {
        (*mp3Engine.slPlayItf_)->SetPlayState(mp3Engine.slPlayItf_, SL_PLAYSTATE_PLAYING);
    } else {
        (*mp3Engine.slPlayItf_)->SetPlayState(mp3Engine.slPlayItf_, SL_PLAYSTATE_PAUSED);
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_stop(JNIEnv *env, jobject thiz, jlong cptr) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(cptr);
    auto mp3Engine = *mp3EnginePtr;
    SLuint32 state;
    (*mp3Engine.slPlayItf_)->GetPlayState(mp3Engine.slPlayItf_, &state);
    if (state == SL_PLAYSTATE_STOPPED) {
        return 0;
    }
    (*mp3Engine.slPlayItf_)->SetPlayState(mp3Engine.slPlayItf_, SL_PLAYSTATE_STOPPED);
    return 1;
}


JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_destroy(JNIEnv *env, jobject thiz, jlong cptr) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(cptr);
    auto mp3Engine = *mp3EnginePtr;

    (*mp3EnginePtr->slPlayItf_)->SetPlayState(mp3Engine.slPlayItf_, SL_PLAYSTATE_STOPPED);
    (*mp3Engine.slObjectItf_)->Destroy(mp3Engine.slObjectItf_);
    delete mp3EnginePtr;
    return 1;
}

JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_getDuration(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(c_ptr);
    auto mp3Engine = *mp3EnginePtr;
    SLmillisecond duration;
    (*mp3Engine.slPlayItf_)->GetDuration(mp3Engine.slPlayItf_, &duration);
    return duration;
}

JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_seek(JNIEnv *env, jobject thiz, jlong c_ptr,
                                                    jlong position) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(c_ptr);
    auto mp3Engine = *mp3EnginePtr;
    (*mp3EnginePtr->slSeekItf_)->SetPosition(mp3Engine.slSeekItf_, position, SL_SEEKMODE_FAST);
    return 1;
}

JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_getPosition(JNIEnv *env, jobject thiz, jlong c_ptr) {
    auto *mp3EnginePtr = reinterpret_cast<Mp3Engine *>(c_ptr);
    auto mp3Engine = *mp3EnginePtr;
    SLmillisecond position;
    (*mp3Engine.slPlayItf_)->GetPosition(mp3Engine.slPlayItf_, &position);
    return position;
}


} // end external  "C"

