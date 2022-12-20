package com.blueberry.videocut

import android.opengl.GLES20

/**
 * Created by blueberry on 2022/12/19
 * @author blueberrymyg@gmail.com
 */
class VertexArray {

    constructor() {

    }

    fun addVertexBuffer(vertexBuffer: VertexBuffer, layout: VertexBufferLayout) {
        vertexBuffer.bind()
        val list = layout.getElements();
        var offset = 0;
        val stride = layout.getStride();
        for (i in list.indices) {
            val element =list[i]
            GLES20.glEnableVertexAttribArray(element.indx)
            OpenGLUtils.checkError()
            GLES20.glVertexAttribPointer(
                element.indx,
                element.count,
                element.type,
                element.normalized,
                stride,
                offset
            )
            OpenGLUtils.checkError()
            offset += element.count * VertexBufferLayout.getSizeOfType(element.type)
        }
    }

    fun bind() {}
    fun unBInd() {}
}