//
// Created by bytedance on 2022/3/13.
//

#include "gl2jni.h"
#include <GLES2/gl2.h>
//#include <GLES3/gl3.h>
#include <android/log.h>
#include "logger.h"

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
