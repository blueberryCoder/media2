//
// Created by bytedance on 2022/3/20.
//

#ifndef GLEXAMPLE_EGLSURFACE_H
#define GLEXAMPLE_EGLSURFACE_H
#include <jni.h>
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blueberry_glexampe_EGLJNILib_init(JNIEnv *env, jclass clazz,jobject surface) ;
#endif //GLEXAMPLE_EGLSURFACE_H

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blueberry_glexampe_EGLJNILib_destroy(JNIEnv *env, jclass clazz) ;