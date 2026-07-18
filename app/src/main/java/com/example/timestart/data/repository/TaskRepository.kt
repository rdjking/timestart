package com.example.timestart.data.repository

import com.example.timestart.data.local.ExecutionLogDao
import com.example.timestart.data.local.ExecutionLogEntity
import com.example.timestart.data.local.TaskDao
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.domain.scheduling.NextTriggerCalculator
import com.example.timestart.domain.holiday.HolidayCalendar
import com.example.timestart.domain.holiday.LocalWeekPatternHolidayCalendar
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class TaskRepository(
    private val taskDao: TaskDao,
    private val executionLogDao: ExecutionLogDao,
    private val scheduler: TaskScheduler,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
    private val holidayCalendar: HolidayCalendar = LocalWeekPatternHolidayCalendar,
) {
    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeAllOrderedByNextTrigger()

    fun getTask(taskId: Long): TaskEntity? = taskDao.getById(taskId)

    fun observeLogs(taskId: Long?): Flow<List<ExecutionLogEntity>> = if (taskId == null) {
        executionLogDao.observeAll()
    } else {
        executionLogDao.observeForTask(taskId)
    }

    fun clearLogsOlderThan(cutoffMillis: Long): Int = executionLogDao.deleteOlderThan(cutoffMillis)

    fun clearExpiredLogs(nowMillis: Long = System.currentTimeMillis()): Int =
        clearLogsOlderThan(nowMillis - Duration.ofDays(30).toMillis())

    fun save(task: TaskEntity): Long {
        val taskToSave = task.withNextTriggerIfNeeded()
        val id = taskDao.insert(taskToSave)
        if (taskToSave.enabled && taskToSave.nextTriggerAt != null) {
            scheduler.schedule(id)
        }
        return id
    }

    fun setEnabled(taskId: Long, enabled: Boolean) {
        if (!enabled) {
            taskDao.setEnabled(taskId, false)
            taskDao.setResumeAfterSkippedOccurrence(taskId, false)
            scheduler.cancel(taskId)
            executionLogDao.insert(
                ExecutionLogEntity(
                    taskId = taskId,
                    occurredAt = System.currentTimeMillis(),
                    eventType = "TASK_DISABLED",
                    resultCode = "OK",
                    message = "Task disabled by user",
                ),
            )
            return
        }

        val task = taskDao.getById(taskId) ?: return
        val nextTriggerAt = NextTriggerCalculator.nextOrNull(task.toScheduleTask(), now(), holidayCalendar)
            ?.toInstant()
            ?.toEpochMilli()

        taskDao.updateNextTriggerAt(taskId, nextTriggerAt)
        taskDao.setResumeAfterSkippedOccurrence(taskId, false)
        taskDao.setEnabled(taskId, nextTriggerAt != null)
        if (nextTriggerAt != null) {
            scheduler.schedule(taskId)
        } else {
            scheduler.cancel(taskId)
        }
    }

    /**
     * Temporarily turns off a task until its currently scheduled trigger has passed.
     * The original alarm is retained solely to restore the task; no target app is launched then.
     */
    fun skipCurrentOccurrence(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (!task.enabled) return

        val currentOccurrence = task.nextTriggerAt
            ?.let { Instant.ofEpochMilli(it).atZone(now().zone) }
            ?: NextTriggerCalculator.nextOrNull(task.toScheduleTask(), now(), holidayCalendar)
        if (currentOccurrence != null) {
            taskDao.updateNextTriggerAt(taskId, currentOccurrence.toInstant().toEpochMilli())
            taskDao.setEnabled(taskId, false)
            taskDao.setResumeAfterSkippedOccurrence(taskId, true)
            scheduler.schedule(taskId)
        } else {
            taskDao.setEnabled(taskId, false)
            taskDao.setResumeAfterSkippedOccurrence(taskId, false)
            scheduler.cancel(taskId)
        }
        executionLogDao.insert(
            ExecutionLogEntity(
                taskId = taskId,
                occurredAt = System.currentTimeMillis(),
                eventType = "OCCURRENCE_SKIPPED",
                resultCode = "OK",
                message = "Current scheduled occurrence skipped by user",
            ),
        )
    }

    fun update(task: TaskEntity) {
        require(task.id != 0L) { "A task must have an ID before it can be updated" }
        scheduler.cancel(task.id)
        val taskToSave = task.copy(nextTriggerAt = null).withNextTriggerIfNeeded()
        taskDao.insert(taskToSave)
        if (taskToSave.enabled && taskToSave.nextTriggerAt != null) {
            scheduler.schedule(taskToSave.id)
        }
        executionLogDao.insert(
            ExecutionLogEntity(
                taskId = task.id,
                occurredAt = System.currentTimeMillis(),
                eventType = "TASK_UPDATED",
                resultCode = "OK",
                message = "Task updated",
            ),
        )
    }

    fun copy(taskId: Long): Long {
        val source = requireNotNull(taskDao.getById(taskId)) { "Task $taskId does not exist" }
        val copiedTaskId = save(source.copy(id = 0, nextTriggerAt = null))
        executionLogDao.insert(
            ExecutionLogEntity(
                taskId = copiedTaskId,
                occurredAt = System.currentTimeMillis(),
                eventType = "TASK_COPIED",
                resultCode = "OK",
                message = "Task copied from $taskId",
            ),
        )
        return copiedTaskId
    }

    fun delete(taskId: Long) {
        scheduler.cancel(taskId)
        taskDao.deleteById(taskId)
        executionLogDao.insert(
            ExecutionLogEntity(
                taskId = taskId,
                occurredAt = System.currentTimeMillis(),
                eventType = "TASK_DELETED",
                resultCode = "OK",
                message = "Task deleted",
            ),
        )
    }

    private fun TaskEntity.withNextTriggerIfNeeded(): TaskEntity {
        if (!enabled || nextTriggerAt != null) return this
        val nextTriggerAt = NextTriggerCalculator.nextOrNull(toScheduleTask(), now(), holidayCalendar)
            ?.toInstant()
            ?.toEpochMilli()
        return copy(nextTriggerAt = nextTriggerAt)
    }
}
