package com.blueberry.videocut

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlin.concurrent.thread


// https://blog.csdn.net/qq_15893929/article/details/82219073?utm_medium=distribute.pc_relevant.none-task-blog-2~default~baidujs_baidulandingword~default-1-82219073-blog-86603587.topnsimilarv1&spm=1001.2101.3001.4242.2&utm_relevant_index=4
// https://github.com/ChyengJason/GLPreviewCamera
/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
open class CameraPreviewFragment : Fragment(), ICameraPreviewView {
    companion object {
        private const val TAG = "CameraPreviewFragment"
    }

    private var bitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_preview, container, false)
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var viewModel: CameraPreviewViewModel
    private var isCameraOpened = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraPreviewViewModel::class.java)
        initializeView(view)
        // check permission
        requestPermission { status ->
            if (status) {
                initViewModel()
            }
        }

        viewModel.openCameraStatusLiveData.observe(this.viewLifecycleOwner) {
            if (it.isSuccess) {
                isCameraOpened = true
            }
        }
    }

    private fun requestPermission(callback: (success: Boolean) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        callback.invoke(true)
                    } else {
                        callback.invoke(false)
                    }
                }
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            callback.invoke(true)
        }
    }

    private fun initializeView(view: View) {
        glSurfaceView = view.findViewById<GLSurfaceView>(R.id.glSurfaceView)
        glSurfaceView.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
            }
        })
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(
            CameraSurfaceRender(
                context = requireActivity(),
                glSurfaceView = glSurfaceView,
                openCamera = this::openCamera,
                getPreviewSize = { matchedSize },
                startPreview = this::startPreview,
            )
        )
        glSurfaceView.renderMode = RENDERMODE_WHEN_DIRTY

        view.findViewById<Button>(R.id.btnAddWaterMark).setOnClickListener {
            // add bitmap
            if (bitmap == null) {
                thread {
                    bitmap = BitmapUtil.createBitmapFromId(requireContext(), R.drawable.lyf)
                }
            }
        }
    }

    private fun initViewModel() {
        viewModel.initialize(this)
    }

    override fun getPreviewSurface(): Surface {
        return previewSurface
    }

    private lateinit var previewSurface: Surface
    private lateinit var matchedSize: Size

    private fun openCamera(surfaceTexture: SurfaceTexture) {
        viewModel.findDefaultCameraId()
        matchedSize = viewModel.matchSize(glSurfaceView.display) ?: return
        surfaceTexture.setDefaultBufferSize(matchedSize.width, matchedSize.height)
        previewSurface = Surface(surfaceTexture)
        viewModel.openCamera()
    }

    private fun startPreview(texture: SurfaceTexture) {
        Log.i(TAG, "startPreview: ")
    }


}