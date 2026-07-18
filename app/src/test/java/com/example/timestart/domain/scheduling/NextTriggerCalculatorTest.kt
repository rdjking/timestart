package com.example.timestart.domain.scheduling

import com.example.timestart.domain.model.ScheduleRule
import com.example.timestart.domain.model.ScheduleTask
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class NextTriggerCalculatorTest {

    @Test
    fun `returns today's time when a weekday schedule has not happened yet`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(
            hour = 18,
            minute = 0,
            rule = ScheduleRule.Weekday,
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 15),
            LocalTime.of(17, 59),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 7, 15), LocalTime.of(18, 0), zone),
            result,
        )
    }

    @Test
    fun `moves to tomorrow when a weekday schedule has already happened`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 18, minute = 0, rule = ScheduleRule.Weekday)
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 15),
            LocalTime.of(18, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 7, 16), LocalTime.of(18, 0), zone),
            result,
        )
    }

    @Test
    fun `skips the weekend for a weekday schedule`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 18, minute = 0, rule = ScheduleRule.Weekday)
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 17),
            LocalTime.of(18, 1),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 7, 20), LocalTime.of(18, 0), zone),
            result,
        )
    }

    @Test
    fun `returns a weekend date for a daily schedule`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 8, minute = 30, rule = ScheduleRule.Daily)
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 18),
            LocalTime.of(8, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 7, 18), LocalTime.of(8, 30), zone),
            result,
        )
    }

    @Test
    fun `moves from Friday night to Saturday for a weekend schedule`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 10, minute = 0, rule = ScheduleRule.Weekend)
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 17),
            LocalTime.of(22, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 7, 18), LocalTime.of(10, 0), zone),
            result,
        )
    }

    @Test
    fun `finds the next selected day for a weekly schedule`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(
            hour = 9,
            minute = 0,
            rule = ScheduleRule.Weekly(setOf(DayOfWeek.WEDNESDAY)),
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 13),
            LocalTime.of(12, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 7, 15), LocalTime.of(9, 0), zone),
            result,
        )
    }

    @Test
    fun `moves to next month when this month's monthly schedule has passed`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 9, minute = 0, rule = ScheduleRule.Monthly(15))
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 16),
            LocalTime.of(8, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), zone),
            result,
        )
    }

    @Test
    fun `returns the scheduled instant for a future one time task`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val scheduledAt = ZonedDateTime.of(
            LocalDate.of(2026, 7, 20),
            LocalTime.of(9, 0),
            zone,
        )
        val task = ScheduleTask(hour = 9, minute = 0, rule = ScheduleRule.Once(scheduledAt))

        val result = NextTriggerCalculator.nextOrNull(
            task,
            ZonedDateTime.of(LocalDate.of(2026, 7, 19), LocalTime.NOON, zone),
        )

        assertEquals(scheduledAt, result)
    }

    @Test
    fun `returns null for an expired one time task`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(
            hour = 9,
            minute = 0,
            rule = ScheduleRule.Once(
                ZonedDateTime.of(LocalDate.of(2026, 7, 20), LocalTime.of(9, 0), zone),
            ),
        )

        val result = NextTriggerCalculator.nextOrNull(
            task,
            ZonedDateTime.of(LocalDate.of(2026, 7, 20), LocalTime.of(9, 0), zone),
        )

        assertEquals(null, result)
    }

    @Test
    fun `moves to next year when this year's annual schedule has passed`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 9, minute = 0, rule = ScheduleRule.Yearly(12, 25))
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 12, 26),
            LocalTime.of(8, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2027, 12, 25), LocalTime.of(9, 0), zone),
            result,
        )
    }

    @Test
    fun `skips months without the requested monthly day`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 9, minute = 0, rule = ScheduleRule.Monthly(31))
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 1),
            LocalTime.of(8, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 5, 31), LocalTime.of(9, 0), zone),
            result,
        )
    }

    @Test
    fun `skips non leap years for a February twenty ninth annual schedule`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val task = ScheduleTask(hour = 9, minute = 0, rule = ScheduleRule.Yearly(2, 29))
        val now = ZonedDateTime.of(
            LocalDate.of(2027, 1, 1),
            LocalTime.of(8, 0),
            zone,
        )

        val result = NextTriggerCalculator.next(task, now)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2028, 2, 29), LocalTime.of(9, 0), zone),
            result,
        )
    }
}
