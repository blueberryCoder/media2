package com.blueberry.mediarecorder.encode

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * author: muyonggang
 * date: 2022/6/5
 */
class MediaMuxerMp4(output: String, orientation: Int, private val avcPath: String) {
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
        muxer.setOrientationHint(orientation)
    }

    fun addAudioTrack(audioMediaFormat: MediaFormat) {
        if (audioTrackIndex == null) {
            audioTrackIndex = muxer.addTrack(audioMediaFormat)
            audioReady = true
            tryToStart()
        }
    }

    fun addVideoTrack(videoMediaFormat: MediaFormat) {
        if (videoTrackIndex == null) {
            videoTrackIndex = muxer.addTrack(videoMediaFormat)
            videoReady = true
            tryToStart()
        }
    }

    fun writeAudioSampleData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isStarted) {
            muxer.writeSampleData(audioTrackIndex ?: return, byteBuffer, bufferInfo)
        }
    }

    fun writeVideoSampleData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isStarted) {

            val bytes = ByteArray(bufferInfo.size)
            val oldPos = byteBuffer.position()
            byteBuffer.get(bytes, bufferInfo.offset, bufferInfo.size);
            byteBuffer.position(oldPos)
            avcFos?.write(bytes)

            muxer.writeSampleData(videoTrackIndex ?: return, byteBuffer, bufferInfo)
        }
    }

    private var avcFos: FileOutputStream? = null

    private fun tryToStart() {
        if (audioReady && videoReady) {
            muxer.start()
            isStarted = true
            avcFos = FileOutputStream(avcPath)
        }
    }

    fun stop() {
        muxer.stop()
        isStarted = false
        audioReady = false
        videoReady = false
    }

    fun release() {
        muxer.release()
        avcFos?.close()
    }
}