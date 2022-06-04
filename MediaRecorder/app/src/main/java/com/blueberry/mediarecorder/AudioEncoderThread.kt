package com.blueberry.mediarecorder

import android.media.AudioRecord
import android.media.MediaCodec
import com.blueberry.mediarecorder.utils.AacUtils
import java.io.File
import java.io.FileOutputStream

/**
 * author: muyonggang
 * date: 2022/6/3
 */
class AudioEncoderThread(
    private val audioRecord: AudioRecord,
    private val audioCodec: MediaCodec,
    private val file: File
) : Thread() {
    companion object {
        private const val TAG = "AudioEncoderThread"

        const val STATE_START = 1
        const val STATE_STOP = 2
        const val TIME_OUT = 1000L
    }

    private var state = STATE_START
    private var mStopCallback: (() -> Unit)? = null

    override fun run() {
        FileOutputStream(file).use { fos ->
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
                        inputBufferIndex, 0, size, System.currentTimeMillis(),
                        if (end) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                }
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = audioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                if (outputBufferIndex >= 0) {
                    val outputPacketSize = bufferInfo.size + AacUtils.ADTS_HEADER_LENGTH
                    val outputBuffer = audioCodec.getOutputBuffer(outputBufferIndex)
                    outputBuffer?.position(bufferInfo.offset)
                    outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                    val outputArr = ByteArray(outputPacketSize)
                    AacUtils.addADTStoPacket(outputArr, outputPacketSize)
                    outputBuffer?.get(outputArr,  AacUtils.ADTS_HEADER_LENGTH, bufferInfo.size)

                    fos.write(outputArr)
                    audioCodec.releaseOutputBuffer(outputBufferIndex, false)
                }
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