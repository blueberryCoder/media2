package com.blueberry.mediarecorder.encode

/**
 * author: muyonggang
 * date: 2022/6/5
 */
class TimeSync {

    fun getAudioPts(): Long {
        return currentMicrosecond()
    }

    fun getVideoPts(): Long {
        return currentMicrosecond()
    }

    fun currentMicrosecond() = System.nanoTime() / 1000
}