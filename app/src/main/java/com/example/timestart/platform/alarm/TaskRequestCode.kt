package com.example.timestart.platform.alarm

/** Maps a persisted task ID to the stable request code used by its PendingIntent. */
object TaskRequestCode {
    fun forTask(taskId: Long): Int {
        require(taskId in 0..Int.MAX_VALUE) {
            "Task ID must fit in an Android PendingIntent request code: $taskId"
        }
        return taskId.toInt()
    }
}
