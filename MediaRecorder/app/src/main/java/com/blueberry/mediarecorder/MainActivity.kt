package com.blueberry.mediarecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blueberry.mediarecorder.R
import android.content.Intent
import android.view.View
import com.blueberry.mediarecorder.PageActivity
import com.blueberry.mediarecorder.VideoActivity

/**
 * author: muyonggang
 * date: 2022/5/20
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnStartPageView).setOnClickListener {
            val intent = Intent(this@MainActivity, PageActivity::class.java)
            this@MainActivity.startActivity(intent)
        }
        findViewById<View>(R.id.btnStartVideo).setOnClickListener {
            val intent = Intent(this@MainActivity, VideoActivity::class.java)
            this@MainActivity.startActivity(intent)
        }
    }
}