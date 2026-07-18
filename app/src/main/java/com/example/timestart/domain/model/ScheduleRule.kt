package com.example.timestart.domain.model

import java.time.DayOfWeek
import java.time.MonthDay
import java.time.ZonedDateTime

sealed interface ScheduleRule {
    data object Daily : ScheduleRule
    data object Weekday : ScheduleRule
    data object Weekend : ScheduleRule
    data object StatutoryWorkday : ScheduleRule
    data object HolidayOrWeekend : ScheduleRule
    data class Weekly(val days: Set<DayOfWeek>) : ScheduleRule {
        init {
            require(days.isNotEmpty())
        }
    }
    data class Monthly(val daysOfMonth: Set<Int>) : ScheduleRule {
        init {
            require(daysOfMonth.isNotEmpty() && daysOfMonth.all { it in 1..31 })
        }
        constructor(dayOfMonth: Int) : this(setOf(dayOfMonth))
        val dayOfMonth: Int get() = daysOfMonth.min()
    }
    data class Once(val atTimes: Set<ZonedDateTime>) : ScheduleRule {
        init { require(atTimes.isNotEmpty()) }
        constructor(at: ZonedDateTime) : this(setOf(at))
        val at: ZonedDateTime get() = atTimes.min()
    }
    data class Yearly(val monthDays: Set<MonthDay>) : ScheduleRule {
        init {
            require(monthDays.isNotEmpty())
        }
        constructor(month: Int, dayOfMonth: Int) : this(setOf(MonthDay.of(month, dayOfMonth)))
        val month: Int get() = monthDays.minWith(compareBy(MonthDay::getMonthValue, MonthDay::getDayOfMonth)).monthValue
        val dayOfMonth: Int get() = monthDays.minWith(compareBy(MonthDay::getMonthValue, MonthDay::getDayOfMonth)).dayOfMonth
    }
}
