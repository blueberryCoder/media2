package com.blueberry.mediarecorder

import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import java.io.File

/**
 * author: muyonggang
 * date: 2022/6/3
 */
class AudioEncoder(
    private val audioRecord: AudioRecord,
    private val outputFormat: MediaFormat,
    private val outputFile: File
) {
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mThread: AudioEncoderThread

    fun init() {
        mMediaCodec = MediaCodecFactory.createAudioMediaCodec(outputFormat)
        mThread = AudioEncoderThread(audioRecord, mMediaCodec, outputFile)
    }

    fun start() {
        mMediaCodec.start()
        mThread.start()
    }

    fun stop(finished: AudioEncoder.() -> Unit) {
        mThread.stopEncoder { this.finished() }
    }

    fun destroy() {
        mMediaCodec.stop()
        mMediaCodec.release()
    }
}