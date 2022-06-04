package com.blueberry.mediarecorder

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.blueberry.mediarecorder.view.AutoFitSurfaceView

class VideoActivity : AppCompatActivity(), VideoView {

    companion object {
        private const val TAG = "VideoActivity"
        private const val REQUEST_CODE_CAMERA_PERMISSION = 100
        private const val RECORD_STATE_STOP = 0
        private const val RECORD_STATE_START = 1
    }

    private var mVideoSurfaceView: AutoFitSurfaceView? = null
    private var mBtnStart: Button? = null
    private var mTvTimer: TextView? = null
    private var mVideoViewModel: VideoViewModel? = null
    private var recordState = RECORD_STATE_STOP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        mVideoViewModel = ViewModelProvider(this)[VideoViewModel::class.java]
        mVideoViewModel?.init(this)

        mVideoSurfaceView = findViewById(R.id.videoSurfaceView)
        mTvTimer = findViewById(R.id.tvTime)
        mBtnStart = findViewById(R.id.btnStart)

        mVideoSurfaceView?.holder?.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    Log.i(TAG, "surfaceCreated: ")
                    checkCameraPermissionsAndPreview()
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
        mBtnStart?.setOnClickListener {
            if (recordState == RECORD_STATE_STOP) {
                startRecord()
                recordState = RECORD_STATE_START
            } else {
                stopRecord()
                recordState = RECORD_STATE_STOP
            }
        }

        mVideoViewModel?.recorderStateLiveData?.observe(this) { (state, timestamp) ->
            when (state) {
                RecorderTimer.START -> {
                    mTvTimer?.visibility = View.VISIBLE
                    mTvTimer?.text = ""
                    mBtnStart?.text = "recoding"
                }
                RecorderTimer.RUNNING -> {
                    mTvTimer?.text = timestamp
                }
                RecorderTimer.STOP -> {
                    mTvTimer?.visibility = View.GONE
                    mBtnStart?.background = ColorDrawable(Color.BLUE)
                    mBtnStart?.text = "start"
                }
            }
        }
    }

    private fun startRecord() {
        mVideoViewModel?.startRecord()
    }

    private fun stopRecord() {
        mVideoViewModel?.stopRecord()
    }

    private fun checkCameraPermissionsAndPreview() {
        if (checkHasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
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
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            openPreview()
        }
    }

    private fun openPreview() {
        if (checkHasCameraPermission()) {
            return
        }
        val largestPreviewSize =
            mVideoViewModel?.getLargestSize(mVideoSurfaceView?.display ?: return) ?: return
        mVideoSurfaceView?.setAspectRatio(largestPreviewSize.width, largestPreviewSize.height)
        mVideoViewModel?.openPreview()
    }

    private fun checkHasCameraPermission() = ActivityCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) != PackageManager.PERMISSION_GRANTED

    override fun getPreviewSurface(): Surface? {
        return mVideoSurfaceView?.holder?.surface
    }
}