package com.blueberry.videocut

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import java.nio.*
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
        private const val VERTEX_SIZE = 4
        private const val VERTEX_STRIDE = VERTEX_SIZE * 4
    }

    // back font
    // 3  2
    // 0  1
    private val vertexData = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        1.0f, 1.0f,
        -1.0f, 1.0f,
    )

    private val cameraBackFrontTextureData = floatArrayOf(
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 1.0f,
    )
    private val indicesArray = intArrayOf(0, 1, 2, 2, 3, 0)

    private var positionHandle = GLES20.GL_NONE
    private var textureHandle = GLES20.GL_NONE

    private lateinit var previewRender: Shader
    private lateinit var ibo: IndexBuffer
    private lateinit var vbo0: VertexBuffer
    private lateinit var vbo1: VertexBuffer
    private lateinit var vbo2: VertexBuffer
    private lateinit var externalOES: TextureExternalOES

    private lateinit var waterRender: Shader
    private lateinit var waterIbo: IndexBuffer
    private lateinit var waterVbo0: VertexBuffer
    private lateinit var waterVbo1: VertexBuffer
    private lateinit var waterTexture: Texture2D

    private var waterPositionHandle = GLES20.GL_NONE
    private var waterTextureCoordHandle = GLES20.GL_NONE

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "onSurfaceCreated: gl config");
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        waterRender = Shader(GLConstants.WATER_VERTEX_SOURCE, GLConstants.WATER_FRAGMENT_SOURCE)
        waterRender.bind()
        waterPositionHandle = waterRender.getAttribLocation("vPosition")
        waterTextureCoordHandle = waterRender.getAttribLocation("vTexCoord")

        waterVbo0 = VertexBuffer(
            floatArrayOf(

//                -0.25f, -0.25f,
//                0.25f, -0.25f,
//                0.25f, 0.25f,
//                -0.25f, 0.25f,

                -1.0f,         -1.0f,                        // left-bottom
                -1.0f + 0.25f, -1.0f,                        // right-bottom
                -1.0f + 0.25f, -1.0f + 0.25f,                // right-up
                -1.0f,         -1.0f + 0.25f,               // right-up
            )
        )

        waterVbo1 = VertexBuffer(
            floatArrayOf(
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
            )
        )



        waterTexture = Texture2D(context, R.drawable.gray_cat)
        waterTexture.bind(0)
        waterRender.setUniform1i("s_Texture", 0)
        waterRender.unBind()
        waterVbo0.unBind()
        waterVbo1.unBind()

        previewRender = Shader(GLConstants.CAMERA_VERTEX_SOURCE, GLConstants.CAMERA_FRAGMENT_SOURCE)
        previewRender.bind();
        positionHandle = previewRender.getAttribLocation("vPosition")
        textureHandle = previewRender.getAttribLocation("vTexCoord")




        ibo = IndexBuffer(indicesArray)
        externalOES = TextureExternalOES()
        externalOES.getSurfaceTexture().setOnFrameAvailableListener {
            Log.i(TAG, "OnFrameAvailableListener: ")
            glSurfaceView.requestRender()
        }

        openCamera.invoke(externalOES.getSurfaceTexture())
        externalOES.bind(0)
        previewRender.setUniform1i("s_texture", 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val size = getPreviewSize.invoke()
        GLES20.glViewport(0, 0, width, height)
        startPreview.invoke(externalOES.getSurfaceTexture())
    }

    override fun onDrawFrame(gl: GL10?) {
        Log.i(TAG, "onDrawFrame: ")
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(0.0F, 0F, 0F, 0F)
        externalOES.getSurfaceTexture().updateTexImage()

        vbo0 = VertexBuffer(vertexData)
        vbo1 = VertexBuffer(cameraBackFrontTextureData)
        val vao = VertexArray()
        vao.addVertexBuffer(vbo0, VertexBufferLayout().apply { pushFloat(positionHandle, 2) })
        vao.addVertexBuffer(vbo1, VertexBufferLayout().apply { pushFloat(textureHandle, 2) })

        ibo.bind()
        previewRender.bind()
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0);

        val waterVAO = VertexArray();
        waterVAO.addVertexBuffer(
            waterVbo0,
            VertexBufferLayout().apply { pushFloat(waterPositionHandle, 2) })
        waterVAO.addVertexBuffer(
            waterVbo1,
            VertexBufferLayout().apply { pushFloat(waterTextureCoordHandle, 2) })
        waterIbo = IndexBuffer(
            intArrayOf(
                0, 1, 2, 2, 3, 0
            )
        )
        waterIbo.bind()
        waterRender.bind()
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0);
    }

}