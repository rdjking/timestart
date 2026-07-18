package com.example.timestart.platform.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionTest {
    @Test
    fun `requires runtime consent only from Android 13`() {
        assertFalse(NotificationPermission.isRuntimePermissionRequired(32))
        assertTrue(NotificationPermission.isRuntimePermissionRequired(33))
    }
}
