package com.example.timestart.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TimeStartMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN ruleValue TEXT")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE schedule_tasks ADD COLUMN resumeAfterSkippedOccurrence INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS holiday_calendar (calendarDate TEXT NOT NULL, dayType INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(calendarDate))",
            )
        }
    }
}
