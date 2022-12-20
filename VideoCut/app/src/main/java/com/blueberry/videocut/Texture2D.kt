package com.blueberry.videocut

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by blueberry on 2022/12/19
 * @author blueberrymyg@gmail.com
 */
class Texture2D {
    private val waterMarkTextureId: Int
    private val bitmapBuffer: ByteBuffer

    constructor(context: Context, resId: Int) {
        waterMarkTextureId = OpenGLUtils.createTextureID()
        val bitmap = BitmapUtil.createBitmapFromId(context, resId)
        bitmapBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
            .order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(bitmapBuffer)
        bitmapBuffer?.flip()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterMarkTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            bitmap.width,
            bitmap.height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            bitmapBuffer
        )
    }

    fun bind(slot: Int = 0) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + slot);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterMarkTextureId);
    }

    fun unBind() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}