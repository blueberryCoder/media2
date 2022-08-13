package com.blueberry.audioeffect

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * author: muyonggang
 * date: 2022/7/16
 */
object ResourceUtil {

    private const val TAG = "ResourceUtil"
    private const val AUDIO_ASSEST = "song.wav"

    private const val AUDIO_SOURCE_FILENAME = "audio.wav"

    fun writeIfAbsent(context: Context) {
        Log.i(TAG, "writeIfAbsent: ")
        val file = File(context.externalCacheDir, AUDIO_SOURCE_FILENAME)
        if (file.exists()) {
            Log.i(TAG, "writeIfAbsent: file exists")
            return
        }
        context.assets.open(AUDIO_ASSEST).use { inputstream ->
            FileOutputStream(file).use { outputstream ->
                val bytes = inputstream.readBytes()
                outputstream.write(bytes)
                Log.i(TAG, "writeIfAbsent: write file ok.")
            }
        }
    }

    fun getInputAudio(context: Context): String {
        return File(context.externalCacheDir, AUDIO_SOURCE_FILENAME).absolutePath
    }

    fun getOutputParentDir(context: Context): String? {
        return context.externalCacheDir?.absolutePath
    }
}