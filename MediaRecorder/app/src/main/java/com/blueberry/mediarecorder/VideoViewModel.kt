package com.blueberry.mediarecorder

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import com.blueberry.mediarecorder.data.CameraInfo
import java.util.ArrayList

/**
 * author: muyonggang
 * date: 2022/5/22
 */
class VideoViewModel(val app: Application) : AndroidViewModel(app) {
    companion object {
        private const val TAG = "VideoViewModel"
    }

    private var mCameraManager: CameraManager? = null
    private var handlerThread = HandlerThread("camera-thread")
    private val cameraHandler by lazy { Handler(handlerThread.looper) }
    private var cameraInfoList: ArrayList<CameraInfo> = arrayListOf()

    private var mView: VideoView? = null

    fun init(view: VideoView) {
        mView = view
        handlerThread.start()
        mCameraManager = app.getSystemService(AppCompatActivity.CAMERA_SERVICE) as? CameraManager
        CameraUtils.enumerateCameras(mCameraManager ?: return)
    }

    fun openPreview() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val surface = mView?.getPreviewSurface() ?: return
        try {
            mCameraManager?.openCamera("1", object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraOpened(camera, surface)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.i(TAG, "onDisconnected: ")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "onError: error:$error")
                }
            }, cameraHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun cameraOpened(camera: CameraDevice, previewSurface: Surface) {
        try {
            camera.createCaptureSession(
                arrayListOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.i(TAG, "onConfigured: ")
                        try {
                            val captureRequestBuilder =
                                session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            captureRequestBuilder.addTarget(previewSurface)
                            val captureRequest = captureRequestBuilder
                                .build()
                            session.setRepeatingRequest(
                                captureRequest,
                                null,
                                Handler(Looper.getMainLooper())
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.i(TAG, "onConfigureFailed: ")
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        handlerThread.looper.quit()
    }
}