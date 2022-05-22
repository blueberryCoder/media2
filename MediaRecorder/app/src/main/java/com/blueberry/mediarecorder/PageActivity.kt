package com.blueberry.mediarecorder

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import android.os.Bundle
import android.view.View
import com.blueberry.mediarecorder.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.blueberry.mediarecorder.IndicatorAdapter
import java.util.ArrayList

class PageActivity : AppCompatActivity() {
    var mRecyclerView: RecyclerView? = null
    var mTabLayout: TabLayout? = null
    private val mTabs = ArrayList<TabLayout.Tab>()
    private var count = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)
        mRecyclerView = findViewById(R.id.mRecyclerView)
        val manager = LinearLayoutManager(this)
        mRecyclerView?.setLayoutManager(manager)
        val adapter = IndicatorAdapter()
        mRecyclerView?.setAdapter(adapter)
        findViewById<View>(R.id.btnNext).setOnClickListener {
            adapter.changeConfig(count++, mRecyclerView, manager)
            mTabLayout!!.selectTab(mTabs[count % mTabs.size], true)
        }
        mTabLayout = findViewById(R.id.btnTabLayout)
        for (i in 0..9) {
            val tab = mTabLayout?.newTab()?:continue
            mTabs.add(tab)
            mTabLayout?.addTab(tab)
        }
    }
}