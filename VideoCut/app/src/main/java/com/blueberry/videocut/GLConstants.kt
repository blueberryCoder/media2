package com.blueberry.videocut

/**
 * Created by muyonggang on 2022/9/12
 * @author muyonggang@bytedance.com
 */
object GLConstants {

    val CAMERA_VERTEX_SOURCE = """
       attribute vec4 vPosition;  
       attribute vec2 vTexCoord;
       varying vec2 textureCoord;
       void main() {
           gl_Position = vPosition; 
           textureCoord = vTexCoord;
       }
    """.trimIndent()

    val CAMERA_FRAGMENT_SOURCE = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        uniform samplerExternalOES s_texture;
        varying vec2 textureCoord;
        void main() {
           vec4 video_color = texture2D(s_texture, textureCoord);
            gl_FragColor = video_color;
        }
    """.trimIndent()

    val WATER_VERTEX_SOURCE = """
        attribute vec4 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 textureCoord;
        void main() { 
            gl_Position = vPosition;
            textureCoord = vTexCoord;
        }
    """.trimIndent()

    val WATER_FRAGMENT_SOURCE = """
        precision mediump float;
        uniform sampler2D s_Texture;
        varying vec2 textureCoord;
        void main() {
            gl_FragColor = texture2D(s_Texture, textureCoord);
        }
    """.trimIndent()

}