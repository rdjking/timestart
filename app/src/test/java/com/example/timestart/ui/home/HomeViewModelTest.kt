package com.example.timestart.ui.home

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.data.local.TimeStartDatabase
import com.example.timestart.data.repository.TaskRepository
import com.example.timestart.data.repository.TaskScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    private lateinit var database: TimeStartDatabase
    private lateinit var scheduler: RecordingScheduler
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TimeStartDatabase::class.java,
        ).allowMainThreadQueries().build()
        scheduler = RecordingScheduler()
        repository = TaskRepository(database.taskDao(), database.executionLogDao(), scheduler)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `delete task removes it and cancels its scheduled alarm`() = runBlocking {
        val taskId = repository.save(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
            ),
        )
        val viewModel = HomeViewModel(repository)

        viewModel.deleteTask(taskId)

        withTimeout(2_000) {
            while (database.taskDao().getById(taskId) != null) {
                delay(10)
            }
        }
        assertNull(database.taskDao().getById(taskId))
        assertEquals(listOf(taskId), scheduler.cancelledIds)
    }

    private class RecordingScheduler : TaskScheduler {
        val cancelledIds = mutableListOf<Long>()

        override fun schedule(taskId: Long) = Unit

        override fun cancel(taskId: Long) {
            cancelledIds += taskId
        }
    }
}
