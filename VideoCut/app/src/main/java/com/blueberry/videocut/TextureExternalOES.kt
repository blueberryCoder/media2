package com.blueberry.videocut

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20

/**
 * Created by blueberry on 2022/12/19
 * @author blueberrymyg@gmail.com
 */
class TextureExternalOES {
    private val textureId: Int
    private val surfaceTexture: SurfaceTexture

    constructor() {
        textureId = OpenGLUtils.createExternalOESTextureID()
        surfaceTexture = SurfaceTexture(textureId)
    }

    fun getSurfaceTexture(): SurfaceTexture {
        return surfaceTexture
    }

    fun bind(slot: Int = 0) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + slot)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    }

    fun unBind() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}