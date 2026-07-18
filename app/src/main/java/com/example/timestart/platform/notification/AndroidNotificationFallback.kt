package com.example.timestart.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.timestart.domain.execution.NotificationFallback
import com.example.timestart.domain.execution.NotificationFallbackResult
import com.example.timestart.platform.alarm.TaskRequestCode

/** Posts the user-visible fallback notification associated with an alarm trigger. */
class AndroidNotificationFallback(
    private val context: Context,
) : NotificationFallback {
    override fun post(
        taskId: Long,
        appLabel: String,
        packageName: String,
    ): NotificationFallbackResult {
        if (!NotificationPermission.hasPermission(context)) {
            return NotificationFallbackResult.PermissionDenied
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (!notificationManager.areNotificationsEnabled()) {
            return NotificationFallbackResult.PermissionDenied
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return NotificationFallbackResult.TargetUnavailable

        return try {
            createChannel(notificationManager)
            val pendingIntent = PendingIntent.getActivity(
                context,
                TaskRequestCode.forTask(taskId),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("定时启动：$appLabel")
                .setContentText("点按打开$appLabel")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(TaskRequestCode.forTask(taskId), notification)
            NotificationFallbackResult.Displayed
        } catch (exception: SecurityException) {
            NotificationFallbackResult.Failed(exception.message ?: "Android denied notification access")
        }
    }

    private fun createChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "定时启动提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "定时启动任务的点按打开入口"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "scheduled_launch_fallback"
    }
}
