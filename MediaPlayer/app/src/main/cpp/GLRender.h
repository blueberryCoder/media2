//
// Created by bytedance on 2022/5/2.
//

#ifndef MEDIAPLAYER_GLRENDER_H
#define MEDIAPLAYER_GLRENDER_H

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

class GLRender {
    constexpr static auto gVertexShader =
            "attribute vec4 vPosition;\n"     // 输入的顶点坐标
            "attribute vec2 aTextCoord;\n"    // 输入的纹理坐标
            "varying vec2 vTextCoord;\n"      // 输出的的纹理坐标，输入到片段着色器
            "void main() {\n"
            "  vTextCoord = vec2(aTextCoord.x,1.0 - aTextCoord.y);\n" // 上下反转y轴
            "  gl_Position = vPosition;\n"
            "}\n";

    constexpr static auto gFragmentShader =
            "precision mediump float;\n"
            "varying vec2 vTextCoord;\n"
            "uniform sampler2D yTexture;\n"
            "uniform sampler2D uTexture;\n"
            "uniform sampler2D vTexture;\n"
            "void main() {\n"
            "  vec3 yuv;\n"
            "  vec3 rgb;\n"
            "  yuv.r = texture2D(yTexture,vTextCoord).g;\n"
            "  yuv.g = texture2D(uTexture,vTextCoord).g - 0.5;\n"
            "  yuv.b = texture2D(vTexture,vTextCoord).g - 0.5;\n"
            "  rgb = mat3("
            "    1.0,1.0,1.0,"
            "    0.0, -0.39465,2.03211,"
            "    1.13983, -0.5806, 0.0"
            "  ) * yuv ;"
            "  gl_FragColor = vec4(rgb,1.0);\n"
            "}\n";
public :

    GLRender();

    ~GLRender();

    GLuint createGlProgram();

    void checkGlError(const char *op);

    GLfloat *getData();

private:
    GLfloat *data_;

    GLuint loadShader(GLenum shaderType, const char *pSource);

    GLuint createGlProgram(const char *pVertexShaderSource, const char *pFragmentShaderSource);
};


#endif //MEDIAPLAYER_GLRENDER_H
