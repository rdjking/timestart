package com.example.timestart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: TaskEntity): Long

    @Query("SELECT * FROM schedule_tasks WHERE enabled = 1 ORDER BY nextTriggerAt ASC")
    fun getEnabledOrderedByNextTrigger(): List<TaskEntity>

    @Query(
        "SELECT * FROM schedule_tasks WHERE enabled = 1 OR resumeAfterSkippedOccurrence = 1 " +
            "ORDER BY nextTriggerAt ASC",
    )
    fun getScheduledOrPendingResumeTasks(): List<TaskEntity>

    @Query(
        "SELECT * FROM schedule_tasks " +
            "ORDER BY CASE WHEN nextTriggerAt IS NULL THEN 1 ELSE 0 END, nextTriggerAt ASC, id ASC",
    )
    fun observeAllOrderedByNextTrigger(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM schedule_tasks WHERE id = :id LIMIT 1")
    fun getById(id: Long): TaskEntity?

    @Query("UPDATE schedule_tasks SET enabled = :enabled WHERE id = :id")
    fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE schedule_tasks SET nextTriggerAt = :nextTriggerAt WHERE id = :id")
    fun updateNextTriggerAt(id: Long, nextTriggerAt: Long?)

    @Query("UPDATE schedule_tasks SET resumeAfterSkippedOccurrence = :shouldResume WHERE id = :id")
    fun setResumeAfterSkippedOccurrence(id: Long, shouldResume: Boolean)

    @Query("DELETE FROM schedule_tasks WHERE id = :id")
    fun deleteById(id: Long)
}
