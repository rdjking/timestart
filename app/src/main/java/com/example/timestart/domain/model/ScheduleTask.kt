package com.example.timestart.domain.model

data class ScheduleTask(
    val packageName: String = "",
    val appLabel: String = "",
    val hour: Int,
    val minute: Int,
    val rule: ScheduleRule,
)
