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
class ExecutionLogDaoTest {
    private lateinit var database: TimeStartDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TimeStartDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `returns task logs newest first`() {
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 7, occurredAt = 100, eventType = "ALARM_TRIGGERED", resultCode = "OK", message = "first"))
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 7, occurredAt = 200, eventType = "LAUNCH_REQUESTED", resultCode = "REQUEST_SENT", message = "second"))

        val logs = database.executionLogDao().getForTask(7)

        assertEquals(listOf("second", "first"), logs.map(ExecutionLogEntity::message))
    }

    @Test
    fun `observes all logs newest first`() = runBlocking {
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 7, occurredAt = 100, eventType = "ALARM_TRIGGERED", resultCode = "OK", message = "first"))
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 8, occurredAt = 200, eventType = "LAUNCH_REQUESTED", resultCode = "REQUEST_SENT", message = "second"))

        val logs = database.executionLogDao().observeAll().first()

        assertEquals(listOf("second", "first"), logs.map(ExecutionLogEntity::message))
    }

    @Test
    fun `deletes only logs older than the retention cutoff`() {
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 7, occurredAt = 99, eventType = "OLD", resultCode = "OK", message = "old"))
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 7, occurredAt = 100, eventType = "BOUNDARY", resultCode = "OK", message = "boundary"))
        database.executionLogDao().insert(ExecutionLogEntity(taskId = 7, occurredAt = 101, eventType = "NEW", resultCode = "OK", message = "new"))

        val deleted = database.executionLogDao().deleteOlderThan(100)

        assertEquals(1, deleted)
        assertEquals(listOf("new", "boundary"), database.executionLogDao().getForTask(7).map(ExecutionLogEntity::message))
    }
}
