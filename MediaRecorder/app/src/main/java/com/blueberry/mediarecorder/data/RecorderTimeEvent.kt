package com.blueberry.mediarecorder.data

/**
 * author: muyonggang
 * date: 2022/5/29
 */
data class RecorderTimeEvent(
    val state: Int = STATE_UPDATE,
    val timestamp: String
) {
   companion object {
       const val STATE_START  = 0
       const val STATE_UPDATE = 1
       const val STATE_STOP= 2
   }
}