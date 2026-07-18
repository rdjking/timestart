package com.example.timestart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {
    @Insert
    fun insert(log: ExecutionLogEntity): Long

    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY occurredAt DESC")
    fun getForTask(taskId: Long): List<ExecutionLogEntity>

    @Query("SELECT * FROM execution_logs ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY occurredAt DESC, id DESC")
    fun observeForTask(taskId: Long): Flow<List<ExecutionLogEntity>>

    @Query("DELETE FROM execution_logs WHERE occurredAt < :cutoffMillis")
    fun deleteOlderThan(cutoffMillis: Long): Int
}
