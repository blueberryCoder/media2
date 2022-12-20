package com.blueberry.videocut

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer


/**
 * Created by blueberry on 2022/12/18
 * @author blueberrymyg@gmail.com
 */
class IndexBuffer {

    private val intArray: IntArray
    private val intArrayBuffer: IntBuffer

    private val glIdArray = IntArray(1)

    val id: Int get() = this.glIdArray[0]

    constructor(intArray: IntArray) {
        this.intArray = intArray
        this.intArrayBuffer = ByteBuffer.allocateDirect(intArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(intArray)

        this.intArrayBuffer.flip()

        GLES20.glGenBuffers(1, glIdArray, 0)
        OpenGLUtils.checkError()
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, id)
        OpenGLUtils.checkError()
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            this.intArrayBuffer.limit() * 4,
            this.intArrayBuffer,
            GLES20.GL_STATIC_DRAW
        )
        OpenGLUtils.checkError()
    }

    fun bind() {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, id)
    }

    fun unBind() {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }
}