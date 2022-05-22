package com.blueberry.mediarecorder

import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import com.blueberry.mediarecorder.data.CameraInfo
import java.util.ArrayList

/**
 * author: muyonggang
 * date: 2022/5/22
 */
object  CameraUtils {
    private const val TAG = "CameraUtils"
    fun enumerateCameras(cameraManager: CameraManager) : ArrayList<CameraInfo> {
        val cameraInfoList = arrayListOf<CameraInfo>()
        try {
            val cameraIdList = cameraManager?.cameraIdList ?: return cameraInfoList
            for (cameraId in cameraIdList) {
                val cameraCharacteristics =
                    cameraManager?.getCameraCharacteristics(cameraId) ?: return cameraInfoList
                val lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                val capabilities =
                    cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val containsBackward = containsBackward(capabilities)
                if (containsBackward) {
                    val streamConfigurationMap =
                        cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputFormats = streamConfigurationMap!!.outputFormats
                    val outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)
                }
                cameraInfoList.add(CameraInfo(cameraId,lensFacing?:-1))
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "onCreate: e" + e.message)
            e.printStackTrace()
        }
        return cameraInfoList
    }

    private fun containsBackward(ints: IntArray?): Boolean {
        var result = false
        for (i in ints!!) {
            if (i == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                result = true
            }
        }
        return result
    }

}