package com.blueberry.mediarecorder.data

import com.blueberry.mediarecorder.RecorderTimer

/**
 * author: muyonggang
 * date: 2022/5/29
 */
data class RecorderTimeEvent(
    val state: Int =RecorderTimer.RUNNING,
    val timestamp: String
)
