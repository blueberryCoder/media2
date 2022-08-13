package com.blueberry.mediarecorder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.blueberry.mediarecorder.utils.PermissionUtil
import com.blueberry.mediarecorder.view.AutoFitSurfaceView

class VideoActivity : AppCompatActivity(), VideoView {

    companion object {
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
                    checkCameraPermissionsAndPreview()
                }

                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                }
            })
        mBtnStart?.setOnClickListener {
            recordState = if (recordState == RECORD_STATE_STOP) {
                startRecord()
                RECORD_STATE_START
            } else {
                stopRecord()
                RECORD_STATE_STOP
            }
        }
    }

    private fun startRecord() {
        mVideoViewModel?.startRecord { state: Int, timestamp: String ->
            when (state) {
                RecorderTimer.START -> {
                    mTvTimer?.visibility = View.VISIBLE
                    mTvTimer?.text = ""
                    mBtnStart?.text = getString(R.string.btn_recorder_recording)
                }
                RecorderTimer.RUNNING -> {
                    mTvTimer?.text = timestamp
                }
                RecorderTimer.STOP -> {
                    mTvTimer?.visibility = View.GONE
                    mBtnStart?.background = ColorDrawable(Color.BLUE)
                    mBtnStart?.text = getString(R.string.btn_recorder_start)
                }
            }
        }
    }

    private fun stopRecord() {
        mVideoViewModel?.stopRecord()
    }

    private fun checkCameraPermissionsAndPreview() {
        if (PermissionUtil.isHavePermissions(this).not()) {
            PermissionUtil.requestPermissions(this)
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
        PermissionUtil.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            ::openPreview
        )
    }

    private fun openPreview() {
        if (PermissionUtil.isHavePermissions(this).not()) {
            return
        }
        val largestPreviewSize =
            mVideoViewModel?.getLargestSize(mVideoSurfaceView?.display ?: return) ?: return
        mVideoSurfaceView?.setAspectRatio(largestPreviewSize.width, largestPreviewSize.height)
        mVideoViewModel?.openPreview(
            largestPreviewSize,
            mVideoSurfaceView?.holder?.surface ?: return
        )
    }


    override fun getPreviewSurface(): Surface? {
        return mVideoSurfaceView?.holder?.surface
    }
}