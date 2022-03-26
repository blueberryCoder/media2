//
// Created by bytedance on 2022/3/26.
//

#ifndef GLEXAMPLE_GLRENDER_H
#define GLEXAMPLE_GLRENDER_H

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

class GLRender {
    constexpr static auto gVertexShader =
            "attribute vec4 vPosition;\n"
            "void main() {\n"
            "  gl_Position = vPosition;\n"
            "}\n";

    constexpr static auto gFragmentShader =
            "precision mediump float;\n"
            "void main() {\n"
            "  gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n"
            "}\n";
public :

    GLRender();
    ~GLRender();

    GLuint createGlProgram();

    void checkGlError(const char *op);

    GLfloat* getData() ;

private:
    GLfloat * data_;
    GLuint loadShader(GLenum shaderType, const char *pSource);

    GLuint createGlProgram(const char *pVertexShaderSource, const char *pFragmentShaderSource);
};


#endif //GLEXAMPLE_GLRENDER_H
