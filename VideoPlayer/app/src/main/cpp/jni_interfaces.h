//
// Created by bytedance on 2022/4/3.
//

#ifndef VIDEOPLAYER_JNI_INTERFACES_H
#define VIDEOPLAYER_JNI_INTERFACES_H

#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_createSLEngine(JNIEnv *env, jclass clazz, jint rate,
                                                       jint frames_per_buf, jlong delay_in_ms,
                                                       jfloat decay);

JNIEXPORT jboolean JNICALL
Java_com_blueberry_videoplayer_SLJniLib_createSLBufferQueueAudioPlayer(JNIEnv *env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_com_blueberry_videoplayer_SLJniLib_createAudioRecorder(JNIEnv *env, jclass clazz);

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_deleteAudioRecorder(JNIEnv *env, jclass clazz);

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_startPlay(JNIEnv *env, jclass clazz);

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_stopPlay(JNIEnv *env, jclass clazz);

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_deleteSLEngine(JNIEnv *env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_com_blueberry_videoplayer_SLJniLib_configureEcho(JNIEnv *env, jclass clazz, jint delay_in_ms,
                                                      jfloat decay);
JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_deleteSLBufferQueueAudioPlayer(JNIEnv *env, jclass clazz);

};

#endif //VIDEOPLAYER_JNI_INTERFACES_H
