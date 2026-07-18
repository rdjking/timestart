package com.example.timestart.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenStateTest {
    @Test
    fun `shows notification permission card only when runtime permission is required and missing`() {
        assertTrue(
            HomeScreenState.from(
                requiresRuntimePermission = true,
                hasNotificationPermission = false,
            ).showNotificationPermissionCard,
        )
        assertFalse(
            HomeScreenState.from(
                requiresRuntimePermission = true,
                hasNotificationPermission = true,
            ).showNotificationPermissionCard,
        )
        assertFalse(
            HomeScreenState.from(
                requiresRuntimePermission = false,
                hasNotificationPermission = false,
            ).showNotificationPermissionCard,
        )
    }

    @Test
    fun `shows exact alarm card only when Android requires permission and access is missing`() {
        assertTrue(
            HomeScreenState.from(
                requiresRuntimePermission = false,
                hasNotificationPermission = true,
                requiresExactAlarmPermission = true,
                hasExactAlarmPermission = false,
            ).showExactAlarmPermissionCard,
        )
        assertFalse(
            HomeScreenState.from(
                requiresRuntimePermission = false,
                hasNotificationPermission = true,
                requiresExactAlarmPermission = true,
                hasExactAlarmPermission = true,
            ).showExactAlarmPermissionCard,
        )
    }
}
