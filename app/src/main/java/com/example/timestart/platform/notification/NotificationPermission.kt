package com.example.timestart.platform.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Android 13+ notification permission policy, kept outside receivers because receivers cannot show consent UI. */
object NotificationPermission {
    const val REQUEST_CODE = 3101

    fun isRuntimePermissionRequired(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun hasPermission(context: Context): Boolean =
        !isRuntimePermissionRequired() ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun requestFrom(activity: Activity, requestCode: Int = REQUEST_CODE) {
        if (isRuntimePermissionRequired() && !hasPermission(activity)) {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
        }
    }
}
