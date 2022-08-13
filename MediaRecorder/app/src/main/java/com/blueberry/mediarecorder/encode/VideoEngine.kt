package com.blueberry.mediarecorder.encode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.hardware.camera2.*
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Range
import android.view.Display
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.blueberry.mediarecorder.RecorderTimer
import com.blueberry.mediarecorder.VideoViewModel
import com.blueberry.mediarecorder.data.CameraInfo
import com.blueberry.mediarecorder.data.RecorderTimeEvent
import com.blueberry.mediarecorder.data.SmartSize
import com.blueberry.mediarecorder.utils.CameraUtils
import com.blueberry.mediarecorder.utils.CameraUtils.and
import java.util.ArrayList

/**
 * author: muyonggang
 * date: 2022/6/5
 */
class VideoEngine
constructor(private val context: Context) {
    companion object {
        private const val TAG = "VideoEngine"
        private const val CAMERA_THREAD_NAME = "camera-thread"
        private const val FPS = 50
    }

    private val mCameraManager =
        context.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager

    private var cameraInfoList: ArrayList<CameraInfo> = arrayListOf()
    private var mCurCameraInfo: CameraInfo? = null

    private var handlerThread = HandlerThread(CAMERA_THREAD_NAME)
    private val cameraHandler by lazy { Handler(handlerThread.looper) }

    private lateinit var previewSurface: Surface
    private lateinit var previewSize: SmartSize
    private lateinit var mCameraCaptureSession: CameraCaptureSession

    private val audioMediaFormat = MediaFoundationFactory.createAudioMediaFormat()
    private val audioSourceFormat = MediaFoundationFactory.createAudioSourceFormat()

    private val mAudioRecord: AudioRecord by lazy {
        MediaFoundationFactory.createAudioRecord(audioSourceFormat)
    }

    private var mVideoEncoder: VideoEncoder? = null
    private var mAudioEncoder: AudioEncoder? = null
    private var mMp4Muxer: MediaMuxerMp4? = null
    private var timeSync: TimeSync? = null
    private var mRecorderTimer: RecorderTimer? = null

    private val recorderRequest: CaptureRequest by lazy {
        val builder =
            mCameraCaptureSession.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        builder.addTarget(previewSurface)
        builder.addTarget(videoCodecInputSurface)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(FPS, FPS))
        builder.build()
    }

    private val captureRequest: CaptureRequest? by lazy {
        val builder =
            mCameraCaptureSession.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(previewSurface)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(FPS, FPS))
        builder.build()
    }


    private val videoCodecInputSurface: Surface by lazy {
        val surface = MediaCodec.createPersistentInputSurface()
        MediaFoundationFactory.createVideoMediaCodec(videoMediaFormat, surface)
        surface
    }

    private val videoMediaFormat: MediaFormat by lazy {
        val (width, height) = previewSize
        MediaFoundationFactory.createVideoMediaFormat(width, height)
    }

    fun initialize() {
        cameraInfoList = CameraUtils.enumerateCameras(mCameraManager)
        mCurCameraInfo = findBestCameraInfo(cameraInfoList)
        handlerThread.start()
    }

    @SuppressLint("MissingPermission")
    fun openPreview(previewSize: SmartSize, surface: Surface) {
        this.previewSize = previewSize
        this.previewSurface = surface
        try {
            mCameraManager.openCamera(
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


    /**
     * @param display 窗口的大小信息，根据此信息结合相机支持的大小选择合适的Size
     */
    fun getLargestPreviewSize(display: Display): SmartSize {
        mCurCameraInfo ?: SmartSize.SIZE_NONE
        val cameraCharacteristics = mCameraManager.getCameraCharacteristics(
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
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val surfaceOutputSizes =
            map?.getOutputSizes(SurfaceHolder::class.java) ?: return SmartSize.SIZE_NONE
        val mediacodecOutputSizes =
            map.getOutputSizes(MediaCodec::class.java) ?: return SmartSize.SIZE_NONE
        val outputSizes = surfaceOutputSizes and mediacodecOutputSizes
        outputSizes.sortByDescending { it.width * it.height }
        val targetSize =
            outputSizes.first { it.width <= maxSize.width && it.height <= maxSize.height }
        return SmartSize(targetSize.width, targetSize.height)
    }

    fun startRecord(callback: (actionType: Int, timestamp: String) -> Unit) {
        mCameraCaptureSession.setRepeatingRequest(
            recorderRequest,
            null,
            cameraHandler
        )
        mMp4Muxer = MediaFoundationFactory.createMuxer(
            CameraUtils.geMp4FilePath(context).absolutePath,
            mCurCameraInfo?.orientation ?: 0
        )
        val timeSync = TimeSync()
        if (mVideoEncoder == null) {
            mVideoEncoder = VideoEncoder(
                videoMediaFormat,
                videoCodecInputSurface,
                mMp4Muxer ?: return, timeSync
            )
        }
        if (mAudioEncoder == null) {
            mAudioEncoder =
                AudioEncoder(
                    mAudioRecord,
                    audioMediaFormat,
                    mMp4Muxer ?: return, timeSync
                )
        }
        mAudioRecord.startRecording()
        mVideoEncoder?.init()
        mVideoEncoder?.start()
        mAudioEncoder?.init()
        mAudioEncoder?.start()
        mRecorderTimer = RecorderTimer(Looper.getMainLooper(), callback)
        mRecorderTimer?.start(System.currentTimeMillis())
    }

    fun stopRecord() {
        mAudioRecord.stop()
        val destroyMuxer = object : Runnable {
            private var count = 2
            override fun run() {
                count--
                if (count == 0) {
                    Log.i(TAG, "run: count $count")
                    mMp4Muxer?.stop()
                    mMp4Muxer?.release()
                    mMp4Muxer = null
                }
            }
        }
        mVideoEncoder?.stop {
            this.destroy()
            destroyMuxer.run()
        }
        mAudioEncoder?.stop {
            this.destroy()
            destroyMuxer.run()
        }
        mAudioEncoder = null
        mVideoEncoder = null
        mRecorderTimer?.stop()
        mRecorderTimer = null
        timeSync = null
    }

    fun destroy() {
        handlerThread.looper.quit()
        videoCodecInputSurface.release()
        mVideoEncoder?.stop { this.destroy() }
        mAudioRecord.release()
    }

    private fun cameraOpened(camera: CameraDevice) {
        try {
            camera.createCaptureSession(
                arrayListOf(previewSurface, videoCodecInputSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        // session 打开成功
                        mCameraCaptureSession = session
                        startPreview(session)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
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

    private fun findBestCameraInfo(cameraInfoList: ArrayList<CameraInfo>): CameraInfo {
        var cameraInfo: CameraInfo? =
            cameraInfoList.first { it.lenFacing == CameraCharacteristics.LENS_FACING_FRONT }
        if (cameraInfo == null) {
            cameraInfo = cameraInfoList.first()
        }
        return cameraInfo
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

}