package com.example.timestart.ui.create

import com.example.timestart.domain.model.ScheduleRule
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleRuleOptionTest {
    @Test
    fun `weekly option uses the selected date weekday`() {
        val rule = ScheduleRuleOption.WEEKLY.toScheduleRule(
            date = LocalDate.of(2026, 7, 20),
            hour = 18,
            minute = 0,
            zone = ZoneId.of("Asia/Shanghai"),
        )

        assertEquals(ScheduleRule.Weekly(setOf(java.time.DayOfWeek.MONDAY)), rule)
    }

    @Test
    fun `statutory workday option maps to its holiday aware rule`() {
        val rule = ScheduleRuleOption.STATUTORY_WORKDAY.toScheduleRule(
            date = LocalDate.of(2026, 7, 20),
            hour = 18,
            minute = 0,
            zone = ZoneId.of("Asia/Shanghai"),
        )

        assertEquals(ScheduleRule.StatutoryWorkday, rule)
    }
}
