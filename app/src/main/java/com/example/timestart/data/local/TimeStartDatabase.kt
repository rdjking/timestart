package com.example.timestart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, ExecutionLogEntity::class, HolidayCalendarEntity::class], version = 5, exportSchema = true)
abstract class TimeStartDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun holidayCalendarDao(): HolidayCalendarDao
}
