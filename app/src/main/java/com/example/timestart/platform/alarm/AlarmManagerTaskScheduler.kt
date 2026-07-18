package com.example.timestart.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.timestart.MainActivity
import com.example.timestart.data.local.TaskDao
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.platform.service.SchedulerForegroundService

/** Schedules one user-visible exact alarm for each enabled task. */
class AlarmManagerTaskScheduler(
    context: Context,
    private val taskDao: TaskDao,
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java),
) : TaskScheduler {
    private val appContext = context.applicationContext

    override fun schedule(taskId: Long) {
        val task = taskDao.getById(taskId)
        val triggerAt = task?.nextTriggerAt
        if (task == null || !task.enabled || triggerAt == null) {
            cancel(taskId)
            return
        }

        if (!canScheduleExactAlarms()) {
            cancel(taskId)
            return
        }

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showIntentFor(taskId)),
            pendingIntentFor(taskId),
        )
    }

    override fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntentFor(taskId))
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntentFor(taskId: Long): PendingIntent {
        val intent = Intent(appContext, SchedulerForegroundService::class.java)
            .setAction(SchedulerForegroundService.ACTION_TRIGGER_TASK)
            .putExtra(SchedulerForegroundService.EXTRA_TASK_ID, taskId)

        return PendingIntent.getForegroundService(
            appContext,
            TaskRequestCode.forTask(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showIntentFor(taskId: Long): PendingIntent = PendingIntent.getActivity(
        appContext,
        TaskRequestCode.forTask(taskId),
        (appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(appContext, MainActivity::class.java))
            .setAction("com.example.timestart.action.SHOW_SCHEDULE")
            .putExtra(SchedulerForegroundService.EXTRA_TASK_ID, taskId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
