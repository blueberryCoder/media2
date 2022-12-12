package com.blueberry.videocut

import android.util.Size

/**
 * Created by blueberry on 2022/9/11
 * @author blueberrymyg@gmail.com
 */
data class CameraInfo(val cameraId: String, val surfaceHolderSupportedSize: Array<Size>?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraInfo) return false

        if (cameraId != other.cameraId) return false
        if (!surfaceHolderSupportedSize.contentEquals(other.surfaceHolderSupportedSize)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cameraId.hashCode()
        result = 31 * result + surfaceHolderSupportedSize.contentHashCode()
        return result
    }
}
