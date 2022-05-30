package com.blueberry.mediarecorder

import android.media.MediaCodec
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * author: muyonggang
 * date: 2022/5/29
 */
class RecordVideoThread(
    private val videoCodec: MediaCodec,
    private val file: File
) : Thread() {
    companion object {
        private const val TAG = "RecordVideoThread"
        const val STATE_START = 1
        const val STATE_STOP = 2
        const val TIME_OUT = 1000L
    }

    private var state = STATE_START

    override fun run() {
        videoCodec.start()
        FileOutputStream(file).use { fos ->
            while (state == STATE_START) {
                val bufferInfo = MediaCodec.BufferInfo()
                val outputIndex = videoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                if (outputIndex >= 0) {
                    val outputBuffer = videoCodec.getOutputBuffer(outputIndex) ?: continue
                    val outputArr = ByteArray(bufferInfo.size)
                    outputBuffer.get(outputArr, bufferInfo.offset, bufferInfo.size)
                    fos.write(outputArr)
                    videoCodec.releaseOutputBuffer(outputIndex,false)
                    Log.i(TAG,  "consume output buffer index $outputIndex ")
                }else{

                }
            }
        }
    }

    fun stopRecord() {
        state = STATE_STOP
        videoCodec.stop()
    }
}