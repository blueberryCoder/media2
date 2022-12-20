package com.blueberry.videocut

import android.opengl.GLES20
import java.lang.IllegalArgumentException

/**
 * Created by blueberry on 2022/12/19
 * @author blueberrymyg@gmail.com
 */
class VertexBufferLayout {

    companion object {

        fun getSizeOfType(type: Int):Int {
            return when(type) {
                GLES20.GL_FLOAT ->  4
                GLES20.GL_UNSIGNED_INT -> 4
                GLES20.GL_UNSIGNED_BYTE -> 1
                else -> {
                    throw IllegalArgumentException("The type is not be supported.")
                }
            }
        }
    }

    data class Element(
        val indx: Int,
        val type: Int,
        val count: Int,
        val normalized: Boolean
    )

    private val list: MutableList<Element> = mutableListOf()
    private var stride = 0

    fun getStride(): Int {
        return stride
    }

    fun pushFloat(handle: Int, count: Int) {
        val element = Element(handle, GLES20.GL_FLOAT, count, false)
        list.add(element)
        stride += count * getSizeOfType(GLES20.GL_FLOAT)
    }

    fun getElements(): List<Element> = list
}