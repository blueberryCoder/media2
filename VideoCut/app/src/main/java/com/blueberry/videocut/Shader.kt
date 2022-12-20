package com.blueberry.videocut

import android.opengl.GLES20

/**
 * Created by blueberry on 2022/12/19
 * @author blueberrymyg@gmail.com
 */
class Shader {

    private val vertexShaderId: Int
    private val fragmentShaderId: Int
    private val programId: Int

    constructor(vertexShaderSource: String, fragmentShaderSource: String) {
        vertexShaderId = OpenGLUtils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        fragmentShaderId = OpenGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        programId = OpenGLUtils.createProgram(vertexShaderId, fragmentShaderId)
    }

    fun bind() {
        GLES20.glUseProgram(programId)
    }

    fun unBind() {
        GLES20.glUseProgram(0)
    }

    fun getAttribLocation(attribute: String): Int {
        return GLES20.glGetAttribLocation(programId, attribute)
    }

    fun getUniformLocation(name: String): Int {
        return GLES20.glGetUniformLocation(programId, name);
    }

    fun setUniform1i(name: String, value: Int) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES20.glUniform1i(location, value);
        }
    }

    fun getId(): Int {
        return programId
    }
}