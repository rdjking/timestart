package com.example.timestart

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(application = TimeStartApplication::class)
class TimeStartApplicationTest {
    @Test
    fun `provides a shared trigger coordinator for broadcast receivers`() {
        val application = ApplicationProvider.getApplicationContext<TimeStartApplication>()

        assertNotNull(application.taskTriggerCoordinator)
    }
}
