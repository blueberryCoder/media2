package com.blueberry.videocut

/**
 * Created by muyonggang on 2022/9/12
 * @author muyonggang@bytedance.com
 */
object GLConstants {

    val VERTEX_SOURCE = """
       attribute vec4 vPosition;  
       attribute vec2 inputTextureCoordinate; 
       attribute vec2 inputWaterMarkCoordinate; 
       varying vec2 textureCoordinate; 
       varying vec2 waterMarkTextureCoordinate; 
       
       void main() {
           gl_Position = vPosition; 
           textureCoordinate = inputTextureCoordinate; 
           waterMarkTextureCoordinate  = inputWaterMarkCoordinate;
       }
    """.trimIndent()

//    val FRAGMENT_SOURCE = """
//        #extension GL_OES_EGL_image_external : require
//        precision mediump float;
//        varying vec2 textureCoordinate;
//        varying vec2 waterMarkTextureCoordinate;
//        uniform samplerExternalOES s_texture;
//        uniform sampler2D  water_marker_texture;
//        void main() {
//           vec4 video_color = texture2D(s_texture, textureCoordinate);
//           vec4 water_marker_color = texture2D(water_marker_texture,waterMarkTextureCoordinate);
//           gl_FragColor = video_color * (1.0 - water_marker_color.a) + water_marker_color * water_marker_color.a;
//        }
//    """.trimIndent()

    val FRAGMENT_SOURCE = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 textureCoordinate;
        varying vec2 waterMarkTextureCoordinate;
        uniform samplerExternalOES s_texture;
        uniform sampler2D  water_marker_texture;
        void main() {
           vec4 video_color = texture2D(s_texture, textureCoordinate);
           vec4 water_marker_color = texture2D(water_marker_texture,waterMarkTextureCoordinate);
           gl_FragColor = video_color * (1.0 - water_marker_color.a) + water_marker_color * water_marker_color.a;
        }
    """.trimIndent()
}