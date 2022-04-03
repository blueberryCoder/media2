# Android使用SurfaceView搭建OpenGL环境
## 流程
1. 在页面上使用SurfaceView。
2. 在SurfaceView创建成功之后使用surface.getSurface获取到Surface对象传到native层。
3. Native层根据传入的Surface对象获取ANativeWindow。
4. 获取显示对象EGLDisplay并初始化。
5. 根据配置创建EGL上下文(EGLContext)。
6. 为ANativeWindow设置buffer.
7. 根据EGLDisplay、ANativeWindow创建一个EGLSurface
8. 启动一个渲染线程，在线程运行后使用eglMakeCurrent将EGLContext绑定到EGLSurface上。
9. 就是可以使用openGL API进行绘制了.
## 概念
### EGL
EGL是一组介于OpenGL、OpenGLEs或OpenGLVG与本地窗口系统之间的接口。它用来处理图形渲染的上下文管理，surface/buffer的绑定
渲染同步等。
- EGLDisplay
是对本地实现显示设备的抽象。
- EGLSurface
背后关联的是buffer用来将数据推入buffer.
### ANativeWindow
定义在Ndk头文件中可以从Java层Surface中获取，它提供了对本地窗口的访问
## 关键代码
### 获取ANativeWindow
```c++
// jSurface是java层传递下来的
ANativeWindow *window = ANativeWindow_fromSurface(env, jSurface);
```
### 给ANativeWindow设置缓存
```c++
ANativeWindow_setBuffersGeometry(window, 0, 0, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);

```
### 获取EGLDisplay
```c++
EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
```
### 创建EGLContext
```c++
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
```
### 创建EGLSurface
```c++
gSurface = eglCreateWindowSurface(display, eglConfig, window, 0);

```
### 在渲染线程关联GLContext和GLSurface和GLDisplay
```c++
eglMakeCurrent(gDisplay, gSurface, gSurface, gContext);
```

## 完整代码
```c++
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

```