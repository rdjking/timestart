package com.example.timestart.domain.recovery

import com.example.timestart.data.local.TaskDao
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.domain.scheduling.NextTriggerCalculator
import java.time.ZonedDateTime

/** Rebuilds system alarms from durable task state after system time or process state changes. */
class TaskRescheduler(
    private val taskDao: TaskDao,
    private val scheduler: TaskScheduler,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) {
    fun rescheduleEnabledTasks() {
        val recoveryTime = now()
        taskDao.getEnabledOrderedByNextTrigger().forEach { entity ->
            val nextTrigger = NextTriggerCalculator.nextOrNull(entity.toScheduleTask(), recoveryTime)
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
