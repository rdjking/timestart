package com.example.timestart.platform.receiver

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleReceiverTest {
    @Test
    fun `accepts every system event that requires task recovery`() {
        assertTrue(RescheduleReceiver.isRecoveryAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(RescheduleReceiver.isRecoveryAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertTrue(RescheduleReceiver.isRecoveryAction(Intent.ACTION_TIME_CHANGED))
        assertTrue(RescheduleReceiver.isRecoveryAction(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun `ignores unrelated broadcasts`() {
        assertFalse(RescheduleReceiver.isRecoveryAction(Intent.ACTION_SCREEN_ON))
    }
}
