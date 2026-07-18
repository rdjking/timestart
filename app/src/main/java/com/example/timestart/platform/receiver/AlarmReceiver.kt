package com.example.timestart.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timestart.TimeStartApplication
import java.util.concurrent.Executors

/** Entry point invoked by AlarmManager when a scheduled task reaches its trigger time. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_TASK) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, NO_TASK_ID)
        if (taskId == NO_TASK_ID) {
            Log.w(TAG, "Ignoring task alarm without a task ID")
            return
        }
        Log.i(TAG, "Received exact alarm for task=$taskId")

        val pendingResult = goAsync()
        receiverExecutor.execute {
            try {
                val application = context.applicationContext as? TimeStartApplication
                if (application == null) {
                    Log.e(TAG, "Application is not configured as TimeStartApplication")
                    return@execute
                }
                application.taskTriggerCoordinator.handleAlarm(taskId)
                Log.i(TAG, "Finished trigger coordination for task=$taskId")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_TASK = "com.example.timestart.action.TRIGGER_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"

        private const val NO_TASK_ID = -1L
        private const val TAG = "AlarmReceiver"
        private val receiverExecutor = Executors.newSingleThreadExecutor()
    }
}
