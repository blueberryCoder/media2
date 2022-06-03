package com.blueberry.mediarecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blueberry.mediarecorder.R
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import com.blueberry.mediarecorder.PageActivity
import com.blueberry.mediarecorder.VideoActivity
import com.blueberry.mediarecorder.screen.ScreenTest

/**
 * author: muyonggang
 * date: 2022/5/20
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartPageView = findViewById<View>(R.id.btnStartPageView)
        btnStartPageView.setOnClickListener {
            val intent = Intent(this@MainActivity, PageActivity::class.java)
            this@MainActivity.startActivity(intent)
        }

        val btnStartVideo = findViewById<View>(R.id.btnStartVideo)
        btnStartVideo.setOnClickListener {
            val intent = Intent(this@MainActivity, VideoActivity::class.java)
            this@MainActivity.startActivity(intent)
        }

        findViewById<View>(R.id.btnScreenTest).setOnClickListener {
            val intent = Intent(this@MainActivity, ScreenTest::class.java)
            this@MainActivity.startActivity(intent)
        }

//        btnStartPageView.visibility = View.GONE
//        btnStartVideo.visibility = View.GONE

        val textView = findViewById<TextView>(R.id.tvLog)

        val sb = StringBuffer()
        sb.append("sdkinit:${Build.VERSION.SDK_INT}")

        textView.setText(sb.toString())

    }
}