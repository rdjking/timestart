package com.example.timestart.ui.create

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.data.local.TimeStartDatabase
import com.example.timestart.data.repository.TaskRepository
import com.example.timestart.data.repository.TaskScheduler
import com.example.timestart.platform.apps.LaunchableApp
import com.example.timestart.platform.apps.LaunchableAppsProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CreateTaskViewModelTest {
    private lateinit var database: TimeStartDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TimeStartDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = TaskRepository(database.taskDao(), database.executionLogDao(), NoOpScheduler)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `update saves edited values using the existing task ID`() = runBlocking {
        val taskId = repository.save(
            TaskEntity(
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 0,
                ruleType = "WEEKDAY",
            ),
        )
        val viewModel = CreateTaskViewModel(repository, EmptyAppsProvider)
        var saved = false

        viewModel.update(
            taskId = taskId,
            app = LaunchableApp("com.tencent.wework", "企业微信"),
            hour = 19,
            minute = 30,
            ruleOption = ScheduleRuleOption.DAILY,
            date = LocalDate.of(2026, 7, 20),
        ) {
            saved = true
        }
        withTimeout(2_000) {
            while (!saved) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                delay(10)
            }
        }
        val updated = requireNotNull(database.taskDao().getById(taskId))
        assertEquals(19, updated.hour)
        assertEquals(30, updated.minute)
        assertEquals("DAILY", updated.ruleType)
    }

    private object EmptyAppsProvider : LaunchableAppsProvider {
        override fun getApps(): List<LaunchableApp> = emptyList()
    }

    private object NoOpScheduler : TaskScheduler {
        override fun schedule(taskId: Long) = Unit
        override fun cancel(taskId: Long) = Unit
    }
}
