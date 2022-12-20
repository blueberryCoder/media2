package com.blueberry.videocut

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * Created by blueberry on 2022/12/18
 * @author blueberrymyg@gmail.com
 */
class VertexBuffer {

    private val floatArray: FloatArray
    private val floatArrayBuffer: FloatBuffer
    private val glIdArray = IntArray(1)

    val id: Int get() = this.glIdArray[0]

    constructor(floatArray: FloatArray) {
        this.floatArray = floatArray
        this.floatArrayBuffer = ByteBuffer.allocateDirect(floatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArray)
        this.floatArrayBuffer.flip()

        GLES20.glGenBuffers(1, glIdArray, 0)
        OpenGLUtils.checkError()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        OpenGLUtils.checkError()
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            this.floatArrayBuffer.limit() * 4,
            this.floatArrayBuffer,
            GLES20.GL_STATIC_DRAW
        )
        OpenGLUtils.checkError()
    }

    fun bind() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        OpenGLUtils.checkError()
    }

    fun unBind() {
        OpenGLUtils.checkError()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }
}