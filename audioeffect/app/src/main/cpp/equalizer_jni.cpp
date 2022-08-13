//
// Created by bytedance on 2022/7/10.
//

#include "jni.h"
#include "logger.h"
#include "sox.h"
#include "Equalizer.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_com_blueberry_audioeffect_EqualizerJniLib_init(JNIEnv *env, jobject thiz) {
    auto *equalizer = new Equalizer();
    return reinterpret_cast<jlong>(equalizer);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_audioeffect_EqualizerJniLib_setInput(JNIEnv *env, jobject thiz, jlong cptr,
                                                        jstring input) {
    auto equalizer = reinterpret_cast<Equalizer * > (cptr);
    auto input_str = env->GetStringUTFChars(input, NULL);
    equalizer->setInput(input_str);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_audioeffect_EqualizerJniLib_setOutput(JNIEnv *env, jobject thiz, jlong cptr,
                                                         jstring output) {
    auto equalizer = reinterpret_cast<Equalizer * > (cptr);
    auto output_str = env->GetStringUTFChars(output, NULL);
    equalizer->setOutput(output_str);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blueberry_audioeffect_EqualizerJniLib_start(JNIEnv *env, jobject thiz, jlong cptr) {

    auto equalizer = reinterpret_cast<Equalizer * > (cptr);
    equalizer->start();
}