package com.example.timestart.ui.create

import com.example.timestart.data.local.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

internal fun TaskEntity.toScheduleRuleOption(): ScheduleRuleOption = when (ruleType) {
    "DAILY" -> ScheduleRuleOption.DAILY
    "WEEKDAY" -> ScheduleRuleOption.WEEKDAY
    "WEEKEND" -> ScheduleRuleOption.WEEKEND
    "WEEKLY" -> ScheduleRuleOption.WEEKLY
    "MONTHLY" -> ScheduleRuleOption.MONTHLY
    "ONCE" -> ScheduleRuleOption.ONCE
    "YEARLY" -> ScheduleRuleOption.YEARLY
    else -> ScheduleRuleOption.WEEKDAY
}

internal fun TaskEntity.initialDate(): LocalDate {
    val today = LocalDate.now()
    return when (ruleType) {
        "WEEKLY" -> ruleValue
            ?.split(',')
            ?.firstOrNull()
            ?.toIntOrNull()
            ?.let(DayOfWeek::of)
            ?.let { today.with(TemporalAdjusters.nextOrSame(it)) }
            ?: today

        "MONTHLY" -> ruleValue
            ?.toIntOrNull()
            ?.takeIf { it in 1..today.lengthOfMonth() }
            ?.let(today::withDayOfMonth)
            ?: today

        "ONCE" -> ruleValue?.let(ZonedDateTime::parse)?.toLocalDate() ?: today
        "YEARLY" -> ruleValue
            ?.split('-')
            ?.takeIf { it.size == 2 }
            ?.let { parts ->
                val month = parts[0].toIntOrNull() ?: return@let today
                val day = parts[1].toIntOrNull() ?: return@let today
                val year = if (month == 2 && day == 29) 2028 else today.year
                runCatching { LocalDate.of(year, month, day) }.getOrDefault(today)
            }
            ?: today

        else -> today
    }
}
