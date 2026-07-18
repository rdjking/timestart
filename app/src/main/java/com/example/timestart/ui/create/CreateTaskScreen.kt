package com.example.timestart.ui.create

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.platform.apps.LaunchableApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
fun CreateTaskScreen(
    apps: List<LaunchableApp>,
    initialTask: TaskEntity? = null,
    onCancel: () -> Unit,
    onSave: (LaunchableApp, Int, Int, LocalDate, ScheduleRuleOption, RuleSelections) -> Unit,
) {
    val context = LocalContext.current
    var selectedApp by remember(initialTask?.id) {
        mutableStateOf(initialTask?.let { LaunchableApp(it.packageName, it.appLabel) })
    }
    var hour by remember(initialTask?.id) { mutableIntStateOf(initialTask?.hour ?: 18) }
    var minute by remember(initialTask?.id) { mutableIntStateOf(initialTask?.minute ?: 0) }
    var rule by remember(initialTask?.id) {
        mutableStateOf(initialTask?.toScheduleRuleOption() ?: ScheduleRuleOption.WEEKDAY)
    }
    var date by remember(initialTask?.id) { mutableStateOf(initialTask?.initialDate() ?: LocalDate.now()) }
    var appQuery by remember(initialTask?.id) { mutableStateOf("") }
    var showRuleSheet by remember { mutableStateOf(false) }
    var showRuleDetailsFor by remember { mutableStateOf<ScheduleRuleOption?>(null) }
    var selections by remember(initialTask?.id) { mutableStateOf(RuleSelections()) }
    var showAppPicker by remember(initialTask?.id) { mutableStateOf(false) }
    val isEditing = initialTask != null
    val visibleApps = apps.filter { app ->
        app.label.contains(appQuery, ignoreCase = true) || app.packageName.contains(appQuery, ignoreCase = true)
    }
    val selectedAppForDisplay = selectedApp?.let { selected ->
        apps.firstOrNull { it.packageName == selected.packageName } ?: selected
    }
    val timeLabel = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(if (isEditing) "编辑任务" else "新建任务", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (isEditing) "调整下次自动启动的条件" else "设置一次可靠的自动启动",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = { TextButton(onClick = onCancel) { Text("取消") } },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel) { Text("稍后再说") }
                    Button(
                        enabled = selectedApp != null,
                        onClick = { selectedApp?.let { onSave(it, hour, minute, date, rule, selections) } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (isEditing) "保存修改" else "保存并开启")
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FormSection("启动应用", "任务触发时将尝试打开此应用") {
                if (selectedApp != null) {
                    SelectedAppCard(
                        app = requireNotNull(selectedAppForDisplay),
                        onChange = {
                            appQuery = ""
                            showAppPicker = true
                        },
                    )
                } else {
                    AddAppCard(onClick = {
                        appQuery = ""
                        showAppPicker = true
                    })
                }
            }

            FormSection("执行时间", "按设备当前时区计算下一次触发") {
                ValueCard(
                    headline = timeLabel,
                    supporting = "点击修改时间",
                    prominent = true,
                    onClick = {
                        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                            hour = selectedHour
                            minute = selectedMinute
                        }, hour, minute, true).show()
                    },
                )
            }

            FormSection("重复规则", ruleDescription(rule, date, selections)) {
                ValueCard(
                    headline = rule.label,
                    supporting = ruleDescription(rule, date, selections),
                    trailing = "更改",
                    onClick = { showRuleSheet = true },
                )
            }

            SchedulePreview(
                appName = selectedApp?.label,
                timeLabel = timeLabel,
                description = ruleDescription(rule, date, selections),
            )
        }
    }

    if (showRuleSheet) {
        ModalBottomSheet(onDismissRequest = { showRuleSheet = false }) {
            Text(
                "选择重复规则",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "日期会作为单次、每周、每月和每年规则的基准。",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ScheduleRuleOption.entries.forEach { option ->
                ListItem(
                    modifier = Modifier.fillMaxWidth().clickable {
                        rule = option
                        showRuleSheet = false
                        if (option in setOf(ScheduleRuleOption.WEEKLY, ScheduleRuleOption.MONTHLY, ScheduleRuleOption.YEARLY, ScheduleRuleOption.ONCE)) {
                            showRuleDetailsFor = option
                        }
                    },
                    headlineContent = { Text(option.label) },
                    supportingContent = { Text(ruleDescription(option, date)) },
                    trailingContent = {
                        if (rule == option) Text("已选择", color = MaterialTheme.colorScheme.primary)
                    },
                )
            }
            Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {}
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            query = appQuery,
            apps = visibleApps,
            selectedPackageName = selectedApp?.packageName,
            onQueryChanged = { appQuery = it },
            onAppSelected = { app ->
                selectedApp = app
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }

    when (showRuleDetailsFor) {
        ScheduleRuleOption.WEEKLY -> MultiChoiceDialog(
            title = "选择每周执行日",
            items = java.time.DayOfWeek.entries.map { it.localizedName() },
            selectedIndexes = selections.weeklyDays.map { it.value - 1 }.toSet(),
            onConfirm = { indexes ->
                selections = selections.copy(weeklyDays = indexes.map { java.time.DayOfWeek.of(it + 1) }.toSet())
                showRuleDetailsFor = null
            },
            onDismiss = { showRuleDetailsFor = null },
        )
        ScheduleRuleOption.MONTHLY -> MultiChoiceDialog(
            title = "选择每月执行日期",
            items = (1..31).map { "$it 日" },
            selectedIndexes = selections.monthlyDays.map { it - 1 }.toSet(),
            onConfirm = { indexes ->
                selections = selections.copy(monthlyDays = indexes.map { it + 1 }.toSet())
                showRuleDetailsFor = null
            },
            onDismiss = { showRuleDetailsFor = null },
        )
        ScheduleRuleOption.YEARLY -> DateListDialog(
            title = "选择每年执行日期",
            dates = selections.yearlyDates.map { LocalDate.of(2024, it.monthValue, it.dayOfMonth) }.toSet(),
            onConfirm = { dates ->
                selections = selections.copy(yearlyDates = dates.map { java.time.MonthDay.of(it.monthValue, it.dayOfMonth) }.toSet())
                showRuleDetailsFor = null
            },
            onDismiss = { showRuleDetailsFor = null },
        )
        ScheduleRuleOption.ONCE -> DateListDialog(
            title = "选择单次执行日期",
            dates = selections.onceDates,
            onConfirm = { dates ->
                selections = selections.copy(onceDates = dates)
                showRuleDetailsFor = null
            },
            onDismiss = { showRuleDetailsFor = null },
        )
        else -> Unit
    }
}

@Composable
private fun FormSection(
    title: String,
    supportingText: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            supportingText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SelectedAppCard(app: LaunchableApp, onChange: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppVisual(app)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onChange) { Text("更换") }
        }
    }
}

@Composable
private fun AddAppCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("＋", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("添加要启动的应用", style = MaterialTheme.typography.titleMedium)
                Text("从已安装的可启动应用中选择", style = MaterialTheme.typography.bodyMedium)
            }
            Text("选择", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AppPickerDialog(
    query: String,
    apps: List<LaunchableApp>,
    selectedPackageName: String?,
    onQueryChanged: (String) -> Unit,
    onAppSelected: (LaunchableApp) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("选择应用", style = MaterialTheme.typography.titleLarge)
                        Text("仅显示系统可启动的应用", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索应用名称或包名") },
                    singleLine = true,
                )
                if (apps.isEmpty()) {
                    EmptyAppResult()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(apps, key = LaunchableApp::packageName) { app ->
                            AppOption(
                                app = app,
                                selected = selectedPackageName == app.packageName,
                                onClick = { onAppSelected(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppVisual(app: LaunchableApp) {
    Surface(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (app.icon == null) {
            Text(
                app.label.take(1),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE }
                },
                update = { imageView -> imageView.setImageDrawable(app.icon) },
                modifier = Modifier.fillMaxSize().padding(5.dp),
            )
        }
    }
}

@Composable
private fun ValueCard(
    headline: String,
    supporting: String,
    trailing: String? = null,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (prominent) 18.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    headline,
                    style = if (prominent) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium,
                    color = if (prominent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                trailing ?: "修改",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SchedulePreview(appName: String?, timeLabel: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("执行预览", style = MaterialTheme.typography.labelLarge)
            Text(
                appName?.let { "$timeLabel 打开$it" } ?: "请选择要启动的应用",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyAppResult() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            "没有匹配的可启动应用。请调整关键词，或确认目标应用已安装。",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppOption(app: LaunchableApp, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppVisual(app)
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) Text("已选", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MultiChoiceDialog(
    title: String,
    items: List<String>,
    selectedIndexes: Set<Int>,
    onConfirm: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(title, selectedIndexes) { mutableStateOf(selectedIndexes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(items.indices.toList()) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = if (index in selected) selected - index else selected + index
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = index in selected, onCheckedChange = null)
                        Text(items[index], modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = { onConfirm(selected) },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DateListDialog(
    title: String,
    dates: Set<LocalDate>,
    onConfirm: (Set<LocalDate>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedDates by remember(title, dates) { mutableStateOf(dates) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("可重复点击“添加日期”选择多个日期。", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    val initial = selectedDates.minOrNull() ?: LocalDate.now()
                    DatePickerDialog(context, { _, year, month, day ->
                        selectedDates = selectedDates + LocalDate.of(year, month + 1, day)
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }) { Text("添加日期") }
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(selectedDates.sorted()) { selectedDate ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA)),
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { selectedDates = selectedDates - selectedDate }) { Text("移除") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedDates.isNotEmpty(),
                onClick = { onConfirm(selectedDates) },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun ruleDescription(
    rule: ScheduleRuleOption,
    date: LocalDate,
    selections: RuleSelections = RuleSelections(),
): String = when (rule) {
    ScheduleRuleOption.DAILY -> "每天在设定时间执行"
    ScheduleRuleOption.WEEKDAY -> "周一至周五执行"
    ScheduleRuleOption.WEEKEND -> "周六、周日执行"
    ScheduleRuleOption.WEEKLY -> selections.weeklyDays.ifEmpty { setOf(date.dayOfWeek) }
        .sortedBy { it.value }.joinToString("、", prefix = "每周", postfix = "执行") { it.localizedName() }
    ScheduleRuleOption.MONTHLY -> selections.monthlyDays.ifEmpty { setOf(date.dayOfMonth) }
        .sorted().joinToString("、", prefix = "每月 ", postfix = " 日执行")
    ScheduleRuleOption.ONCE -> "在 ${selections.onceDates.ifEmpty { setOf(date) }.size} 个所选日期执行一次"
    ScheduleRuleOption.YEARLY -> "每年在 ${selections.yearlyDates.ifEmpty { setOf(java.time.MonthDay.of(date.monthValue, date.dayOfMonth)) }.size} 个日期执行"
}

private fun java.time.DayOfWeek.localizedName(): String = when (this) {
    java.time.DayOfWeek.MONDAY -> "一"
    java.time.DayOfWeek.TUESDAY -> "二"
    java.time.DayOfWeek.WEDNESDAY -> "三"
    java.time.DayOfWeek.THURSDAY -> "四"
    java.time.DayOfWeek.FRIDAY -> "五"
    java.time.DayOfWeek.SATURDAY -> "六"
    java.time.DayOfWeek.SUNDAY -> "日"
}
