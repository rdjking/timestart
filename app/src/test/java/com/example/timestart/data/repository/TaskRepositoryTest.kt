package com.example.timestart.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.data.local.TimeStartDatabase
import com.example.timestart.domain.holiday.HolidayCalendar
import com.example.timestart.domain.holiday.HolidayDataSource
import com.example.timestart.domain.holiday.HolidayDayInfo
import com.example.timestart.domain.holiday.HolidayDayType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZonedDateTime
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TaskRepositoryTest {
    private lateinit var database: TimeStartDatabase
    private lateinit var scheduler: RecordingScheduler
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TimeStartDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = RecordingScheduler()
        repository = TaskRepository(database.taskDao(), database.executionLogDao(), scheduler)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `save persists an enabled task and schedules it`() {
        val id = repository.save(
            TaskEntity(packageName = "com.tencent.wework", appLabel = "企业微信", hour = 18, minute = 0, ruleType = "WEEKDAY", nextTriggerAt = 100),
        )

        assertEquals("com.tencent.wework", database.taskDao().getById(id)?.packageName)
        assertEquals(listOf(id), scheduler.scheduledIds)
    }

    @Test
    fun `save calculates next trigger before scheduling an enabled task`() {
        val now = ZonedDateTime.parse("2026-07-17T17:30:00+08:00[Asia/Shanghai]")
        val deterministicRepository = TaskRepository(
            database.taskDao(),
            database.executionLogDao(),
            scheduler,
            now = { now },
        )

        val id = deterministicRepository.save(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "DAILY",
            ),
        )

        assertEquals(now.plusMinutes(30).toInstant().toEpochMilli(), database.taskDao().getById(id)?.nextTriggerAt)
        assertEquals(listOf(id), scheduler.scheduledIds)
    }

    @Test
    fun `save skips Sunday when calculating a statutory workday task`() {
        val now = ZonedDateTime.parse("2026-07-18T21:41:00+08:00[Asia/Shanghai]")
        val deterministicRepository = TaskRepository(
            database.taskDao(),
            database.executionLogDao(),
            scheduler,
            now = { now },
            holidayCalendar = MapHolidayCalendar(
                mapOf(
                    LocalDate.of(2026, 7, 19) to HolidayDayType.WEEKEND,
                    LocalDate.of(2026, 7, 20) to HolidayDayType.WORKDAY,
                ),
            ),
        )

        val id = deterministicRepository.save(
            TaskEntity(
                packageName = "com.tencent.mobileqq",
                appLabel = "QQ",
                hour = 20,
                minute = 9,
                ruleType = "STATUTORY_WORKDAY",
            ),
        )

        assertEquals(
            ZonedDateTime.parse("2026-07-20T20:09:00+08:00[Asia/Shanghai]").toInstant().toEpochMilli(),
            database.taskDao().getById(id)?.nextTriggerAt,
        )
    }

    @Test
    fun `disable persists state and cancels scheduling`() {
        val id = repository.save(
            TaskEntity(packageName = "com.tencent.wework", appLabel = "企业微信", hour = 18, minute = 0, ruleType = "WEEKDAY"),
        )

        repository.setEnabled(id, false)

        assertEquals(false, database.taskDao().getById(id)?.enabled)
        assertEquals(listOf(id), scheduler.cancelledIds)
    }

    @Test
    fun `enabling a paused task recalculates its next trigger and schedules it`() {
        val now = ZonedDateTime.parse("2026-07-17T17:30:00+08:00[Asia/Shanghai]")
        val deterministicRepository = TaskRepository(
            database.taskDao(),
            database.executionLogDao(),
            scheduler,
            now = { now },
        )
        val id = database.taskDao().insert(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "DAILY",
                enabled = false,
                nextTriggerAt = null,
            ),
        )

        deterministicRepository.setEnabled(id, true)

        assertEquals(true, database.taskDao().getById(id)?.enabled)
        assertEquals(now.plusMinutes(30).toInstant().toEpochMilli(), database.taskDao().getById(id)?.nextTriggerAt)
        assertEquals(listOf(id), scheduler.scheduledIds)
    }

    @Test
    fun `skipping the current occurrence temporarily disables a daily task until its scheduled time`() {
        val now = ZonedDateTime.parse("2026-07-17T17:30:00+08:00[Asia/Shanghai]")
        val deterministicRepository = TaskRepository(
            database.taskDao(),
            database.executionLogDao(),
            scheduler,
            now = { now },
        )
        val id = deterministicRepository.save(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "DAILY",
            ),
        )

        deterministicRepository.skipCurrentOccurrence(id)

        val task = database.taskDao().getById(id)
        assertEquals(false, task?.enabled)
        assertEquals(true, task?.resumeAfterSkippedOccurrence)
        assertEquals(
            ZonedDateTime.parse("2026-07-17T18:00:00+08:00[Asia/Shanghai]").toInstant().toEpochMilli(),
            task?.nextTriggerAt,
        )
        assertEquals(listOf(id, id), scheduler.scheduledIds)
        assertEquals("OCCURRENCE_SKIPPED", database.executionLogDao().getForTask(id).single().eventType)
    }

    @Test
    fun `delete cancels scheduling removes task and writes a log`() {
        val id = repository.save(
            TaskEntity(packageName = "com.tencent.wework", appLabel = "企业微信", hour = 18, minute = 0, ruleType = "WEEKDAY"),
        )

        repository.delete(id)

        assertEquals(null, database.taskDao().getById(id))
        assertEquals(listOf(id), scheduler.cancelledIds)
        assertEquals("TASK_DELETED", database.executionLogDao().getForTask(id).single().eventType)
    }

    @Test
    fun `update cancels old alarm recalculates schedule and writes a log`() {
        val now = ZonedDateTime.parse("2026-07-17T17:30:00+08:00[Asia/Shanghai]")
        val deterministicRepository = TaskRepository(
            database.taskDao(),
            database.executionLogDao(),
            scheduler,
            now = { now },
        )
        val id = deterministicRepository.save(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
            ),
        )

        deterministicRepository.update(
            database.taskDao().getById(id)!!.copy(hour = 19, minute = 0),
        )

        assertEquals(listOf(id), scheduler.cancelledIds)
        assertEquals(listOf(id, id), scheduler.scheduledIds)
        assertEquals(19, database.taskDao().getById(id)?.hour)
        assertEquals("TASK_UPDATED", database.executionLogDao().getForTask(id).first().eventType)
    }

    @Test
    fun `copy creates an independently scheduled task with a new ID`() {
        val id = repository.save(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
            ),
        )

        val copiedId = repository.copy(id)

        assert(copiedId != id)
        assertEquals("com.tencent.wework", database.taskDao().getById(copiedId)?.packageName)
        assertEquals(listOf(id, copiedId), scheduler.scheduledIds)
        assertEquals("TASK_COPIED", database.executionLogDao().getForTask(copiedId).single().eventType)
    }

    private class RecordingScheduler : TaskScheduler {
        val scheduledIds = mutableListOf<Long>()
        val cancelledIds = mutableListOf<Long>()

        override fun schedule(taskId: Long) {
            scheduledIds += taskId
        }

        override fun cancel(taskId: Long) {
            cancelledIds += taskId
        }
    }

    private class MapHolidayCalendar(
        private val types: Map<LocalDate, HolidayDayType>,
    ) : HolidayCalendar {
        override fun dayInfo(date: LocalDate): HolidayDayInfo = HolidayDayInfo(
            type = requireNotNull(types[date]) { "Missing holiday data for $date" },
            source = HolidayDataSource.NETWORK,
        )
    }
}
