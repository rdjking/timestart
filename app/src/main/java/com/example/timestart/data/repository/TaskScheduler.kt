package com.example.timestart.data.repository

interface TaskScheduler {
    fun schedule(taskId: Long)
    fun cancel(taskId: Long)
}
