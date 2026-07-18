package com.example.timestart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.timestart.domain.model.ScheduleRule
import com.example.timestart.domain.model.ScheduleTask
import java.time.DayOfWeek
import java.time.MonthDay
import java.time.ZonedDateTime

@Entity(tableName = "schedule_tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val hour: Int,
    val minute: Int,
    val ruleType: String,
    val ruleValue: String? = null,
    val enabled: Boolean = true,
    val nextTriggerAt: Long? = null,
    val resumeAfterSkippedOccurrence: Boolean = false,
) {
    companion object {
        fun from(task: ScheduleTask): TaskEntity = TaskEntity(
            packageName = task.packageName,
            appLabel = task.appLabel,
            hour = task.hour,
            minute = task.minute,
            ruleType = task.rule.persistenceName(),
            ruleValue = task.rule.persistenceValue(),
        )
    }

    fun toScheduleTask(): ScheduleTask = ScheduleTask(
        packageName = packageName,
        appLabel = appLabel,
        hour = hour,
        minute = minute,
        rule = ruleType.toScheduleRule(ruleValue),
    )
}

private fun ScheduleRule.persistenceName(): String = when (this) {
    ScheduleRule.Daily -> "DAILY"
    ScheduleRule.Weekday -> "WEEKDAY"
    ScheduleRule.Weekend -> "WEEKEND"
    is ScheduleRule.Weekly -> "WEEKLY"
    is ScheduleRule.Monthly -> "MONTHLY"
    is ScheduleRule.Once -> "ONCE"
    is ScheduleRule.Yearly -> "YEARLY"
}

private fun ScheduleRule.persistenceValue(): String? = when (this) {
    ScheduleRule.Daily,
    ScheduleRule.Weekday,
    ScheduleRule.Weekend,
    -> null

    is ScheduleRule.Weekly -> days
        .sortedBy(DayOfWeek::getValue)
        .joinToString(",") { it.value.toString() }

    is ScheduleRule.Monthly -> daysOfMonth.sorted().joinToString(",")
    is ScheduleRule.Once -> atTimes.sorted().joinToString(",")
    is ScheduleRule.Yearly -> monthDays
        .sortedWith(compareBy(MonthDay::getMonthValue, MonthDay::getDayOfMonth))
        .joinToString(",") { "${it.monthValue}-${it.dayOfMonth}" }
}

private fun String.toScheduleRule(value: String?): ScheduleRule = when (this) {
    "DAILY" -> ScheduleRule.Daily
    "WEEKDAY" -> ScheduleRule.Weekday
    "WEEKEND" -> ScheduleRule.Weekend
    "WEEKLY" -> ScheduleRule.Weekly(
        requireNotNull(value) { "WEEKLY rule requires selected days" }
            .split(',')
            .map(String::toInt)
            .map(DayOfWeek::of)
            .toSet(),
    )

    "MONTHLY" -> ScheduleRule.Monthly(
        requireNotNull(value) { "MONTHLY rule requires a day of month" }
            .split(',').map(String::toInt).toSet(),
    )

    "ONCE" -> ScheduleRule.Once(
        requireNotNull(value) { "ONCE rule requires a trigger time" }
            .split(',').map(ZonedDateTime::parse).toSet(),
    )

    "YEARLY" -> {
        ScheduleRule.Yearly(requireNotNull(value) { "YEARLY rule requires month and day" }
            .split(',').map { item ->
                val parts = item.split('-')
                require(parts.size == 2) { "YEARLY rule has an invalid value: $value" }
                MonthDay.of(parts[0].toInt(), parts[1].toInt())
            }.toSet())
    }

    else -> error("Unsupported persisted rule type: $this")
}
