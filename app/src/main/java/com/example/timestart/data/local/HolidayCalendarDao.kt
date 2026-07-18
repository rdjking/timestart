package com.example.timestart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HolidayCalendarDao {
    @Query("SELECT * FROM holiday_calendar WHERE calendarDate = :date LIMIT 1")
    fun get(date: String): HolidayCalendarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: HolidayCalendarEntity)
}
