package com.blueberry.mediarecorder.data

/**
 * author: muyonggang
 * date: 2022/5/22
 */
data class CameraInfo(
    val cameraId: String? = null,
    // 前置还是后置
    val lenFacing: Int = -1,
    // 输出的数据旋转角度
    val orientation: Int? = null
)