package com.example.timestart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val occurredAt: Long,
    val eventType: String,
    val resultCode: String,
    val message: String,
)
