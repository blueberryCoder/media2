//
// Created by bytedance on 2022/3/13.
//

#ifndef GLEXAMPLE_GL2JNI_H
#define GLEXAMPLE_GL2JNI_H

#include <jni.h>

extern "C"
{

JNIEXPORT void JNICALL
Java_com_blueberry_glexampe_GL2JNILib_init(JNIEnv *env, jclass clazz, jint
width, jint height);

JNIEXPORT void JNICALL
Java_com_blueberry_glexampe_GL2JNILib_step(JNIEnv *env, jclass clazz);

}
#endif //GLEXAMPLE_GL2JNI_H
