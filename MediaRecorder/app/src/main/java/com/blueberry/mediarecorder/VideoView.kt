package com.blueberry.mediarecorder

import android.view.Surface

/**
 * author: muyonggang
 * date: 2022/5/22
 */
interface VideoView {

    fun getPreviewSurface():Surface?
}