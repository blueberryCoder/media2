//
// Created by bytedance on 2022/3/20.
//

#ifndef GLEXAMPLE_SURFACEJNI_H
#define GLEXAMPLE_SURFACEJNI_H
#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_blueberry_glexampe_SurfaceJNILib_init(JNIEnv *env, jclass clazz, jobject surface) ;

#endif //GLEXAMPLE_SURFACEJNI_H
