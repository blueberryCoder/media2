#include <jni.h>
#include "logger.h"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD("jni_onload");
    return JNI_VERSION_1_6;
}
