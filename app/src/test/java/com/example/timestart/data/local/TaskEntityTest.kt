package com.example.timestart.data.local

import com.example.timestart.domain.model.ScheduleRule
import com.example.timestart.domain.model.ScheduleTask
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEntityTest {

    @Test
    fun `maps a daily domain task to a persistent entity`() {
        val task = ScheduleTask(
            packageName = "com.tencent.wework",
            appLabel = "企业微信",
            hour = 18,
            minute = 0,
            rule = ScheduleRule.Daily,
        )

        val entity = TaskEntity.from(task)

        assertEquals("com.tencent.wework", entity.packageName)
        assertEquals("企业微信", entity.appLabel)
        assertEquals(18, entity.hour)
        assertEquals(0, entity.minute)
        assertEquals("DAILY", entity.ruleType)
        assertTrue(entity.enabled)
    }

    @Test
    fun `round trips a weekly rule with all selected weekdays`() {
        val task = ScheduleTask(
            packageName = "com.tencent.wework",
            appLabel = "企业微信",
            hour = 18,
            minute = 0,
            rule = ScheduleRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
        )

        val restored = TaskEntity.from(task).toScheduleTask()

        assertEquals(task, restored)
    }
}
