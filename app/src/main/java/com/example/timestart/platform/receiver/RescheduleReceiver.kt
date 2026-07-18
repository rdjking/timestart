package com.example.timestart.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timestart.TimeStartApplication
import java.util.concurrent.Executors

/** Restores system alarms after boot, package replacement, or a device clock change. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isRecoveryAction(intent.action)) return

        val pendingResult = goAsync()
        receiverExecutor.execute {
            try {
                val application = context.applicationContext as? TimeStartApplication
                if (application == null) {
                    Log.e(TAG, "Application is not configured as TimeStartApplication")
                    return@execute
                }
                application.taskRescheduler.rescheduleEnabledTasks()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun isRecoveryAction(action: String?): Boolean = action in RECOVERY_ACTIONS

        private val RECOVERY_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
        private const val TAG = "RescheduleReceiver"
        private val receiverExecutor = Executors.newSingleThreadExecutor()
    }
}
