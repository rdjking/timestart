package com.example.timestart.domain.execution

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
class TaskTriggerCoordinatorTest {
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
    fun `reschedules a recurring task before requesting its app launch`() {
        val now = ZonedDateTime.parse("2026-07-17T18:00:00+08:00[Asia/Shanghai]")
        val taskId = database.taskDao().insert(
            TaskEntity.from(
                ScheduleTask(
                    packageName = "com.tencent.wework",
                    appLabel = "企业微信",
                    hour = 18,
                    minute = 0,
                    rule = ScheduleRule.Daily,
                ),
            ).copy(nextTriggerAt = now.toInstant().toEpochMilli()),
        )
        val scheduler = RecordingScheduler()
        val launcher = RecordingLauncher()
        val coordinator = TaskTriggerCoordinator(
            taskDao = database.taskDao(),
            executionLogDao = database.executionLogDao(),
            scheduler = scheduler,
            launchExecutor = launcher,
            notificationFallback = RecordingNotificationFallback(),
            now = { now },
        )

        coordinator.handleAlarm(taskId)

        assertEquals(listOf(taskId), scheduler.scheduledTaskIds)
        assertEquals(listOf("com.tencent.wework"), launcher.requestedPackages)
        assertEquals(
            now.plusDays(1).toInstant().toEpochMilli(),
            database.taskDao().getById(taskId)?.nextTriggerAt,
        )
    }

    @Test
    fun `posts a notification fallback after requesting a target app launch`() {
        val now = ZonedDateTime.parse("2026-07-17T18:00:00+08:00[Asia/Shanghai]")
        val taskId = database.taskDao().insert(
            TaskEntity.from(
                ScheduleTask(
                    packageName = "com.tencent.wework",
                    appLabel = "企业微信",
                    hour = 18,
                    minute = 0,
                    rule = ScheduleRule.Daily,
                ),
            ).copy(nextTriggerAt = now.toInstant().toEpochMilli()),
        )
        val notifications = RecordingNotificationFallback()
        val coordinator = TaskTriggerCoordinator(
            taskDao = database.taskDao(),
            executionLogDao = database.executionLogDao(),
            scheduler = RecordingScheduler(),
            launchExecutor = RecordingLauncher(),
            notificationFallback = notifications,
            now = { now },
        )

        coordinator.handleAlarm(taskId)

        assertEquals(listOf(taskId to "企业微信"), notifications.postedTasks)
        val notificationLog = database.executionLogDao().getForTask(taskId)
            .first { it.eventType == "NOTIFICATION_FALLBACK" }
        assertEquals("DISPLAYED", notificationLog.resultCode)
    }

    private class RecordingScheduler : TaskScheduler {
        val scheduledTaskIds = mutableListOf<Long>()

        override fun schedule(taskId: Long) {
            scheduledTaskIds += taskId
        }

        override fun cancel(taskId: Long) = Unit
    }

    private class RecordingLauncher : LaunchExecutor {
        val requestedPackages = mutableListOf<String>()

        override fun requestLaunch(packageName: String): LaunchRequestResult {
            requestedPackages += packageName
            return LaunchRequestResult.Requested
        }
    }

    private class RecordingNotificationFallback : NotificationFallback {
        val postedTasks = mutableListOf<Pair<Long, String>>()

        override fun post(taskId: Long, appLabel: String, packageName: String): NotificationFallbackResult {
            postedTasks += taskId to appLabel
            return NotificationFallbackResult.Displayed
        }
    }
}
