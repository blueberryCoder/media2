package com.blueberry.audioeffect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_equalizer).setOnClickListener {
            val intent = Intent(this@MainActivity, EqualizerActivity::class.java)
            this@MainActivity.startActivity(intent)
        }
        findViewById<View>(R.id.btn_compressor).setOnClickListener { }
        findViewById<View>(R.id.btn_reverb).setOnClickListener { }

        Log.i(TAG, "onCreate: ")
        thread {
            Log.i(TAG, "onCreate: thread block")
            ResourceUtil.writeIfAbsent(this)
        }
    }
}