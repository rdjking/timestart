package com.example.timestart.ui.logs

import com.example.timestart.data.local.ExecutionLogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExecutionLogItem(
    val id: Long,
    val taskId: Long?,
    val timeLabel: String,
    val eventType: String,
    val resultCode: String,
    val message: String,
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(log: ExecutionLogEntity): ExecutionLogItem = ExecutionLogItem(
            id = log.id,
            taskId = log.taskId,
            timeLabel = formatter.format(Instant.ofEpochMilli(log.occurredAt).atZone(ZoneId.systemDefault())),
            eventType = log.eventType,
            resultCode = log.resultCode,
            message = log.message,
        )
    }

    fun copyText(): String = "$timeLabel\n$eventType · $resultCode\n$message"
}
