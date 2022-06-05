package com.blueberry.mediarecorder.encode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface

/**
 * author: muyonggang
 * date: 2022/6/3
 */
class VideoEncoder(
    private val videoFormat: MediaFormat,
    private val inputSurface: Surface,
    private val muxerMp4: MediaMuxerMp4,
    private val timeSync: TimeSync
) {
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mThread: VideoEncoderThread

    fun init() {
        mMediaCodec = MediaFoundationFactory.createVideoMediaCodec(videoFormat, inputSurface)
        mThread = VideoEncoderThread(mMediaCodec, muxerMp4,timeSync)
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