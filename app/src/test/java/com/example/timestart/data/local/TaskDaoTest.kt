package com.example.timestart.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {
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
    fun `returns enabled tasks ordered by next trigger time`() {
        database.taskDao().insert(TaskEntity(packageName = "b", appLabel = "B", hour = 9, minute = 0, ruleType = "DAILY", nextTriggerAt = 200))
        database.taskDao().insert(TaskEntity(packageName = "a", appLabel = "A", hour = 8, minute = 0, ruleType = "DAILY", nextTriggerAt = 100))
        database.taskDao().insert(TaskEntity(packageName = "off", appLabel = "Off", hour = 7, minute = 0, ruleType = "DAILY", enabled = false, nextTriggerAt = 50))

        val tasks = database.taskDao().getEnabledOrderedByNextTrigger()

        assertEquals(listOf("a", "b"), tasks.map(TaskEntity::packageName))
    }

    @Test
    fun `reads a task by id and updates its enabled state`() {
        val id = database.taskDao().insert(
            TaskEntity(packageName = "com.tencent.wework", appLabel = "企业微信", hour = 18, minute = 0, ruleType = "WEEKDAY"),
        )

        database.taskDao().setEnabled(id, false)

        val task = requireNotNull(database.taskDao().getById(id))
        assertEquals("com.tencent.wework", task.packageName)
        assertEquals(false, task.enabled)
    }

    @Test
    fun `updates a task next trigger after an alarm fires`() {
        val id = database.taskDao().insert(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
                nextTriggerAt = 1_900_000_000_000L,
            ),
        )

        database.taskDao().updateNextTriggerAt(id, 1_900_086_400_000L)

        assertEquals(1_900_086_400_000L, database.taskDao().getById(id)?.nextTriggerAt)
    }

    @Test
    fun `observes enabled and paused tasks with tasks lacking a next trigger last`() {
        database.taskDao().insert(
            TaskEntity(packageName = "later", appLabel = "Later", hour = 9, minute = 0, ruleType = "DAILY", nextTriggerAt = 200),
        )
        database.taskDao().insert(
            TaskEntity(packageName = "paused", appLabel = "Paused", hour = 8, minute = 0, ruleType = "DAILY", enabled = false, nextTriggerAt = 100),
        )
        database.taskDao().insert(
            TaskEntity(packageName = "none", appLabel = "None", hour = 7, minute = 0, ruleType = "ONCE"),
        )

        val tasks = runBlocking {
            database.taskDao().observeAllOrderedByNextTrigger().first()
        }

        assertEquals(listOf("paused", "later", "none"), tasks.map(TaskEntity::packageName))
    }
}
