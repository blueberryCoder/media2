package com.blueberry.videocut

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.StateCallback
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Display
import android.view.SurfaceHolder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Created by blueberry on 2022/9/11
 * @author blueberrymyg@gmail.com
 */
class CameraPreviewViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "CameraPreviewViewModel"
    }

    val openCameraStatusLiveData: LiveData<CameraOpenStatus>
        get() = _openCameraStatusLiveData

    private val _openCameraStatusLiveData: MutableLiveData<CameraOpenStatus> =
        MutableLiveData<CameraOpenStatus>()

    private val cameraManager by lazy {
        app.getSystemService(Context.CAMERA_SERVICE)
                as CameraManager
    }
    private val uiHandler by lazy { Handler(Looper.getMainLooper()) }
    private val app get() = getApplication<Application>()
    private lateinit var curCameraDevice: CameraDevice
    private lateinit var cameraPreviewView: ICameraPreviewView
    private lateinit var cameraInfo: CameraInfo
    private lateinit var cameraSession: CameraCaptureSession
    private var matchedSize: Size? = null

    @SuppressLint("MissingPermission")
    fun initialize(view: ICameraPreviewView) {
        this.cameraPreviewView = view
    }

    fun matchSize(display: Display): Size? {
        val list = cameraInfo.surfaceHolderSupportedSize?.toList()
            ?.sortedByDescending { it.width * it.height }
        matchedSize =
            list?.first { it.width <= display.width && it.height <= display.height } ?: Size(
                0,
                0
            )

        return matchedSize
    }

    fun findDefaultCameraId() {
        val cameraIdList = cameraManager.cameraIdList
        cameraIdList.forEach { cameraId ->
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            val fpsRange =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val streamConfigurationMap = cameraCharacteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = streamConfigurationMap?.getOutputSizes(SurfaceHolder::class.java)
            cameraInfo = CameraInfo(cameraId, outputSizes)
            if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(
            cameraInfo.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "onOpened: ")
                    _openCameraStatusLiveData.postValue(CameraOpenStatus(true, 0))
                    curCameraDevice = camera
                    matchedSize ?: return
                    openCameraSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    _openCameraStatusLiveData.postValue(
                        CameraOpenStatus(
                            true,
                            CameraOpenStatus.CUSTOM_ERROR_DISCONNECT
                        )
                    )
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    _openCameraStatusLiveData.postValue(CameraOpenStatus(false, error))
                }
            }, uiHandler
        )
    }

    private fun openCameraSession() {
        val outputSessionConfig = OutputConfiguration(
            matchedSize ?: return, SurfaceHolder::class.java
        )
        outputSessionConfig.addSurface(cameraPreviewView.getPreviewSurface())
        val sessionConfig = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(outputSessionConfig),
            MainThreadExecutor(),
            object : StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.i(TAG, "onConfigured: ")
                    cameraSession = session
                    startPreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.i(TAG, "onConfigureFailed: ")
                }
            }
        )
        curCameraDevice.createCaptureSession(sessionConfig)
    }

    fun startPreview() {
        val previewCaptureRequestBuilder =
            curCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewCaptureRequestBuilder.addTarget(cameraPreviewView.getPreviewSurface())
        previewCaptureRequestBuilder.set(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            Range(50, 50)
        )
        val previewRequest = previewCaptureRequestBuilder.build()
        cameraSession.setRepeatingRequest(
            previewRequest,
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long
                ) {
                    Log.i(TAG, "onCaptureStarted: ")
                }

            }, uiHandler
        )

    }
}