package com.example.timestart.ui.home

import com.example.timestart.data.local.TaskEntity
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HomeTaskItem(
    val id: Long,
    val packageName: String,
    val appLabel: String,
    val timeLabel: String,
    val ruleLabel: String,
    val enabled: Boolean,
    val nextTriggerLabel: String,
    val nextTriggerDetailLabel: String,
    val currentOccurrenceDateLabel: String,
) {
    companion object {
        fun from(task: TaskEntity): HomeTaskItem = HomeTaskItem(
            id = task.id,
            packageName = task.packageName,
            appLabel = task.appLabel,
            timeLabel = String.format(Locale.ROOT, "%02d:%02d", task.hour, task.minute),
            ruleLabel = task.ruleType.toRuleLabel(),
            enabled = task.enabled,
            nextTriggerLabel = task.nextTriggerLabel(),
            nextTriggerDetailLabel = task.nextTriggerDetailLabel(),
            currentOccurrenceDateLabel = task.currentOccurrenceDateLabel(),
        )
    }
}

private fun TaskEntity.nextTriggerLabel(): String {
    if (!enabled) return "已暂停"
    val triggerAt = nextTriggerAt ?: return "等待计算下次执行时间"
    val dateTime = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
    val date = dateTime.toLocalDate()
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    return when (date) {
        LocalDate.now() -> "下次：今天 $time"
        LocalDate.now().plusDays(1) -> "下次：明天 $time"
        else -> "下次：${dateTime.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm", Locale.getDefault()))}"
    }
}

private fun TaskEntity.nextTriggerDetailLabel(): String {
    if (!enabled) return "已暂停"
    val triggerAt = nextTriggerAt ?: return "等待计算"
    return Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm", Locale.getDefault()))
}

private fun TaskEntity.currentOccurrenceDateLabel(): String {
    if (!enabled) return "当前"
    val triggerAt = nextTriggerAt ?: return "当前"
    val date = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val weekday = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
    return "${date.monthValue}月${date.dayOfMonth}日（$weekday）"
}

private fun String.toRuleLabel(): String = when (this) {
    "DAILY" -> "每天"
    "WEEKDAY" -> "工作日"
    "WEEKEND" -> "周末"
    "WEEKLY" -> "每周"
    "MONTHLY" -> "每月"
    "ONCE" -> "单次"
    "YEARLY" -> "每年"
    else -> "自定义规则"
}
