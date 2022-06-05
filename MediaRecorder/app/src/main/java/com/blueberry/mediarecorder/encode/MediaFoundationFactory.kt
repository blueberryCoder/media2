package com.blueberry.mediarecorder.encode

import android.annotation.SuppressLint
import android.media.*
import android.view.Surface

/**
 * author: muyonggang
 * date: 2022/6/3
 */
object MediaFoundationFactory {

    private const val FRAME_RATE = 15
    private const val IFRAME_INTERVAL = 10
    private const val VIDEO_BIT_RATE = 2000000
    private const val AUDIO_BIT_RATE = 128000

    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_COUNT = 2

    fun createMuxer(
        filePath: String,
        audioFormat: MediaFormat,
        videoFormat: MediaFormat
    ): MediaMuxerMp4 {
        return MediaMuxerMp4(filePath)
    }

    // pcm16bit
    fun createAudioSourceFormat() = AudioFormat.Builder()
        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        .setSampleRate(SAMPLE_RATE)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .build()

    @SuppressLint("MissingPermission")
    fun createAudioRecord(audioFormat: AudioFormat): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(minBufferSize * 2)
            .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            .build()
    }

    fun createAudioMediaFormat(): MediaFormat {
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            SAMPLE_RATE,
            CHANNEL_COUNT
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
        mediaFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectELD
        )
        return mediaFormat
    }

    fun createVideoMediaFormat(width: Int, height: Int): MediaFormat {
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            width,
            height
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        return mediaFormat
    }

    fun createVideoMediaCodec(format: MediaFormat, inputSurface: Surface): MediaCodec {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecName = mediaCodecList.findEncoderForFormat(format)
        val videoCodec = MediaCodec.createByCodecName(codecName)
        videoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoCodec.setInputSurface(inputSurface)
        return videoCodec
    }

    fun createAudioMediaCodec(format: MediaFormat): MediaCodec {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecName = mediaCodecList.findEncoderForFormat(format)
        val audioCodec = MediaCodec.createByCodecName(codecName)
        audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return audioCodec
    }
}
