
# android使用GLSurfaceView搭建OpenGL环境
## 流程
1. 新建Android工程,需要配置支持jni。
2. 新建一个类继承自GLSurfaceView实现Render
3. 通过jni控制渲染
## 渲染的步骤
### 初始化
1. 编写vertex shader、fragment shader程序.
2. 使用glCreateShader创建shader.
3. 给创建好的shader加载shader程序
4. 编译shader，并查看是否报错
5. 创建program
6. attach之前shader到program
7. link program并查看是否报错
以上初始化步骤非常类似c语言的编译链接过程。
### 绘制
1. 获取vertex shader中定义的attribute
2. 将数据传给该attribute
3. 使用glDrawArray绘制

## 实战:在Android屏幕上画一个三角形
### 自定义View继承GLSurfaceView
```java 
public class GL2JNIView extends GLSurfaceView {
    private static final String TAG = "GL2JNIView";

    public GL2JNIView(Context context) {
        this(context, null);
    }

    public GL2JNIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        Log.d(TAG, "init: ");
        setEGLContextFactory(new ContextFactory());
        setRenderer(new Render());
    }
    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            Log.w(TAG, "creating OpenGL ES 2.0 context");
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }
    }

    public static class Render implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d(TAG, "onSurfaceCreated: ");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d(TAG, "onSurfaceChanged: width=" + width + ",height=" + height);
            String version = gl.glGetString(GL10.GL_VERSION);
            Log.d(TAG, "version: "+ version);
            GL2JNILib.init(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            Log.i(TAG, "onDrawFrame: ");
            GL2JNILib.step();
        }
    }
}
```
### jni层
```java 
public class GL2JNILib {
    public static native void init(int width,int height);
    public static native void step();
}
```
### c++
```c++

#include "gl2jni.h"
#include <GLES2/gl2.h>
#include <android/log.h>

#define TAG  "EGL"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
extern "C" {

auto gVertexShader =
        "attribute vec4 vPosition;\n"
        "void main() {\n"
        "  gl_Position = vPosition;\n"
        "}\n";

auto gFragmentShader =
        "precision mediump float;\n"
        "void main() {\n"
        "  gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n"
        "}\n";

const GLfloat gTriangleVertices[] = { 0.0f, 0.5f,
                                      -0.5f, -0.5f,
                                      0.5f, -0.5f };

static void checkGlError(const char * op){
    GLenum error = GL_NO_ERROR;
    for(error =glGetError();error;error = glGetError()){
        LOGE("after %s glGetError:0x%d",op,error);
    }
}
GLuint loadShader(GLenum shaderType, const char *pSource) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        // 1. load source
        glShaderSource(shader, 1, &pSource, nullptr);
        // 2. compile source.
        glCompileShader(shader);
        GLint compiled = 0;
        // 3. Get compiled result.
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);

        if (!compiled) {
            // if compile failed We should get error msg length  then print it .
            GLint errorLength = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &errorLength);
            if (errorLength) {
                char *msg = new char[errorLength];
                glGetShaderInfoLog(shader, errorLength, nullptr, msg);
                LOGE("We couldn't compile this shader,%s", msg);
                delete[] msg;
            }
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

GLuint createGlProgram(const char *pVertexShaderSource, const char *pFragmentShaderSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexShaderSource);
    if (!vertexShader) {
        return 0;
    }
    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, pFragmentShaderSource);
    if (!fragmentShader) {
        return 0;
    }
    GLuint  glProgram =glCreateProgram();
    if(glProgram){
        glAttachShader(glProgram,vertexShader);
        checkGlError("attach vertexShader");
        glAttachShader(glProgram,fragmentShader);
        checkGlError("attach fragmentShader");
        glLinkProgram(glProgram);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(glProgram,GL_LINK_STATUS,&linkStatus);
        if(linkStatus != GL_TRUE){
            GLint errorLength = 0;
            glGetProgramiv(glProgram,GL_INFO_LOG_LENGTH,&errorLength);
            if(errorLength){
                char * errorMsg = new char [errorLength];
                glGetProgramInfoLog(glProgram,errorLength, nullptr,errorMsg);
                LOGE("Link program is error %s",errorMsg);
                delete[] errorMsg;
            }
            glDeleteProgram(glProgram);
            glProgram = 0;
        }
    }
    return glProgram;
}

GLuint  gProgram;
GLuint  gPosition;
JNIEXPORT void JNICALL
Java_com_blueberry_glexampe_GL2JNILib_init(JNIEnv *env, jclass clazz, jint width, jint height) {
    auto version = glGetString(GL_VERSION);
    auto vendor = glGetString(GL_VENDOR);
    auto renderer = glGetString(GL_RENDERER);
    auto extension = glGetString(GL_EXTENSIONS);
    LOGD("glversion is : %s", version);
    LOGD("glvender is : %s", vendor);
    LOGD("glrender is : %s", renderer);
    LOGD("glextension is : %s", extension);
    // We have created glContext in java layer before.
    gProgram = createGlProgram(gVertexShader, gFragmentShader);
    if (!gProgram) {
        LOGE("could not create program.");
        return;
    }
    gPosition =glGetAttribLocation(gProgram,"vPosition");
    checkGlError("get attribLocation");
    glViewport(0,0,width,height);
}

JNIEXPORT void JNICALL
Java_com_blueberry_glexampe_GL2JNILib_step(JNIEnv *env, jclass clazz) {
    static float  grey = 0;
    grey += 0.01f;
    if(grey > 1.0){
        grey = 0.0;
    }
    glClearColor(grey,grey,grey,1.0f);
    checkGlError("glClearColor");
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    checkGlError("glClear bufferBit | color buffer.");
    glUseProgram(gProgram);
    checkGlError("use program.");
    // https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glVertexAttribPointer.xhtml
    glVertexAttribPointer(gPosition,
                          2,   // 一个顶点所拥有的数据个数
                          GL_FLOAT, // 数据类型
                          GL_FALSE, // 是否需要把数据归一化到 -1，1之间
                          0,gTriangleVertices);
    checkGlError("atrrib pointer.");
    glEnableVertexAttribArray(gPosition);
    checkGlError("glEnabled Vertext attrib array.");
    glDrawArrays(GL_TRIANGLES,0,3);
    checkGlError("glDrawArrays error.");
}
}

```
### CMakeLists需要依赖EGL,GLESv2
```cmake
cmake_minimum_required(VERSION 3.4.1)

# now build app's shared lib
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall")

add_library(gl2jni SHARED
            gl_code.cpp)

# add lib dependencies
target_link_libraries(gl2jni
                      android
                      log 
                      EGL
                      GLESv2)
```
