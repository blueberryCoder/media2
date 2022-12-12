package com.blueberry.videocut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.util.Size
import com.blueberry.videocut.OpenGLUtils.createProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Created by muyonggang on 2022/9/12
 * @author muyonggang@bytedance.com
 *
 * https://cloud.tencent.com/developer/article/1746433
 * https://www.jianshu.com/p/97c4e95df7b0
 */
class CameraSurfaceRender(
    private val context: Context,
    private val glSurfaceView: GLSurfaceView,
    private val openCamera: (texture: SurfaceTexture) -> Unit,
    private val getPreviewSize: () -> Size,
    private val startPreview: (texture: SurfaceTexture) -> Unit,
) :
    GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "CameraSurfaceRender"
        private const val COORDS_PER_VERTEX_COUNT = 3
        private const val VERTEX_SIZE = 2
        private const val VERTEX_STRIDE = VERTEX_SIZE * 4
    }

    private var vertexShader: Int = GLES20.GL_NONE
    private var fragmentShader: Int = GLES20.GL_NONE
    private var program: Int = GLES20.GL_NONE


    private val vertexData = floatArrayOf(
        -1.0f, 1.0f, // left-up
        -1.0f, -1.0f, // left-bottom
        1.0f, -1.0f, // right-bottom
        1.0f, 1.0f, // right -up
    )

    // 纹理坐标对应顶点坐标与后置摄像头映射
    private val backTextureData = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 0.0f,
    )

    // 纹理坐标对应顶点坐标与前置摄像头映射
    private val frontTextureData = floatArrayOf(
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
    )

    private val waterMarkerTextureData = floatArrayOf(
        0.0f, 0.5f,
        0.5f, 0.5f,
        0.5f, 0.0f,
        0.0f, 0.0f,
    )

    private var bitmapBuffer: ByteBuffer? = null
    private val vertexOrderData = byteArrayOf(0, 1, 2, 3)

    private var vertexBuffer: FloatBuffer? = null
    private var backTextureBuffer: FloatBuffer? = null
    private var frontTextureBuffer: FloatBuffer? = null
    private var vertexOrderBuffer: ByteBuffer? = null
    private var waterMarkerBuffer: FloatBuffer? = null

    private var positionHandle = GLES20.GL_NONE
    private var textureHandle = GLES20.GL_NONE
    private var waterMarkHandle = GLES20.GL_NONE
    private var textureId = GLES20.GL_NONE
    private var waterMarkTextureId = GLES20.GL_NONE
    private var vboId = GLES20.GL_NONE
    private lateinit var surfaceTexture: SurfaceTexture

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        createMemories()
        createProgram()

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        textureHandle = GLES20.glGetAttribLocation(program, "inputTextureCoordinate")
        waterMarkHandle = GLES20.glGetAttribLocation(program, "inputWaterMarkCoordinate")

        textureId = OpenGLUtils.createExternalOESTextureID()
        surfaceTexture = SurfaceTexture(textureId)

        surfaceTexture.setOnFrameAvailableListener {
            Log.i(TAG, "OnFrameAvailableListener: ")
            glSurfaceView.requestRender()
        }
        openCamera.invoke(surfaceTexture)
    }

    private fun createMemories() {
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
        vertexBuffer?.position(0)

        backTextureBuffer = ByteBuffer.allocateDirect(backTextureData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(backTextureData)
        backTextureBuffer?.position(0)

        frontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(frontTextureData)
        frontTextureBuffer?.position(0)

        vertexOrderBuffer = ByteBuffer.allocateDirect(vertexOrderData.size * 4)
            .order(ByteOrder.nativeOrder())
            .put(vertexOrderData)
        vertexOrderBuffer?.position(0)

        waterMarkerBuffer = ByteBuffer.allocateDirect(waterMarkerTextureData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(waterMarkerTextureData)
        waterMarkerBuffer?.position(0)


        val bitmap = BitmapUtil.createBitmapFromId(context, R.drawable.lyf)
        bitmapBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
            .order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(bitmapBuffer)
        bitmapBuffer?.position(0)
//        bitmapBuffer?.flip()

        waterMarkTextureId = OpenGLUtils.createTextureID()

//        GLES20.glTexImage2D(
//            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.width,
//            bitmap.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer
//        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0,  bitmap, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val size = getPreviewSize.invoke()
        GLES20.glViewport(0, 0, width, height)

        startPreview.invoke(surfaceTexture)
    }


    override fun onDrawFrame(gl: GL10?) {
        Log.i(TAG, "onDrawFrame: ")
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(0F, 0F, 0F, 0F)

        surfaceTexture.updateTexImage()

        // draw
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 框架
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, VERTEX_SIZE,
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        // 视频texture
        GLES20.glEnableVertexAttribArray(textureHandle)

        if (true) {
            // back facing
            GLES20.glVertexAttribPointer(
                textureHandle, VERTEX_SIZE, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, backTextureBuffer
            )
        } else {
            GLES20.glVertexAttribPointer(
                textureHandle, VERTEX_SIZE, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, frontTextureBuffer
            )
        }

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_FAN, vertexOrderData.size, GLES20.GL_UNSIGNED_BYTE, vertexOrderBuffer
        )
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureHandle)

        // 解除绑纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)


        //================= 水印 =======
        GLES20.glActiveTexture(GLES20.GL_TEXTURE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterMarkTextureId)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, VERTEX_SIZE,
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(waterMarkHandle)
        GLES20.glVertexAttribPointer(
            waterMarkHandle, VERTEX_SIZE, GLES20.GL_FLOAT, false,
            VERTEX_STRIDE, waterMarkerBuffer
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(waterMarkHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

    }

    private fun createProgram() {
        vertexShader = OpenGLUtils.loadShader(GLES20.GL_VERTEX_SHADER, GLConstants.VERTEX_SOURCE)
        fragmentShader =
            OpenGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, GLConstants.FRAGMENT_SOURCE)
        program = OpenGLUtils.createProgram(vertexShader, fragmentShader)
    }

}