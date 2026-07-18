package com.example.timestart.ui.home

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
fun HomeScreen(
    state: HomeScreenState,
    tasks: List<HomeTaskItem>,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onTaskEnabledChanged: (Long, Boolean) -> Unit,
    onTaskDeleted: (Long) -> Unit,
    onTaskEdited: (Long) -> Unit,
    onTaskCopied: (Long) -> Unit,
    onTaskLogsRequested: (Long) -> Unit,
    onCreateTask: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("辰启", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "定时启动器",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text(
                            text = if (tasks.isEmpty()) "准备就绪" else "${tasks.count { it.enabled }} 个运行中",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTask,
                text = { Text("新建任务") },
                icon = { Text("＋", style = MaterialTheme.typography.titleLarge) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScheduleSummary(tasks) }
            if (state.showNotificationPermissionCard) {
                item {
                    PermissionBanner(
                        title = "允许通知",
                        body = "通知会在系统限制后台启动时提供可靠的手动打开入口。",
                        actionLabel = "允许通知",
                        onAction = onRequestNotificationPermission,
                    )
                }
            }
            if (state.showExactAlarmPermissionCard) {
                item {
                    PermissionBanner(
                        title = "需要精确闹钟权限",
                        body = "未授权时，系统无法为辰启精确登记定时任务。",
                        actionLabel = "前往授权",
                        onAction = onRequestExactAlarmPermission,
                    )
                }
            }
            if (state.showOverlayPermissionCard) {
                item {
                    PermissionBanner(
                        title = "允许后台自动打开",
                        body = "允许“显示在其他应用上层”后，任务触发时才能可靠切换到目标应用。",
                        actionLabel = "前往授权",
                        onAction = onRequestOverlayPermission,
                    )
                }
            }
            if (tasks.isEmpty()) {
                item { EmptyTaskState(onCreateTask) }
            } else {
                item {
                    Text(
                        text = "我的任务",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(tasks, key = HomeTaskItem::id) { task ->
                    TaskCard(
                        task = task,
                        onEnabledChanged = onTaskEnabledChanged,
                        onDeleted = onTaskDeleted,
                        onEdited = onTaskEdited,
                        onCopied = onTaskCopied,
                        onLogsRequested = onTaskLogsRequested,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSummary(tasks: List<HomeTaskItem>) {
    val nextTask = tasks.firstOrNull { it.enabled }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "下一次执行",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (nextTask == null) {
                Text("还没有已启用的任务", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "新建一个任务后，辰启会在设定时间启动目标应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(
                            packageName = nextTask.packageName,
                            appLabel = nextTask.appLabel,
                            containerSize = 64.dp,
                            drawableSize = 54.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                nextTask.appLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                nextTask.nextTriggerDetailLabel,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    "!",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun EmptyTaskState(onCreateTask: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("从一次准时启动开始", style = MaterialTheme.typography.titleLarge)
            Text(
                "创建一个任务，按时间自动打开应用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCreateTask) { Text("新建任务") }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun TaskCard(
    task: HomeTaskItem,
    onEnabledChanged: (Long, Boolean) -> Unit,
    onDeleted: (Long) -> Unit,
    onEdited: (Long) -> Unit,
    onCopied: (Long) -> Unit,
    onLogsRequested: (Long) -> Unit,
) {
    var showDeleteConfirmation by remember(task.id) { mutableStateOf(false) }
    var showMoreMenu by remember(task.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdited(task.id) },
                onLongClick = { showMoreMenu = true },
            ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.enabled) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 80.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(task.packageName, task.appLabel)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(task.appLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            task.ruleLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            task.nextTriggerLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp)) {
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("复制") },
                        onClick = { showMoreMenu = false; onCopied(task.id) },
                    )
                    DropdownMenuItem(
                        text = { Text("日志") },
                        onClick = { showMoreMenu = false; onLogsRequested(task.id) },
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMoreMenu = false; showDeleteConfirmation = true },
                    )
                }
            }
            Switch(
                checked = task.enabled,
                onCheckedChange = { onEnabledChanged(task.id, it) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            )
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除任务？") },
            text = { Text("将取消该任务的定时调度，并删除本地任务记录。") },
            confirmButton = {
                TextButton(onClick = { onDeleted(task.id); showDeleteConfirmation = false }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    appLabel: String,
    containerSize: Dp = 48.dp,
    drawableSize: Dp = 40.dp,
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    contentDescription = appLabel
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            },
            update = { imageView -> imageView.setImageDrawable(icon) },
            modifier = Modifier.size(drawableSize),
        )
    }
}
