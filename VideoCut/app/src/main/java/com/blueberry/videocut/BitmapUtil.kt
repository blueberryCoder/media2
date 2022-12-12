package com.blueberry.videocut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Created by blueberry on 2022/9/17
 * @author blueberrymyg@gmail.com
 */
object BitmapUtil {

    fun createBitmapFromId(
        context: Context,
        rid: Int
    ): Bitmap {
        return BitmapFactory.decodeResource(context.resources, rid)
    }

}