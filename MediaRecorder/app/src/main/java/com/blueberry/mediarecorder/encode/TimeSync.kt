package com.blueberry.mediarecorder.encode

/**
 * author: muyonggang
 * date: 2022/6/5
 */
class TimeSync {

    private var fistAudioPts: Long? = null
    private var diff: Long? = null

    fun getAudioPts(): Long {
        val time = currentMicrosecond()
        if (fistAudioPts == null) {
            fistAudioPts = time
        }
        return time
    }

    fun audioUpdated(): Boolean = fistAudioPts != null

    fun getVideoPts(pts: Long): Long {
        if (diff == null) {
            diff =  pts - fistAudioPts!!
        }
        return pts+ diff!!
    }

    private fun currentMicrosecond() = System.nanoTime() / 1000


}