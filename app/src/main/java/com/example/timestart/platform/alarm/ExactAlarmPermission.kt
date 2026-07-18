package com.example.timestart.platform.alarm

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object ExactAlarmPermission {
    fun requiresSpecialAccess(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.S

    fun canSchedule(context: Context): Boolean =
        !requiresSpecialAccess() || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun requestFrom(activity: Activity) {
        if (requiresSpecialAccess() && !canSchedule(activity)) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
        }
    }
}
