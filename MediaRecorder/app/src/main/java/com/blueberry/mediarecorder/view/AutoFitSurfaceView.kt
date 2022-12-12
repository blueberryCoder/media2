package com.blueberry.mediarecorder.view

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import kotlin.math.roundToInt

/**
 * author: muyonggang
 * date: 2022/5/22
 */
class AutoFitSurfaceView @JvmOverloads
constructor(context: Context? = null, attributeSet: AttributeSet? = null, defStyle: Int = 0) :
    SurfaceView(context, attributeSet) {
    private var aspectRatio: Float = 0F
    fun setAspectRatio(width: Int, height: Int) {
        this.aspectRatio = width.toFloat() / height.toFloat()
        holder.setFixedSize(width, height)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        if (aspectRatio == 0f) {
            setMeasuredDimension(width, height)
        } else {
            val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio
            val newWidth: Int
            val newHeight: Int
            if (width < height * actualRatio) {
                newHeight = height
                newWidth = (height * actualRatio).roundToInt()
            } else {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            }
            setMeasuredDimension(newWidth, newHeight)
        }
    }
}