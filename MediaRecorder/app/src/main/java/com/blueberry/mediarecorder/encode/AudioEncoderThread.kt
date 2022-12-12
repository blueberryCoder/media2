package com.blueberry.mediarecorder.encode

import android.media.AudioRecord
import android.media.MediaCodec
import android.util.Log

/**
 * author: muyonggang
 * date: 2022/6/3
 */
class AudioEncoderThread(
    private val audioRecord: AudioRecord,
    private val audioCodec: MediaCodec,
    private val muxerMp4: MediaMuxerMp4,
    private val timeSync: TimeSync
) : Thread() {
    companion object {
        const val STATE_START = 1
        const val STATE_STOP = 2
        const val TIME_OUT = 1000L
        private const val TAG = "AudioEncoderThread"
    }

    private var state = STATE_START
    private var mStopCallback: (() -> Unit)? = null

    override fun run() {
        while (state == STATE_START) {
            val inputBufferIndex = audioCodec.dequeueInputBuffer(TIME_OUT)
            if (inputBufferIndex >= 0) {
                val inputBuffer = audioCodec.getInputBuffer(inputBufferIndex) ?: return
                val size = audioRecord.read(inputBuffer, inputBuffer.limit())
                var end = false
                if (size <= 0) {
                    end = audioRecord.recordingState == AudioRecord.RECORDSTATE_STOPPED
                }
                audioCodec.queueInputBuffer(
                    inputBufferIndex, 0, size, timeSync.getAudioPts(),
                    if (end) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                )
            }
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = audioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
            if (outputBufferIndex >= 0) {
                val outputBuffer = audioCodec.getOutputBuffer(outputBufferIndex) ?: return
                muxerMp4.writeAudioSampleData(outputBuffer, bufferInfo)
                audioCodec.releaseOutputBuffer(outputBufferIndex, false)
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.i(TAG, "run: outputBufferIndex:${outputBufferIndex}")
                muxerMp4.addAudioTrack(audioCodec.outputFormat)
            }
        }
        mStopCallback?.invoke()
        mStopCallback = null
    }

    fun stopEncoder(stopCallback: () -> Unit) {
        mStopCallback = stopCallback
        state = STATE_STOP
    }

}