#include <jni.h>

#include <android/log.h>
#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {

    __android_log_print(ANDROID_LOG_DEBUG, "EGL", "jni_onload");


    return JNI_VERSION_1_6;
}
