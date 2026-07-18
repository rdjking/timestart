package com.example.timestart.domain.recovery

import com.example.timestart.data.local.TaskDao
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.domain.scheduling.NextTriggerCalculator
import com.example.timestart.domain.holiday.HolidayCalendar
import com.example.timestart.domain.holiday.LocalWeekPatternHolidayCalendar
import java.time.ZonedDateTime

/** Rebuilds system alarms from durable task state after system time or process state changes. */
class TaskRescheduler(
    private val taskDao: TaskDao,
    private val scheduler: TaskScheduler,
    private val holidayCalendar: HolidayCalendar = LocalWeekPatternHolidayCalendar,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) {
    fun rescheduleEnabledTasks() {
        val recoveryTime = now()
        taskDao.getScheduledOrPendingResumeTasks().forEach { entity ->
            if (entity.resumeAfterSkippedOccurrence) {
                val skippedOccurrence = entity.nextTriggerAt
                    ?.let { java.time.Instant.ofEpochMilli(it).atZone(recoveryTime.zone) }
                if (skippedOccurrence != null && skippedOccurrence.isAfter(recoveryTime)) {
                    scheduler.schedule(entity.id)
                    return@forEach
                }

                val nextTrigger = NextTriggerCalculator.nextOrNull(
                    entity.toScheduleTask(),
                    skippedOccurrence?.plusNanos(1) ?: recoveryTime,
                    holidayCalendar,
                )
                taskDao.updateNextTriggerAt(entity.id, nextTrigger?.toInstant()?.toEpochMilli())
                taskDao.setResumeAfterSkippedOccurrence(entity.id, false)
                taskDao.setEnabled(entity.id, nextTrigger != null)
                if (nextTrigger == null) scheduler.cancel(entity.id) else scheduler.schedule(entity.id)
                return@forEach
            }
            val nextTrigger = NextTriggerCalculator.nextOrNull(entity.toScheduleTask(), recoveryTime, holidayCalendar)
            if (nextTrigger == null) {
                taskDao.updateNextTriggerAt(entity.id, null)
                taskDao.setEnabled(entity.id, false)
                scheduler.cancel(entity.id)
            } else {
                taskDao.updateNextTriggerAt(entity.id, nextTrigger.toInstant().toEpochMilli())
                scheduler.schedule(entity.id)
            }
        }
    }
}
