//
// Created by bytedance on 2022/3/20.
//

#include "surfacejni.h"
#include <jni.h>

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <pthread.h>
#include <unistd.h>
#include "logger.h"

JavaVM *gVm = nullptr;
jobject gJavaSurface;
#define RED  0xFFFF0000  // ABGR
#define BLUE 0xFF0000FF
void *threadRun(void *) {
    // thread run
    JNIEnv *env;
    gVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    gVm->AttachCurrentThread(&env, nullptr);


    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, gJavaSurface);
    if (!nativeWindow) {
        LOGE("native window is null ,check java layer .");
    }
    ANativeWindow_acquire(nativeWindow);

    int width = ANativeWindow_getWidth(nativeWindow);
    int height = ANativeWindow_getHeight(nativeWindow);

    LOGD("the width is %d,the height is %d", width, height);

    ANativeWindow_setBuffersGeometry(nativeWindow, width, height,
                                     AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);
    ANativeWindow_Buffer windowBuffer;

    static int reverse = 0;
    while (reverse < 10000) {
        reverse++;
        ANativeWindow_lock(nativeWindow, &windowBuffer, nullptr);
        int halfHeight = height / 2;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int *buffer = static_cast<int *>(windowBuffer.bits);
                int index = i * width + j;
                int topColor = RED;
                int bottomColor = BLUE;
                if (reverse % 2 == 1) {
                    topColor = RED;
                    bottomColor = BLUE;
                } else {
                    topColor = BLUE;
                    bottomColor = RED;
                }
                if (i < halfHeight) {
                    buffer[index] = topColor;
                } else {
                    buffer[index] = bottomColor;
                }
            }
        }
        ANativeWindow_unlockAndPost(nativeWindow);
        sleep(1);
    }
    ANativeWindow_release(nativeWindow);
    LOGD("thread run.");
    gVm->DetachCurrentThread();

    return nullptr;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blueberry_glexampe_SurfaceJNILib_init(JNIEnv *env, jclass clazz, jobject surface) {
    env->GetJavaVM(&gVm);
    gJavaSurface = env->NewGlobalRef(surface);
    pthread_t thread;
    pthread_create(&thread, nullptr, threadRun, nullptr);
    return JNI_TRUE;
}