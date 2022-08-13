//
// Created by bytedance on 2022/7/10.
//
#include "jni.h"
#include "android/log.h"

#define TAG "audioeffect-jni"


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_6;
}
