package com.blueberry.videocut

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.lang.RuntimeException
import javax.microedition.khronos.opengles.GL10

/**
 * Created by muyonggang on 2022/9/12
 * @author muyonggang@bytedance.com
 */
object OpenGLUtils {
    private const val TAG = "GLUtils"

    fun createExternalOESTextureID(): Int {
        val textureArr = intArrayOf(1)

        GLES20.glGenTextures(1, textureArr, 0)
        checkError()
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureArr[0])
        checkError()
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_LINEAR
        )
        checkError()
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR
        )
        checkError()
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S,
            GL10.GL_CLAMP_TO_EDGE,
        )
        checkError()
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T,
            GL10.GL_CLAMP_TO_EDGE,
        )
        checkError()
        return textureArr[0]
    }

    fun createTextureID(): Int {
        val textureArr = intArrayOf(1)
        GLES20.glGenTextures(1, textureArr, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureArr[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_WRAP_S,
            GL10.GL_REPEAT,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_WRAP_T,
            GL10.GL_REPEAT,
        )
        return textureArr[0]
    }


    fun loadShader(shaderType: Int, shaderSource: String?): Int {
        val shader = GLES20.glCreateShader(shaderType)
        if (shader == GLES20.GL_NONE) {
            return GLES20.GL_NONE
        }
        GLES20.glShaderSource(shader, shaderSource)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.d(TAG, "loadShader: compiler error")
            Log.d(TAG, "loadShader: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return GLES20.GL_NONE
        }
        return shader
    }

    fun createProgram(vertexShader: Int, fragmentShader: Int): Int {
        if (vertexShader == GLES20.GL_NONE || fragmentShader == GLES20.GL_NONE) {
            return GLES20.GL_NONE
        }
        val program = GLES20.glCreateProgram()
        if (program == GLES20.GL_NONE) {
            return GLES20.GL_NONE
        }
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val status = IntArray(1) { 0 }
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            Log.d(TAG, "createProgam: link error");
            Log.d(TAG, "createProgram: " + GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            return GLES20.GL_NONE
        }
        return program
    }


    fun clearError() {
        while(GLES20.glGetError() != 0);
    }
    fun checkError() {
        val error = GLES20.glGetError()
        if (error != 0) {
            throw RuntimeException("OpenGL error. error code is $error");
        }
    }
}