package com.example.timestart.domain.execution

import com.example.timestart.data.local.ExecutionLogDao
import com.example.timestart.data.local.ExecutionLogEntity
import com.example.timestart.data.local.TaskDao
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.domain.scheduling.NextTriggerCalculator
import java.time.ZonedDateTime

/** Handles a fired alarm by making its next occurrence durable before requesting the app launch. */
class TaskTriggerCoordinator(
    private val taskDao: TaskDao,
    private val executionLogDao: ExecutionLogDao,
    private val scheduler: TaskScheduler,
    private val launchExecutor: LaunchExecutor,
    private val notificationFallback: NotificationFallback,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) {
    fun handleAlarm(taskId: Long) {
        val entity = taskDao.getById(taskId) ?: run {
            appendLog(taskId, "ALARM_IGNORED", "TASK_NOT_FOUND", "Task was deleted before its alarm fired")
            return
        }

        if (!entity.enabled) {
            scheduler.cancel(taskId)
            appendLog(taskId, "ALARM_IGNORED", "TASK_DISABLED", "Task is disabled")
            return
        }

        val nextTrigger = NextTriggerCalculator.nextOrNull(entity.toScheduleTask(), now())
        if (nextTrigger == null) {
            taskDao.updateNextTriggerAt(taskId, null)
            taskDao.setEnabled(taskId, false)
            scheduler.cancel(taskId)
        } else {
            taskDao.updateNextTriggerAt(taskId, nextTrigger.toInstant().toEpochMilli())
            scheduler.schedule(taskId)
        }

        val launchResult = launchExecutor.requestLaunch(entity.packageName)
        appendLog(
            taskId = taskId,
            eventType = "LAUNCH_REQUEST",
            resultCode = launchResult.logCode,
            message = launchResult.logMessage,
        )

        val notificationResult = notificationFallback.post(
            taskId = taskId,
            appLabel = entity.appLabel,
            packageName = entity.packageName,
        )
        appendLog(
            taskId = taskId,
            eventType = "NOTIFICATION_FALLBACK",
            resultCode = notificationResult.logCode,
            message = notificationResult.logMessage,
        )
    }

    private fun appendLog(taskId: Long, eventType: String, resultCode: String, message: String) {
        executionLogDao.insert(
            ExecutionLogEntity(
                taskId = taskId,
                occurredAt = System.currentTimeMillis(),
                eventType = eventType,
                resultCode = resultCode,
                message = message,
            ),
        )
    }
}
