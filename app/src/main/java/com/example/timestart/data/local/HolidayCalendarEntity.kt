package com.example.timestart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holiday_calendar")
data class HolidayCalendarEntity(
    @PrimaryKey val calendarDate: String,
    val dayType: Int,
    val updatedAt: Long,
)
