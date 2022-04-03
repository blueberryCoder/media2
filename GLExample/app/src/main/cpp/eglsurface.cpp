//
// Created by bytedance on 2022/3/20.
//

#include "eglsurface.h"
#include <jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <unistd.h>
#include <pthread.h>

#include "logger.h"
#include "GLRender.h"

static JavaVM *gVm;
static EGLSurface gSurface;
static EGLDisplay gDisplay;
static EGLContext gContext;

void *runLoop(void *) {
    // running on bind thread.
    LOGD("start thread.");
    JNIEnv *env = nullptr;
    gVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    gVm->AttachCurrentThread(&env, nullptr);

    eglMakeCurrent(gDisplay, gSurface, gSurface, gContext);
    GLRender render ;
    GLuint glProgram =render.createGlProgram();
    GLuint glPosition = glGetAttribLocation(glProgram,"vPosition");


    int count = 1;
    while(count++ < 1000){
        LOGD("run loop");
        if(count %2 ==0){
            glClearColor(1.0,0,0,1.0);
        }else{
            glClearColor(0,1.0,1.0,1);
        }
        render.checkGlError("clearColor");
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        render.checkGlError("clearGLError");
        glUseProgram(glProgram);
        glEnableVertexAttribArray(glPosition);
        glVertexAttribPointer(glPosition,2,GL_FLOAT,GL_FALSE,0,render.getData());
        glDrawArrays(GL_TRIANGLES,0,3);
        eglSwapBuffers(gDisplay,gSurface);
        sleep(1);
    }

    gVm->DetachCurrentThread();
    return nullptr;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blueberry_glexampe_EGLJNILib_init(JNIEnv *env, jclass clazz, jobject jSurface) {

    env->GetJavaVM(&gVm);
    ANativeWindow *window = ANativeWindow_fromSurface(env, jSurface);
    // https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglGetDisplay.xhtml
    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        LOGE("egl have not got display.");
        return JNI_FALSE;
    }
    if (eglInitialize(display, 0, 0) != EGL_TRUE) {
        LOGE("egl Initialize failed.%d", eglGetError());
        return JNI_FALSE;
    }
    gDisplay = display;
    // https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglChooseConfig.xhtml
    const EGLint atrribs[] = {
            EGL_BUFFER_SIZE, 32,
            EGL_ALPHA_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_NONE
    };

    EGLConfig eglConfig;
    EGLint numOfEglConfig;
    if (eglChooseConfig(display, atrribs, &eglConfig, 1, &numOfEglConfig) != EGL_TRUE) {
        LOGE("egl choose config failed.%d,", eglGetError());
        return JNI_FALSE;
    }
    EGLint attributes[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    gContext = eglCreateContext(display, eglConfig, nullptr, attributes);
    if (!gContext) {
        LOGE("eglCreateContext failed.");
        return JNI_FALSE;
    }
    ANativeWindow_acquire(window);
    ANativeWindow_setBuffersGeometry(window, 0, 0, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);
    // screen on
    gSurface = eglCreateWindowSurface(display, eglConfig, window, 0);
    if (!gSurface) {
        return JNI_FALSE;
    }

    // start a thread.
    pthread_t thread;
    pthread_create(&thread, nullptr, runLoop, nullptr);

    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blueberry_glexampe_EGLJNILib_destroy(JNIEnv *env, jclass clazz) {
    return JNI_TRUE;
}
