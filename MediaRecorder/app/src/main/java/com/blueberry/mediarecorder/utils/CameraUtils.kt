package com.blueberry.mediarecorder.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import com.blueberry.mediarecorder.Constants
import com.blueberry.mediarecorder.data.CameraInfo
import java.io.File
import java.util.ArrayList

/**
 * author: muyonggang
 * date: 2022/5/22
 */
object CameraUtils {
    private const val TAG = "CameraUtils"
    fun enumerateCameras(cameraManager: CameraManager): ArrayList<CameraInfo> {
        val cameraInfoList = arrayListOf<CameraInfo>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            for (cameraId in cameraIdList) {
                val cameraCharacteristics =
                    cameraManager.getCameraCharacteristics(cameraId) ?: return cameraInfoList
                val lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                val capabilities =
                    cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val fpsRange =cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                Log.i(TAG, "cameraId:${fpsRange},fpsRange:${fpsRange}:")
                val containsBackward = containsBackward(capabilities)
                if (containsBackward) {
                    val streamConfigurationMap =
                        cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputFormats = streamConfigurationMap!!.outputFormats
                    val outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)
                }
                cameraInfoList.add(CameraInfo(cameraId, lensFacing ?: -1))
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

    fun getRecordVideoPath(context: Context): File {
        val file = File(context.externalCacheDir, Constants.H264_FILE)
        if (file.parentFile?.exists()?.not() == true) {
            file.parentFile?.mkdirs()
        }
        return file
    }

    fun getRecordAudioPath(context: Context): File {
        val file = File(context.externalCacheDir, Constants.AAC_FILE)
        if (file.parentFile?.exists()?.not() == true) {
            file.parentFile?.mkdirs()
        }
        return file
    }

    fun geMp4FilePath(context: Context): File {
        val file = File(context.externalCacheDir, Constants.MP4_FILE)
        if (file.parentFile?.exists()?.not() == true) {
            file.parentFile?.mkdirs()
        }
        return file
    }

    infix fun Array<Size>.and(second: Array<Size>): Array<Size> {
        return arraySizeAnd(this, second)
    }

    private fun arraySizeAnd(first: Array<Size>, second: Array<Size>): Array<Size> {
        val set = hashSetOf<Size>()
        first.forEach { set.add(it) }
        return second.filter { set.contains(it) }.toTypedArray()
    }

}