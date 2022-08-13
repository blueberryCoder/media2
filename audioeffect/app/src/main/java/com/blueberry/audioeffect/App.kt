package com.blueberry.audioeffect

import android.app.Application
import android.util.Log

/**
 * author: muyonggang
 * date: 2022/7/10
 */
class App : Application() {
    companion object {
        private const val TAG = "App"
    }

    override fun onCreate() {
        super.onCreate()
        AudioEffectLib.loadLibrary()
        Log.i(TAG, "onCreate: ")
    }
}