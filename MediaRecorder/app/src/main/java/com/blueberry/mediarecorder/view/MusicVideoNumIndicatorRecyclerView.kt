package com.blueberry.mediarecorder.view

import android.content.Context
import android.util.AttributeSet
import kotlin.jvm.JvmOverloads
import androidx.recyclerview.widget.RecyclerView
import android.view.MotionEvent

/**
 * author: muyonggang
 * date: 2022/5/20
 */
class MusicVideoNumIndicatorRecyclerView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0,
    defStyleRes: Int = 0
) : RecyclerView(
    context!!, attrs, defStyle
) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    init {
        setHasFixedSize(true)
        clipToPadding = false
    }
}