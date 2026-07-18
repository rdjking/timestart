package com.example.timestart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, ExecutionLogEntity::class], version = 3, exportSchema = true)
abstract class TimeStartDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun executionLogDao(): ExecutionLogDao
}
