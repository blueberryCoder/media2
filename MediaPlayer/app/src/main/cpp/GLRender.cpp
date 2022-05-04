//
// Created by bytedance on 2022/5/2.
//

#include "GLRender.h"
#include "logger.h"

void GLRender::checkGlError(const char *op) {
    GLenum error = GL_NO_ERROR;
    for (error = glGetError(); error; error = glGetError()) {
        LOGE("after %s glGetError:0x%d", op, error);
    }
}

GLuint GLRender::createGlProgram() {
    return createGlProgram(this->gVertexShader, this->gFragmentShader);
}

GLuint GLRender::loadShader(GLenum shaderType, const char *pSource) {
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

GLuint
GLRender::createGlProgram(const char *pVertexShaderSource, const char *pFragmentShaderSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexShaderSource);
    if (!vertexShader) {
        return 0;
    }
    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, pFragmentShaderSource);
    if (!fragmentShader) {
        return 0;
    }
    GLuint glProgram = glCreateProgram();
    if (glProgram) {
        glAttachShader(glProgram, vertexShader);
        checkGlError("attach vertexShader");
        glAttachShader(glProgram, fragmentShader);
        checkGlError("attach fragmentShader");
        glLinkProgram(glProgram);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(glProgram, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            GLint errorLength = 0;
            glGetProgramiv(glProgram, GL_INFO_LOG_LENGTH, &errorLength);
            if (errorLength) {
                char *errorMsg = new char[errorLength];
                glGetProgramInfoLog(glProgram, errorLength, nullptr, errorMsg);
                LOGE("Link program is error %s", errorMsg);
                delete[] errorMsg;
            }
            glDeleteProgram(glProgram);
            glProgram = 0;
        }
    }
    return glProgram;
}

GLfloat *GLRender::getData() {
    return this->data_;
}

GLRender::GLRender() {
//    this->data_ = new GLfloat[]{0.0f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f};
    this->data_ = new GLfloat[]{1.0f, -1.0f, 0.0f,
                                -1.0f, -1.0f, 0.0f,
                                1.0f, 1.0f, 0.0f,
                                -1.0f, 1.0f, 0.0f};
}

GLRender::~GLRender() {
    delete[] this->data_;
}

