package com.blueberry.mediarecorder.encode

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

/**
 * author: muyonggang
 * date: 2022/6/5
 */
class MediaMuxerMp4
    (output: String) {
    private val muxer: MediaMuxer
    private var audioTrackIndex: Int? = null
    private var videoTrackIndex: Int? = null

    private var audioReady = false
    private var videoReady = false
    private var isStarted = false

    companion object {
        private const val TAG = "MediaMuxerMp4"
    }

    init {
        muxer = MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer.setOrientationHint(270)
    }

    fun addAudioTrack(audioMediaFormat: MediaFormat) {
        if (audioTrackIndex == null) {
            audioTrackIndex = muxer.addTrack(audioMediaFormat)
            audioReady = true
            start()
        }
    }

    fun addVideoTrack(videoMediaFormat: MediaFormat) {
        if (videoTrackIndex == null) {
            videoTrackIndex = muxer.addTrack(videoMediaFormat)
            videoReady = true
            start()
        }
    }

    fun writeAudioSampleData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isStarted) {
            muxer.writeSampleData(audioTrackIndex ?: return, byteBuffer, bufferInfo)
        }
    }

    fun writeVideoSampleData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isStarted) {
            muxer.writeSampleData(videoTrackIndex ?: return, byteBuffer, bufferInfo)
        }
    }

    private fun start() {
        Log.i(TAG, "start: ")
        if (audioReady && videoReady) {
            muxer.start()
            isStarted = true
        }
    }

    fun stop() {
        Log.i(TAG, "stop: ")
        muxer.stop()
    }

    fun release() {
        Log.i(TAG, "release: ")
        muxer.release()
    }
}