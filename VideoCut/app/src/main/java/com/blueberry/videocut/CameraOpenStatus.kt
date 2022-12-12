package com.blueberry.videocut

/**
 * Created by muyonggang on 2022/9/12
 * @author muyonggang@bytedance.com
 */
data class CameraOpenStatus (
    val isSuccess: Boolean,
    val errorCode:Int = 0
        ){

    companion object {
        const val CUSTOM_ERROR_DISCONNECT = -100
    }
}