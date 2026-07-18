package com.example.timestart.platform.launch

import androidx.test.core.app.ApplicationProvider
import com.example.timestart.domain.execution.LaunchRequestResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidLaunchExecutorTest {
    @Test
    fun `reports target unavailable when no launcher activity exists`() {
        val result = AndroidLaunchExecutor(ApplicationProvider.getApplicationContext())
            .requestLaunch("com.example.timestart.not.installed")

        assertEquals(LaunchRequestResult.TargetUnavailable, result)
    }
}
