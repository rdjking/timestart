package com.example.timestart.domain.recovery

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.data.local.TimeStartDatabase
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.domain.model.ScheduleRule
import com.example.timestart.domain.model.ScheduleTask
import java.time.ZonedDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskReschedulerTest {
    private lateinit var database: TimeStartDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TimeStartDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `recalculates and schedules every enabled task only`() {
        val now = ZonedDateTime.parse("2026-07-17T18:00:00+08:00[Asia/Shanghai]")
        val enabledId = database.taskDao().insert(
            TaskEntity.from(
                ScheduleTask(
                    packageName = "com.tencent.wework",
                    appLabel = "企业微信",
                    hour = 18,
                    minute = 0,
                    rule = ScheduleRule.Daily,
                ),
            ).copy(nextTriggerAt = 1L),
        )
        val disabledId = database.taskDao().insert(
            TaskEntity.from(
                ScheduleTask(
                    packageName = "com.alibaba.android.rimet",
                    appLabel = "钉钉",
                    hour = 9,
                    minute = 0,
                    rule = ScheduleRule.Daily,
                ),
            ).copy(enabled = false, nextTriggerAt = 2L),
        )
        val scheduler = RecordingScheduler()
        val rescheduler = TaskRescheduler(
            taskDao = database.taskDao(),
            scheduler = scheduler,
            now = { now },
        )

        rescheduler.rescheduleEnabledTasks()

        assertEquals(listOf(enabledId), scheduler.scheduledTaskIds)
        assertEquals(
            now.plusDays(1).toInstant().toEpochMilli(),
            database.taskDao().getById(enabledId)?.nextTriggerAt,
        )
        assertEquals(2L, database.taskDao().getById(disabledId)?.nextTriggerAt)
    }

    @Test
    fun `disables an expired one time task during recovery`() {
        val now = ZonedDateTime.parse("2026-07-17T18:00:00+08:00[Asia/Shanghai]")
        val expiredId = database.taskDao().insert(
            TaskEntity.from(
                ScheduleTask(
                    packageName = "com.tencent.wework",
                    appLabel = "企业微信",
                    hour = 18,
                    minute = 0,
                    rule = ScheduleRule.Once(now.minusMinutes(1)),
                ),
            ).copy(nextTriggerAt = now.minusMinutes(1).toInstant().toEpochMilli()),
        )
        val scheduler = RecordingScheduler()
        val rescheduler = TaskRescheduler(database.taskDao(), scheduler) { now }

        rescheduler.rescheduleEnabledTasks()

        assertEquals(emptyList<Long>(), scheduler.scheduledTaskIds)
        assertEquals(false, database.taskDao().getById(expiredId)?.enabled)
        assertEquals(null, database.taskDao().getById(expiredId)?.nextTriggerAt)
    }

    private class RecordingScheduler : TaskScheduler {
        val scheduledTaskIds = mutableListOf<Long>()

        override fun schedule(taskId: Long) {
            scheduledTaskIds += taskId
        }

        override fun cancel(taskId: Long) = Unit
    }
}
