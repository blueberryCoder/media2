package com.blueberry.mediarecorder.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * author: muyonggang
 * date: 2022/6/5
 */
object PermissionUtil {

    private const val REQUEST_CODE_CAMERA_PERMISSION = 100

    fun isHavePermissions(context: Context) = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun requestPermissions(act: Activity) {
        ActivityCompat.requestPermissions(
            act,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            REQUEST_CODE_CAMERA_PERMISSION
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        callback: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            callback.invoke()
        }
    }
}