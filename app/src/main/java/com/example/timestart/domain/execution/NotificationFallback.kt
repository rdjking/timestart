package com.example.timestart.domain.execution

/** Gives the user a tap-to-open route when Android cannot guarantee a background Activity launch. */
interface NotificationFallback {
    fun post(taskId: Long, appLabel: String, packageName: String): NotificationFallbackResult
}

sealed interface NotificationFallbackResult {
    val logCode: String
    val logMessage: String

    data object Displayed : NotificationFallbackResult {
        override val logCode = "DISPLAYED"
        override val logMessage = "A tap-to-open notification was displayed"
    }

    data object PermissionDenied : NotificationFallbackResult {
        override val logCode = "PERMISSION_DENIED"
        override val logMessage = "Notification permission is not granted or notifications are disabled"
    }

    data object TargetUnavailable : NotificationFallbackResult {
        override val logCode = "TARGET_UNAVAILABLE"
        override val logMessage = "No launchable activity was found for the target app"
    }

    data class Failed(val reason: String) : NotificationFallbackResult {
        override val logCode = "FAILED"
        override val logMessage = reason
    }
}
