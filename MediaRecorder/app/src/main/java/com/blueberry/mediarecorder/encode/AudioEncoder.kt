package com.blueberry.mediarecorder.encode

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
    private val outputMuxerMp4: MediaMuxerMp4,
    private val timeSync: TimeSync
) {
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mThread: AudioEncoderThread

    fun init() {
        mMediaCodec = MediaFoundationFactory.createAudioMediaCodec(outputFormat)
        mThread = AudioEncoderThread(audioRecord, mMediaCodec, outputMuxerMp4,timeSync)
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