package com.example.timestart.domain.execution

import com.example.timestart.data.local.ExecutionLogDao
import com.example.timestart.data.local.ExecutionLogEntity
import com.example.timestart.data.local.TaskDao
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.domain.holiday.HolidayCalendar
import com.example.timestart.domain.holiday.LocalWeekPatternHolidayCalendar
import com.example.timestart.domain.model.ScheduleRule
import com.example.timestart.domain.scheduling.NextTriggerCalculator
import java.time.ZonedDateTime

/** Handles a fired alarm by making its next occurrence durable before requesting the app launch. */
class TaskTriggerCoordinator(
    private val taskDao: TaskDao,
    private val executionLogDao: ExecutionLogDao,
    private val scheduler: TaskScheduler,
    private val launchExecutor: LaunchExecutor,
    private val notificationFallback: NotificationFallback,
    private val holidayCalendar: HolidayCalendar = LocalWeekPatternHolidayCalendar,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) {
    fun handleAlarm(taskId: Long) {
        val entity = taskDao.getById(taskId) ?: run {
            appendLog(taskId, "ALARM_IGNORED", "TASK_NOT_FOUND", "Task was deleted before its alarm fired")
            return
        }
        val triggerTime = now()

        if (entity.resumeAfterSkippedOccurrence) {
            val skippedOccurrence = entity.nextTriggerAt
                ?.let { java.time.Instant.ofEpochMilli(it).atZone(triggerTime.zone) }
            val nextTrigger = NextTriggerCalculator.nextOrNull(
                entity.toScheduleTask(),
                skippedOccurrence?.plusNanos(1) ?: triggerTime,
            )
            taskDao.updateNextTriggerAt(taskId, nextTrigger?.toInstant()?.toEpochMilli())
            taskDao.setResumeAfterSkippedOccurrence(taskId, false)
            taskDao.setEnabled(taskId, nextTrigger != null)
            if (nextTrigger == null) {
                scheduler.cancel(taskId)
            } else {
                scheduler.schedule(taskId)
            }
            appendLog(taskId, "SKIP_COMPLETED", "OK", "Skipped occurrence passed; task restored")
            return
        }

        if (!entity.enabled) {
            scheduler.cancel(taskId)
            appendLog(taskId, "ALARM_IGNORED", "TASK_DISABLED", "Task is disabled")
            return
        }

        val scheduleTask = entity.toScheduleTask()
        val nextTrigger = NextTriggerCalculator.nextOrNull(scheduleTask, triggerTime)
        if (nextTrigger == null) {
            taskDao.updateNextTriggerAt(taskId, null)
            taskDao.setEnabled(taskId, false)
            scheduler.cancel(taskId)
        } else {
            taskDao.updateNextTriggerAt(taskId, nextTrigger.toInstant().toEpochMilli())
            scheduler.schedule(taskId)
        }

        val holidayDay = when (scheduleTask.rule) {
            ScheduleRule.StatutoryWorkday,
            ScheduleRule.HolidayOrWeekend,
            -> holidayCalendar.dayInfo(triggerTime.toLocalDate())

            else -> null
        }
        val shouldLaunch = when (scheduleTask.rule) {
            ScheduleRule.StatutoryWorkday -> holidayDay!!.type.isStatutoryWorkday
            ScheduleRule.HolidayOrWeekend -> holidayDay!!.type.isHolidayOrWeekend
            else -> true
        }
        if (!shouldLaunch) {
            appendLog(
                taskId = taskId,
                eventType = "HOLIDAY_RULE_SKIPPED",
                resultCode = holidayDay!!.type.name,
                message = "Holiday rule skipped this date using ${holidayDay.source.name} data",
            )
            return
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
