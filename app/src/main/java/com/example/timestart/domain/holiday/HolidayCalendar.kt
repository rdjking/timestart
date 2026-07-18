package com.example.timestart.domain.holiday

import java.time.DayOfWeek
import java.time.LocalDate

enum class HolidayDayType(val apiValue: Int) {
    WORKDAY(0),
    WEEKEND(1),
    HOLIDAY(2),
    MAKEUP_WORKDAY(3),
    ;

    val isStatutoryWorkday: Boolean get() = this == WORKDAY || this == MAKEUP_WORKDAY
    val isHolidayOrWeekend: Boolean get() = this == WEEKEND || this == HOLIDAY

    companion object {
        fun fromApiValue(value: Int): HolidayDayType? = entries.firstOrNull { it.apiValue == value }
    }
}

enum class HolidayDataSource { NETWORK, CACHE, FALLBACK }

data class HolidayDayInfo(
    val type: HolidayDayType,
    val source: HolidayDataSource,
)

interface HolidayCalendar {
    fun dayInfo(date: LocalDate): HolidayDayInfo
}

object LocalWeekPatternHolidayCalendar : HolidayCalendar {
    override fun dayInfo(date: LocalDate): HolidayDayInfo = HolidayDayInfo(
        type = if (date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            HolidayDayType.WEEKEND
        } else {
            HolidayDayType.WORKDAY
        },
        source = HolidayDataSource.FALLBACK,
    )
}
