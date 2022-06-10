package com.blueberry.mediarecorder

import android.annotation.SuppressLint
import android.app.Application
import android.view.Display
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import com.blueberry.mediarecorder.data.SmartSize
import com.blueberry.mediarecorder.encode.VideoEngine

/**
 * author: muyonggang
 * date: 2022/5/22
 */
@SuppressLint("MissingPermission")
class VideoViewModel(private val app: Application) : AndroidViewModel(app) {

    private var mView: VideoView? = null
    private lateinit var mVideoEngine: VideoEngine

    fun init(view: VideoView) {
        mView = view
        mVideoEngine = VideoEngine(app)
        mVideoEngine.initialize()
    }

    private fun getLargestPreviewSize(display: Display): SmartSize {
        return mVideoEngine.getLargestPreviewSize(display)
    }

    fun startRecord(
        callback: (
            actionType: Int,
            timestamp: String
        ) -> Unit
    ) {
        mVideoEngine.startRecord(callback)
    }

    fun stopRecord() {
        mVideoEngine.stopRecord()
    }

    fun getLargestSize(display: Display): SmartSize? {
        return getLargestPreviewSize(display)
    }

    @SuppressLint("MissingPermission")
    fun openPreview(previewSize: SmartSize, surface: Surface) {
        mVideoEngine.openPreview(previewSize, surface)
    }

    override fun onCleared() {
        super.onCleared()
        mVideoEngine.destroy()
    }
}

