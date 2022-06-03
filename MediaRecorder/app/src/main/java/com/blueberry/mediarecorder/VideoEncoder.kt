package com.blueberry.mediarecorder

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.io.File

/**
 * author: muyonggang
 * date: 2022/6/3
 */
class VideoEncoder(
    private val videoFormat: MediaFormat,
    private val inputSurface: Surface,
    private val outputFile: File
) {
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mThread: VideoEncoderThread

    fun init() {
        mMediaCodec = MediaCodecFactory.createVideoMediaCodec(videoFormat, inputSurface)
        mThread = VideoEncoderThread(mMediaCodec, outputFile)
    }

    fun start() {
        mMediaCodec.start()
        mThread.start()
    }

    fun stop(finished: VideoEncoder.() -> Unit) {
        mThread.stopRecord { this.finished() }

    }

    fun destroy() {
        mMediaCodec.stop()
        mMediaCodec.release()
    }
}