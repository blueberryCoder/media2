package com.blueberry.videocut

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Created by blueberry on 2022/9/11
 * @author blueberrymyg@gmail.com
 */
class MainThreadExecutor : Executor {
    private val uiHandler = Handler(Looper.getMainLooper())
    override fun execute(command: Runnable) {
        uiHandler.post(command)
    }
}