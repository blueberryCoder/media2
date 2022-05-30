package com.blueberry.mediarecorder

import android.Manifest
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
        private const val CAMERA_RECORD_THREAD_NAME = "camera-recorder-thread-name"

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
    private var mRecordVideoThread: RecordVideoThread? = null
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
        builder.addTarget(videoCodecInputSurface)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(50, 50))
        builder.build()
    }

    private val videoCodecInputSurface: Surface by lazy {
        val surface = MediaCodec.createPersistentInputSurface()
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
//        mediaFormat.setInteger(MediaFormat.KEY_ROTATION,270)
        mediaFormat
    }

    private val videoMediaCodec by lazy {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        // https://stackoverflow.com/questions/34143942/android-encoding-audio-and-video-using-mediacodec
        val codecName = mediaCodecList.findEncoderForFormat(videoMediaFormat ?: return@lazy null)
        val videoCodec = MediaCodec.createByCodecName(codecName)
        videoCodec.configure(videoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoCodec.setInputSurface(videoCodecInputSurface)
        videoCodec
    }

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
                ?: return SmartSize.SIZE_NONE
        return SmartSize(targetSize.width, targetSize.height)
    }


    fun startRecord() {
        mCameraCaptureSession.setRepeatingRequest(
            recorderRequest ?: return,
            null,
            cameraHandler
        )
        mRecordVideoThread =
            RecordVideoThread(videoMediaCodec ?: return, CameraUtils.getRecordVideoPath(app))
        mRecorderTimer = RecorderTimer(Looper.getMainLooper()) {
            recorderStateLiveData.postValue(RecorderTimeEvent(timestamp = it))
        }
        mRecorderTimer?.start(System.currentTimeMillis())

//        videoMediaCodec?.reset()
//        videoMediaCodec?.configure(videoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//        videoMediaCodec?.setInputSurface(videoCodecInputSurface)
        mRecordVideoThread?.start()
        recorderStateLiveData.postValue(RecorderTimeEvent(RecorderTimeEvent.STATE_START, ""))
    }

    fun stopRecord() {
        mRecordVideoThread?.stopRecord()
        mRecorderTimer?.stop()
        mRecorderTimer = null
        recorderStateLiveData.postValue(RecorderTimeEvent(RecorderTimeEvent.STATE_STOP, ""))
    }

    fun getLargestSize(display: Display): SmartSize? {
        val size = getLargestPreviewSize(display)
        mPreviewSize = size
        return mPreviewSize
    }

    fun openPreview() {
        if (checkCameraPermission()) {
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

    private fun checkCameraPermission() = ActivityCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.CAMERA
    ) != PackageManager.PERMISSION_GRANTED


    private fun cameraOpened(camera: CameraDevice) {
        try {
            videoMediaCodec
            camera.createCaptureSession(
                arrayListOf(
                    previewSurface ?: return
                    , videoCodecInputSurface
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
        mRecordVideoThread?.stopRecord()
        videoCodecInputSurface.release()
    }
}

