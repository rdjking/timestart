package com.example.timestart.domain.scheduling

import com.example.timestart.domain.model.ScheduleRule
import com.example.timestart.domain.model.ScheduleTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.LocalTime
import java.time.ZonedDateTime

object NextTriggerCalculator {
    fun next(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        return requireNotNull(nextOrNull(task, now)) { "Task has no future trigger" }
    }

    fun nextOrNull(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime? {
        require(task.hour in 0..23)
        require(task.minute in 0..59)

        return when (task.rule) {
            ScheduleRule.Daily -> nextDaily(task, now)
            ScheduleRule.Weekday -> nextWeekday(task, now)
            ScheduleRule.Weekend -> nextWeekend(task, now)
            ScheduleRule.StatutoryWorkday,
            ScheduleRule.HolidayOrWeekend,
            -> nextDaily(task, now)
            is ScheduleRule.Weekly -> nextWeekly(task, now)
            is ScheduleRule.Monthly -> nextMonthly(task, now)
            is ScheduleRule.Once -> nextOnce(task, now)
            is ScheduleRule.Yearly -> nextYearly(task, now)
        }
    }

    private fun nextDaily(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        val scheduledTime = LocalTime.of(task.hour, task.minute)
        val date = if (now.toLocalTime().isBefore(scheduledTime)) {
            now.toLocalDate()
        } else {
            now.toLocalDate().plusDays(1)
        }
        return ZonedDateTime.of(date, scheduledTime, now.zone)
    }

    private fun nextWeekday(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        val scheduledTime = LocalTime.of(task.hour, task.minute)
        var date = if (now.toLocalTime().isBefore(scheduledTime)) {
            now.toLocalDate()
        } else {
            now.toLocalDate().plusDays(1)
        }
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            date = date.plusDays(1)
        }
        return ZonedDateTime.of(date, scheduledTime, now.zone)
    }

    private fun nextWeekend(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        val scheduledTime = LocalTime.of(task.hour, task.minute)
        var date = if (now.toLocalTime().isBefore(scheduledTime)) {
            now.toLocalDate()
        } else {
            now.toLocalDate().plusDays(1)
        }
        while (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
            date = date.plusDays(1)
        }
        return ZonedDateTime.of(date, scheduledTime, now.zone)
    }

    private fun nextWeekly(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        val rule = task.rule as ScheduleRule.Weekly
        val scheduledTime = LocalTime.of(task.hour, task.minute)
        var date = if (now.toLocalTime().isBefore(scheduledTime)) {
            now.toLocalDate()
        } else {
            now.toLocalDate().plusDays(1)
        }
        while (date.dayOfWeek !in rule.days) {
            date = date.plusDays(1)
        }
        return ZonedDateTime.of(date, scheduledTime, now.zone)
    }

    private fun nextMonthly(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        val rule = task.rule as ScheduleRule.Monthly
        val scheduledTime = LocalTime.of(task.hour, task.minute)
        var month = YearMonth.from(now)
        while (true) {
            val candidate = rule.daysOfMonth
                .filter { it <= month.lengthOfMonth() }
                .map { ZonedDateTime.of(month.atDay(it), scheduledTime, now.zone) }
                .filter { it.isAfter(now) }
                .minOrNull()
            if (candidate != null) {
                return candidate
            }
            month = month.plusMonths(1)
        }
    }

    private fun nextOnce(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime? {
        return (task.rule as ScheduleRule.Once).atTimes.filter { it.isAfter(now) }.minOrNull()
    }

    private fun nextYearly(task: ScheduleTask, now: ZonedDateTime): ZonedDateTime {
        val rule = task.rule as ScheduleRule.Yearly
        val scheduledTime = LocalTime.of(task.hour, task.minute)
        var year = now.year
        while (true) {
            val candidate = rule.monthDays.mapNotNull { monthDay ->
                val month = YearMonth.of(year, monthDay.monthValue)
                monthDay.dayOfMonth.takeIf { it <= month.lengthOfMonth() }
                    ?.let { ZonedDateTime.of(month.atDay(it), scheduledTime, now.zone) }
            }.filter { it.isAfter(now) }.minOrNull()
            if (candidate != null) {
                return candidate
            }
            year += 1
        }
    }
}
