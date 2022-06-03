package com.blueberry.mediarecorder.screen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.widget.TextView
import com.blueberry.mediarecorder.R

class ScreenTest : AppCompatActivity() {

    private var mTvLog:TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        mTvLog = findViewById(R.id.tvLog)

        val sb = StringBuffer()
        this.registerReceiver(
            object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val powManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
                val keyGuardManager = context?.getSystemService(Context.KEYGUARD_SERVICE)as KeyguardManager
                val interactive = powManager.isInteractive
                val screenLocked = keyGuardManager.isDeviceLocked
                sb.append("interactive:${interactive},screenLocked:${screenLocked}\n")

                mTvLog?.post {
                    mTvLog?.text = sb.toString()
                }
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF))

    }
}