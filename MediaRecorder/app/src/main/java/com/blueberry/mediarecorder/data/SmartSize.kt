package com.blueberry.mediarecorder.data

/**
 * author: muyonggang
 * date: 2022/5/28
 */
data class SmartSize(val width:Int,val height:Int) {
    companion object {
        val SIZE_NONE =  SmartSize(0,0)
        val SIZE_1080P: SmartSize = SmartSize(1920, 1080)
    }
}
