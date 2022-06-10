package com.blueberry.mediarecorder

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * author: muyonggang
 * date: 2022/5/29
 */

class RecorderTimer(
    private val looper: Looper,
    private val callback: (
        actionType: Int,
        timestamp: String
    ) -> Unit
) {
    private var startTime: Long = 0
    private val simpleDataFormat = SimpleDateFormat("mm:ss.SSS", Locale.ENGLISH)

    companion object {
        const val WHAT_UPDATE = 1000
        const val DELAY = 100L
        const val START = 1
        const val RUNNING = 2
        const val STOP = 3
    }

    private var state = START

    private val handler = object : Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == WHAT_UPDATE && state == RUNNING) {
                notifyTimestamp()
                this.sendEmptyMessageDelayed(WHAT_UPDATE, DELAY)
            }
        }
    }

    private fun notifyTimestamp() {
        val t = System.currentTimeMillis() - startTime
        callback.invoke(state, simpleDataFormat.format(Date(t)))
    }

    fun start(time: Long) {
        state = START
        startTime = time
        notifyTimestamp()
        state = RUNNING
        handler.sendEmptyMessageDelayed(WHAT_UPDATE, DELAY)
    }

    fun stop() {
        state = STOP
        notifyTimestamp()
    }
}