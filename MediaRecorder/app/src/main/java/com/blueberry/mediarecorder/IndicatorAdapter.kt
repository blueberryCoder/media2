package com.blueberry.mediarecorder

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.blueberry.mediarecorder.IndicatorAdapter.MyViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.blueberry.mediarecorder.R
import com.blueberry.mediarecorder.IndicatorAdapter
import android.widget.FrameLayout

/**
 * author: muyonggang
 * date: 2022/5/20
 */
class IndicatorAdapter : RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val inflate = inflater.inflate(R.layout.item_dot_view, parent, false)
        return MyViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.i(TAG, "onBindViewHolder: ")
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return 10
    }

    fun changeConfig(
        curr: Int,
        recyclerView: RecyclerView?,
        layoutManager: RecyclerView.LayoutManager
    ) {
        var curr = curr
        curr = curr % itemCount
        for (i in 0 until layoutManager.childCount) {
            val itemView = layoutManager.findViewByPosition(i)
            val dotView = itemView!!.findViewById<View>(R.id.dotView)
            if (i == curr) {
                dotView.setBackgroundResource(R.drawable.long_drawable)
                val params = dotView.layoutParams as FrameLayout.LayoutParams
                params.height = dp2px(dotView.context, 16)
                dotView.layoutParams = params
            } else {
                dotView.setBackgroundResource(R.drawable.short_drawable)
                val params = dotView.layoutParams as FrameLayout.LayoutParams
                params.height = dp2px(dotView.context, 8)
                dotView.layoutParams = params
            }
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var dotView: View
        fun setIsCurrent(flag: Boolean) {
            if (flag) {
                dotView.setBackgroundResource(R.drawable.long_drawable)
            } else {
                dotView.setBackgroundResource(R.drawable.short_drawable)
            }
        }

        init {
            dotView = itemView.findViewById(R.id.dotView)
        }
    }

    companion object {
        private const val TAG = "IndicatorAdapter"
        fun dp2px(context: Context, dp: Int): Int {
            return Math.round(context.resources.displayMetrics.density * dp)
        }
    }
}