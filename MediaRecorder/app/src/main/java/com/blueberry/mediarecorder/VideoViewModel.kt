package com.blueberry.mediarecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.camera2.*
import android.media.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Range
import android.view.Display
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.blueberry.mediarecorder.data.CameraInfo
import com.blueberry.mediarecorder.data.RecorderTimeEvent
import com.blueberry.mediarecorder.data.SmartSize
import com.blueberry.mediarecorder.utils.CameraUtils
import com.blueberry.mediarecorder.utils.CameraUtils.and
import java.util.*

/**
 * author: muyonggang
 * date: 2022/5/22
 */
class VideoViewModel(val app: Application) : AndroidViewModel(app) {
    companion object {
        private const val TAG = "VideoViewModel"
        private const val CAMERA_THREAD_NAME = "camera-thread"
        private const val FRAME_RATE = 15
        private const val IFRAME_INTERVAL = 10
        private const val BIT_RATE = 2000000
    }

    private lateinit var mCameraCaptureSession: CameraCaptureSession
    private var mCameraManager: CameraManager? = null
    private var handlerThread = HandlerThread(CAMERA_THREAD_NAME)
    private val cameraHandler by lazy { Handler(handlerThread.looper) }
    private var cameraInfoList: ArrayList<CameraInfo> = arrayListOf()
    private var mView: VideoView? = null
    private var mCurCameraInfo: CameraInfo? = null
    private var mVideoEncoderThread: VideoEncoderThread? = null
    private var mRecorderTimer: RecorderTimer? = null
    private var mPreviewSize: SmartSize? = null

    private val captureRequest: CaptureRequest? by lazy {
        val builder =
            mCameraCaptureSession.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(previewSurface ?: return@lazy null)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(50, 50))
        builder.build()
    }

    private val recorderRequest: CaptureRequest? by lazy {
        val builder =
            mCameraCaptureSession.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        builder.addTarget(previewSurface ?: return@lazy null)
        builder.addTarget(videoCodecInputSurface ?: return@lazy null)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(50, 50))
        builder.build()
    }

    private val videoCodecInputSurface: Surface? by lazy {
        val surface = MediaCodec.createPersistentInputSurface()
        MediaCodecFactory.createVideoMediaCodec(
            videoMediaFormat ?: return@lazy null,
            surface
        )
        surface
    }
    private val videoMediaFormat: MediaFormat? by lazy {
        val previewSize = mPreviewSize ?: return@lazy null
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            previewSize.width,
            previewSize.height
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        mediaFormat
    }

    private var mVideoEncoder: VideoEncoder? = null

    private val previewSurface by lazy {
        mView?.getPreviewSurface()
    }

    val recorderStateLiveData = MutableLiveData<RecorderTimeEvent>()

    fun init(view: VideoView) {
        mView = view
        handlerThread.start()
        mCameraManager = app.getSystemService(AppCompatActivity.CAMERA_SERVICE) as? CameraManager
        cameraInfoList = CameraUtils.enumerateCameras(mCameraManager ?: return)
        mCurCameraInfo = findBestCameraInfo(cameraInfoList)
    }

    private fun findBestCameraInfo(cameraInfoList: ArrayList<CameraInfo>): CameraInfo {
        var cameraInfo: CameraInfo? =
            cameraInfoList.first { it.lenFacing == CameraCharacteristics.LENS_FACING_FRONT }
        if (cameraInfo == null) {
            cameraInfo = cameraInfoList.first()
        }
        return cameraInfo
    }

    private fun getLargestPreviewSize(display: Display): SmartSize {
        mCurCameraInfo ?: SmartSize.SIZE_NONE
        val cameraCharacteristics = mCameraManager?.getCameraCharacteristics(
            mCurCameraInfo?.cameraId ?: return SmartSize.SIZE_NONE
        )
        val displayPoint = Point()
        display.getRealSize(displayPoint)
        val screenSize = SmartSize(displayPoint.x, displayPoint.y)
        var hdScreen = false
        if (screenSize.width >= SmartSize.SIZE_1080P.width
            || screenSize.height >= SmartSize.SIZE_1080P.height
        ) {
            hdScreen = true
        }
        val maxSize = if (hdScreen) SmartSize.SIZE_1080P else screenSize
        val map = cameraCharacteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val surfaceOutputSizes =
            map?.getOutputSizes(SurfaceHolder::class.java) ?: return SmartSize.SIZE_NONE
        val mediacodecOutputSizes =
            map?.getOutputSizes(MediaCodec::class.java) ?: return SmartSize.SIZE_NONE

        val outputSizes = surfaceOutputSizes and mediacodecOutputSizes
        outputSizes?.sortByDescending { it.width * it.height }
        val targetSize =
            outputSizes?.first { it.width <= maxSize.width && it.height <= maxSize.height }
        return SmartSize(targetSize.width, targetSize.height)
    }


    fun startRecord() {
        mCameraCaptureSession.setRepeatingRequest(
            recorderRequest ?: return,
            null,
            cameraHandler
        )
        if (mVideoEncoder == null) {
            mVideoEncoder = VideoEncoder(
                videoMediaFormat ?: return,
                videoCodecInputSurface ?: return,
                CameraUtils.getRecordVideoPath(app)
            )
        }
        mVideoEncoder?.init()
        mVideoEncoder?.start()
        mRecorderTimer = RecorderTimer(Looper.getMainLooper()) {
            recorderStateLiveData.postValue(RecorderTimeEvent(timestamp = it))
        }
        mRecorderTimer?.start(System.currentTimeMillis())
        mVideoEncoderThread?.start()
        recorderStateLiveData.postValue(RecorderTimeEvent(RecorderTimeEvent.STATE_START, ""))
    }

    fun stopRecord() {
        mVideoEncoder?.stop {
            this.destroy()
        }
        mVideoEncoder = null
        mRecorderTimer?.stop()
        mRecorderTimer = null
        recorderStateLiveData.postValue(RecorderTimeEvent(RecorderTimeEvent.STATE_STOP, ""))
    }

    fun getLargestSize(display: Display): SmartSize? {
        val size = getLargestPreviewSize(display)
        mPreviewSize = size
        return mPreviewSize
    }

    @SuppressLint("MissingPermission")
    fun openPreview() {
        if (isHaveCameraPermission().not()) {
            return
        }
        try {
            mCameraManager?.openCamera(
                mCurCameraInfo?.cameraId ?: return, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraOpened(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.i(TAG, "onDisconnected: ")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "onError: error:$error")
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun isHaveCameraPermission() = ActivityCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED


    private fun cameraOpened(camera: CameraDevice) {
        try {
            camera.createCaptureSession(
                arrayListOf(
                    previewSurface ?: return, videoCodecInputSurface
                ),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCameraCaptureSession = session
                        startPreview(session)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.i(TAG, "onConfigureFailed: ")
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPreview(session: CameraCaptureSession) {
        try {
            session.setRepeatingRequest(
                captureRequest ?: return,
                null,
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        handlerThread.looper.quit()
        videoCodecInputSurface?.release()
        mVideoEncoder?.stop { this.destroy() }
    }
}

