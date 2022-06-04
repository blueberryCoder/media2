package com.blueberry.mediarecorder.utils


/**
 * author: muyonggang
 * date: 2022/6/4
 */
object AacUtils {
    // 采样频率对照表
    private val samplingFrequencyIndexMap: MutableMap<Int, Int> = HashMap()

    const val ADTS_HEADER_LENGTH = 7

    init {
        samplingFrequencyIndexMap[96000] = 0
        samplingFrequencyIndexMap[88200] = 1
        samplingFrequencyIndexMap[64000] = 2
        samplingFrequencyIndexMap[48000] = 3
        samplingFrequencyIndexMap[44100] = 4
        samplingFrequencyIndexMap[32000] = 5
        samplingFrequencyIndexMap[24000] = 6
        samplingFrequencyIndexMap[22050] = 7
        samplingFrequencyIndexMap[16000] = 8
        samplingFrequencyIndexMap[12000] = 9
        samplingFrequencyIndexMap[11025] = 10
        samplingFrequencyIndexMap[8000] = 11
        samplingFrequencyIndexMap[0x0] = 96000
        samplingFrequencyIndexMap[0x1] = 88200
        samplingFrequencyIndexMap[0x2] = 64000
        samplingFrequencyIndexMap[0x3] = 48000
        samplingFrequencyIndexMap[0x4] = 44100
        samplingFrequencyIndexMap[0x5] = 32000
        samplingFrequencyIndexMap[0x6] = 24000
        samplingFrequencyIndexMap[0x7] = 22050
        samplingFrequencyIndexMap[0x8] = 16000
        samplingFrequencyIndexMap[0x9] = 12000
        samplingFrequencyIndexMap[0xa] = 11025
        samplingFrequencyIndexMap[0xb] = 8000
    }
    // https://www.cnblogs.com/caosiyang/archive/2012/07/16/2594029.html
    // https://blog.katastros.com/a?ID=00450-4a0eb8df-70dd-4904-b7f4-3cd33f657720
    // https://www.p23.nl/projects/aac-header/
    fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
        val profile = 2 //AAC LC
        val freqIdx = 4 //44.1KHz
        val chanCfg = 2 //CPE

        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte() // Mpeg-2 , no crc
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }
}

