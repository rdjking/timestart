package com.example.timestart.ui.home

/** UI-only state for the first launchable home screen. */
data class HomeScreenState(
    val showNotificationPermissionCard: Boolean,
    val showExactAlarmPermissionCard: Boolean,
    val showOverlayPermissionCard: Boolean = false,
) {
    companion object {
        fun from(
            requiresRuntimePermission: Boolean,
            hasNotificationPermission: Boolean,
            requiresExactAlarmPermission: Boolean = false,
            hasExactAlarmPermission: Boolean = true,
            hasOverlayPermission: Boolean = true,
        ): HomeScreenState = HomeScreenState(
            showNotificationPermissionCard = requiresRuntimePermission && !hasNotificationPermission,
            showExactAlarmPermissionCard = requiresExactAlarmPermission && !hasExactAlarmPermission,
            showOverlayPermissionCard = !hasOverlayPermission,
        )
    }
}
