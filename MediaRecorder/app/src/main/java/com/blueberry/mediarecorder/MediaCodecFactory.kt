package com.blueberry.mediarecorder

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Surface

/**
 * author: muyonggang
 * date: 2022/6/3
 */
object MediaCodecFactory {

    fun createVideoMediaCodec(format: MediaFormat, inputSurface: Surface): MediaCodec {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecName = mediaCodecList.findEncoderForFormat(format)
        val videoCodec = MediaCodec.createByCodecName(codecName)
        videoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoCodec.setInputSurface(inputSurface)
        return videoCodec
    }
}
