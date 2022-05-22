package com.blueberry.mediarecorder

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.view.SurfaceView
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.blueberry.mediarecorder.data.CameraInfo
import java.lang.StringBuilder
import java.util.*

class VideoActivity : AppCompatActivity(),VideoView {
    private var mVideoSurfaceView: SurfaceView? = null
    private var mBtnStart: Button? = null

    private var videoViewModel:VideoViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        videoViewModel =ViewModelProvider(this)[VideoViewModel::class.java]
            videoViewModel?.init(this)

        mVideoSurfaceView = findViewById(R.id.videoSurfaceView)
        mBtnStart = findViewById(R.id.btnStart)
        mVideoSurfaceView?.getHolder()?.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    Log.i(TAG, "surfaceCreated: ")
                    checkCameraPermissions()
                }

                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                    Log.i(TAG, "surfaceChanged: ")
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                    Log.i(TAG, "surfaceDestroyed: ")
                }
            })
        mBtnStart?.setOnClickListener(View.OnClickListener { })
    }


    private fun checkCameraPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA_PERMISSION
            )
        } else {
            openPreview()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openPreview()
        }
    }

    private fun openPreview() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
//        val surface = mVideoSurfaceView!!.holder.surface
        videoViewModel?.openPreview()
//        try {
//            mCameraManager!!.openCamera("1", object : CameraDevice.StateCallback() {
//                override fun onOpened(camera: CameraDevice) {
//                    Log.i(TAG, "onOpened: ")
//                    try {
//                        camera.createCaptureSession(object : ArrayList<Surface?>() {
//                            init {
//                                add(surface)
//                            }
//                        }, object : CameraCaptureSession.StateCallback() {
//                            override fun onConfigured(session: CameraCaptureSession) {
//                                Log.i(TAG, "onConfigured: ")
//                                try {
//                                    val captureRequestBuilder =
//                                        session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                                    captureRequestBuilder.addTarget(surface)
//                                    val captureRequest = captureRequestBuilder
//                                        .build()
//                                    session.setRepeatingRequest(
//                                        captureRequest,
//                                        null,
//                                        Handler(Looper.getMainLooper())
//                                    )
//                                } catch (e: CameraAccessException) {
//                                    e.printStackTrace()
//                                }
//                            }
//
//                            override fun onConfigureFailed(session: CameraCaptureSession) {
//                                Log.i(TAG, "onConfigureFailed: ")
//                            }
//                        }, Handler(Looper.getMainLooper()))
//                    } catch (e: CameraAccessException) {
//                        e.printStackTrace()
//                    }
//                }
//
//                override fun onDisconnected(camera: CameraDevice) {
//                    Log.i(TAG, "onDisconnected: ")
//                }
//
//                override fun onError(camera: CameraDevice, error: Int) {
//                    Log.e(TAG, "onError: error:$error")
//                }
//            }, Handler(Looper.getMainLooper()))
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        }
    }

    companion object {
        private const val TAG = "VideoActivity"
        var REQUEST_CODE_CAMERA_PERMISSION = 100

    }

    override fun getPreviewSurface(): Surface? {
        return mVideoSurfaceView?.holder?.surface
    }
}