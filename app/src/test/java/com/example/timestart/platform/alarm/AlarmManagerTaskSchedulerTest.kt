package com.example.timestart.platform.alarm

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.data.local.TimeStartDatabase
import com.example.timestart.platform.service.SchedulerForegroundService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class AlarmManagerTaskSchedulerTest {
    private lateinit var context: Context
    private lateinit var database: TimeStartDatabase
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TimeStartDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `schedules enabled task with a next trigger as an exact idle alarm`() {
        val triggerAt = 1_900_000_000_000L
        val taskId = database.taskDao().insert(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
                nextTriggerAt = triggerAt,
            ),
        )

        AlarmManagerTaskScheduler(context, database.taskDao(), alarmManager).schedule(taskId)

        val scheduled = Shadows.shadowOf(alarmManager).peekNextScheduledAlarm()
        assertNotNull(scheduled)
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled!!.type)
        assertEquals(triggerAt, scheduled.triggerAtTime)
        assertTrue(scheduled.allowWhileIdle)
        assertEquals(
            SchedulerForegroundService.ACTION_TRIGGER_TASK,
            Shadows.shadowOf(scheduled.operation).savedIntent.action,
        )
        assertEquals(
            taskId,
            Shadows.shadowOf(scheduled.operation).savedIntent.getLongExtra(
                SchedulerForegroundService.EXTRA_TASK_ID,
                -1L,
            ),
        )
    }

    @Test
    fun `cancel removes the task alarm`() {
        val taskId = database.taskDao().insert(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
                nextTriggerAt = 1_900_000_000_000L,
            ),
        )
        val scheduler = AlarmManagerTaskScheduler(context, database.taskDao(), alarmManager)
        scheduler.schedule(taskId)

        scheduler.cancel(taskId)

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `does not register an exact alarm when permission is unavailable`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val taskId = database.taskDao().insert(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
                nextTriggerAt = 1_900_000_000_000L,
            ),
        )

        AlarmManagerTaskScheduler(context, database.taskDao(), alarmManager).schedule(taskId)

        assertTrue(Shadows.shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }
}
