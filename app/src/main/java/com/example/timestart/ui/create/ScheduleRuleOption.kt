package com.example.timestart.ui.create

import com.example.timestart.domain.model.ScheduleRule
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZonedDateTime

data class RuleSelections(
    val weeklyDays: Set<java.time.DayOfWeek> = emptySet(),
    val monthlyDays: Set<Int> = emptySet(),
    val yearlyDates: Set<MonthDay> = emptySet(),
    val onceDates: Set<LocalDate> = emptySet(),
)

enum class ScheduleRuleOption(val label: String) {
    DAILY("每天"),
    WEEKDAY("工作日"),
    WEEKEND("周末"),
    STATUTORY_WORKDAY("法定工作日"),
    HOLIDAY_OR_WEEKEND("节假日及周末"),
    WEEKLY("每周"),
    MONTHLY("每月"),
    ONCE("单次"),
    YEARLY("每年");

    fun toScheduleRule(
        date: LocalDate,
        hour: Int,
        minute: Int,
        zone: ZoneId,
        selections: RuleSelections = RuleSelections(),
    ): ScheduleRule = when (this) {
        DAILY -> ScheduleRule.Daily
        WEEKDAY -> ScheduleRule.Weekday
        WEEKEND -> ScheduleRule.Weekend
        STATUTORY_WORKDAY -> ScheduleRule.StatutoryWorkday
        HOLIDAY_OR_WEEKEND -> ScheduleRule.HolidayOrWeekend
        WEEKLY -> ScheduleRule.Weekly(selections.weeklyDays.ifEmpty { setOf(date.dayOfWeek) })
        MONTHLY -> ScheduleRule.Monthly(selections.monthlyDays.ifEmpty { setOf(date.dayOfMonth) })
        ONCE -> ScheduleRule.Once(selections.onceDates.ifEmpty { setOf(date) }
            .map { ZonedDateTime.of(it, java.time.LocalTime.of(hour, minute), zone) }.toSet())
        YEARLY -> ScheduleRule.Yearly(selections.yearlyDates.ifEmpty { setOf(MonthDay.of(date.monthValue, date.dayOfMonth)) })
    }
}
