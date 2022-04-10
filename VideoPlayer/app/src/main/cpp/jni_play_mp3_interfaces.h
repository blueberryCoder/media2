//
// Created by bytedance on 2022/4/9.
//

#ifndef VIDEOPLAYER_JNI_PLAY_MP3_INTERFACES_H
#define VIDEOPLAYER_JNI_PLAY_MP3_INTERFACES_H

#include <jni.h>
#include "logger.h"

extern "C" {
JNIEXPORT   jlong JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_initialize(JNIEnv *env, jobject,
                                                          jstring file_path);
JNIEXPORT  jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_start(JNIEnv *env, jobject, jlong);
JNIEXPORT  jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_pause(JNIEnv *env, jobject thiz, jlong cptr) ;
JNIEXPORT   jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_stop(JNIEnv *env, jobject, jlong);
JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_destroy(JNIEnv *env, jobject thiz, jlong c_ptr);
JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_getDuration(JNIEnv *env, jobject thiz, jlong c_ptr) ;
JNIEXPORT jint JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_seek(JNIEnv *env, jobject thiz, jlong c_ptr,
                                                    jlong position) ;
JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLPlayMp3JniLib_getPosition(JNIEnv *env, jobject thiz, jlong c_ptr) ;
};


#endif //VIDEOPLAYER_JNI_PLAY_MP3_INTERFACES_H
