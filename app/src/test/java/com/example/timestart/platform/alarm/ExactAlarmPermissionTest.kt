package com.example.timestart.platform.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactAlarmPermissionTest {
    @Test
    fun `requires special access from Android 12`() {
        assertFalse(ExactAlarmPermission.requiresSpecialAccess(30))
        assertTrue(ExactAlarmPermission.requiresSpecialAccess(31))
    }
}
