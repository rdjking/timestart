package com.example.timestart.ui.home

import com.example.timestart.data.local.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTaskItemTest {
    @Test
    fun `maps a daily task to a readable home card`() {
        val item = HomeTaskItem.from(
            TaskEntity(
                id = 7,
                packageName = "com.tencent.wework",
                appLabel = "企业微信",
                hour = 18,
                minute = 5,
                ruleType = "DAILY",
                enabled = true,
            ),
        )

        assertEquals(7, item.id)
        assertEquals("企业微信", item.appLabel)
        assertEquals("18:05", item.timeLabel)
        assertEquals("每天", item.ruleLabel)
        assertTrue(item.enabled)
    }
}
