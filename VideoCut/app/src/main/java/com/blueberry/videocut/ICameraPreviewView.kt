package com.blueberry.videocut

import android.view.Surface

/**
 * Created by blueberry on 2022/9/11
 * @author blueberrymyg@gmail.com
 */
interface ICameraPreviewView {

    fun getPreviewSurface(): Surface
}