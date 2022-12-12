package com.blueberry.mediarecorder.encode

import android.media.MediaCodec
import android.util.Log

/**
 * author: muyonggang
 * date: 2022/5/29
 */
class VideoEncoderThread(
    private val videoCodec: MediaCodec,
    private val muxerMp4: MediaMuxerMp4,
    private val timeSync: TimeSync
) : Thread() {
    companion object {
        private const val TAG = "RecordVideoThread"
        const val STATE_START = 1
        const val STATE_STOP = 2
        const val TIME_OUT = 1000L
    }

    private var state = STATE_START
    private var mStopCallback: (() -> Unit)? = null

    override fun run() {
        while (state == STATE_START) {
            val bufferInfo = MediaCodec.BufferInfo()
            val outputIndex = videoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
            if (outputIndex >= 0) {
                val currentTime = timeSync.getVideoPts()
                Log.i(TAG, "run: bufferInfo pts : ${bufferInfo.presentationTimeUs}")
                val outputBuffer = videoCodec.getOutputBuffer(outputIndex) ?: continue
                muxerMp4.writeVideoSampleData(outputBuffer, bufferInfo)
                videoCodec.releaseOutputBuffer(outputIndex, currentTime)
                Log.i(TAG, "consume output buffer index $outputIndex release pts $currentTime")
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                muxerMp4.addVideoTrack(videoCodec.outputFormat)
            }
        }

        mStopCallback?.invoke()
        mStopCallback = null
    }

    fun stopRecord(stopCallback: () -> Unit) {
        mStopCallback = stopCallback
        state = STATE_STOP
    }
}