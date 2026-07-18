package com.example.timestart.platform.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TaskRequestCodeTest {

    @Test
    fun `returns a stable request code for the same task`() {
        assertEquals(TaskRequestCode.forTask(42), TaskRequestCode.forTask(42))
    }

    @Test
    fun `returns different request codes for different tasks`() {
        assertNotEquals(TaskRequestCode.forTask(42), TaskRequestCode.forTask(43))
    }
}
