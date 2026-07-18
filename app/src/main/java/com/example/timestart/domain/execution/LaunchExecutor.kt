package com.example.timestart.domain.execution

/** Requests that Android opens a task's target package. */
interface LaunchExecutor {
    fun requestLaunch(packageName: String): LaunchRequestResult
}

sealed interface LaunchRequestResult {
    val logCode: String
    val logMessage: String

    data object Requested : LaunchRequestResult {
        override val logCode = "REQUESTED"
        override val logMessage = "Android was asked to launch the target app"
    }

    data object TargetUnavailable : LaunchRequestResult {
        override val logCode = "TARGET_UNAVAILABLE"
        override val logMessage = "No launchable activity was found for the target app"
    }

    data object OverlayPermissionRequired : LaunchRequestResult {
        override val logCode = "OVERLAY_PERMISSION_REQUIRED"
        override val logMessage = "Enable display-over-other-apps permission for reliable automatic launches"
    }

    data class Failed(val reason: String) : LaunchRequestResult {
        override val logCode = "FAILED"
        override val logMessage = reason
    }
}
