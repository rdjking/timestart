package com.example.timestart.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ExecutionLogScreen(
    logs: List<ExecutionLogItem>,
    onBack: () -> Unit,
    onClearExpired: () -> Unit,
) {
    Scaffold { paddingValues ->
        LogContent(
            paddingValues = paddingValues,
            logs = logs,
            onBack = onBack,
            onClearExpired = onClearExpired,
        )
    }
}

@Composable
private fun LogContent(
    paddingValues: PaddingValues,
    logs: List<ExecutionLogItem>,
    onBack: () -> Unit,
    onClearExpired: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("本地执行日志", style = MaterialTheme.typography.headlineSmall)
        Text("日志仅保存在本机，用于诊断定时、启动请求和通知保底。")
        OutlinedButton(onClick = onBack) { Text("返回") }
        Button(onClick = onClearExpired) { Text("清理 30 天前日志") }
        if (logs.isEmpty()) {
            Text("暂无执行日志", style = MaterialTheme.typography.titleMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs, key = ExecutionLogItem::id) { log ->
                    LogCard(log)
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: ExecutionLogItem) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(log.timeLabel, style = MaterialTheme.typography.labelMedium)
            Text("${log.eventType} · ${log.resultCode}", style = MaterialTheme.typography.titleSmall)
            Text(log.message, style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("辰启执行日志", log.copyText()))
                },
            ) {
                Text("复制")
            }
        }
    }
}
