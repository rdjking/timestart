package com.example.timestart

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.timestart.platform.notification.NotificationPermission
import com.example.timestart.platform.alarm.ExactAlarmPermission
import com.example.timestart.platform.launch.OverlayPermission
import com.example.timestart.platform.service.SchedulerForegroundService
import com.example.timestart.ui.home.HomeScreen
import com.example.timestart.ui.home.HomeScreenState
import com.example.timestart.ui.home.HomeViewModel
import com.example.timestart.ui.home.HomeViewModelFactory
import com.example.timestart.platform.apps.AndroidLaunchableAppsProvider
import com.example.timestart.ui.create.CreateTaskScreen
import com.example.timestart.ui.create.CreateTaskViewModel
import com.example.timestart.ui.create.CreateTaskViewModelFactory
import com.example.timestart.ui.logs.ExecutionLogScreen
import com.example.timestart.ui.logs.ExecutionLogViewModel
import com.example.timestart.ui.logs.ExecutionLogViewModelFactory
import com.example.timestart.ui.theme.TimeStartTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    // Android attaches the Activity context only after construction, so permission checks
    // must not run from a property initializer.
    private var homeScreenState by mutableStateOf(HomeScreenState(false, false))
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((application as TimeStartApplication).taskRepository)
    }
    private val createTaskViewModel: CreateTaskViewModel by viewModels {
        CreateTaskViewModelFactory(
            (application as TimeStartApplication).taskRepository,
            AndroidLaunchableAppsProvider(this),
        )
    }
    private val executionLogViewModel: ExecutionLogViewModel by viewModels {
        ExecutionLogViewModelFactory((application as TimeStartApplication).taskRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeScreenState = createHomeScreenState()
        SchedulerForegroundService.start(this)
        setContent {
            val tasks by homeViewModel.tasks.collectAsState()
            val apps by createTaskViewModel.apps.collectAsState()
            val editingTask by createTaskViewModel.editingTask.collectAsState()
            val logs by executionLogViewModel.logs.collectAsState()
            var showCreateTask by remember { mutableStateOf(false) }
            var showLogs by remember { mutableStateOf(false) }
            TimeStartTheme {
                if (showCreateTask) {
                    CreateTaskScreen(
                        apps = apps,
                        initialTask = editingTask,
                        onCancel = {
                            createTaskViewModel.stopEditing()
                            showCreateTask = false
                        },
                        onSave = { app, hour, minute, date, rule, selections ->
                            val onSaved = {
                                createTaskViewModel.stopEditing()
                                showCreateTask = false
                            }
                            val taskId = editingTask?.id
                            if (taskId == null) {
                                createTaskViewModel.save(app, hour, minute, rule, date, selections, onSaved)
                            } else {
                                createTaskViewModel.update(taskId, app, hour, minute, rule, date, selections, onSaved)
                            }
                        },
                    )
                } else if (showLogs) {
                    ExecutionLogScreen(
                        logs = logs,
                        onBack = { showLogs = false },
                        onClearExpired = executionLogViewModel::clearExpiredLogs,
                    )
                } else {
                    HomeScreen(
                        state = homeScreenState,
                        tasks = tasks,
                        onRequestNotificationPermission = {
                            NotificationPermission.requestFrom(this)
                        },
                        onRequestExactAlarmPermission = {
                            ExactAlarmPermission.requestFrom(this)
                        },
                        onRequestOverlayPermission = {
                            OverlayPermission.requestFrom(this)
                        },
                        onTaskEnabledChanged = homeViewModel::setTaskEnabled,
                        onTaskCurrentOccurrenceSkipped = homeViewModel::skipTaskCurrentOccurrence,
                        onTaskDeleted = homeViewModel::deleteTask,
                        onTaskEdited = { taskId ->
                            createTaskViewModel.beginEditing(taskId)
                            showCreateTask = true
                        },
                        onTaskCopied = homeViewModel::copyTask,
                        onTaskLogsRequested = { taskId ->
                            executionLogViewModel.showLogsFor(taskId)
                            showLogs = true
                        },
                        onCreateTask = {
                            createTaskViewModel.stopEditing()
                            showCreateTask = true
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeScreenState = createHomeScreenState()
    }

    private fun createHomeScreenState(): HomeScreenState = HomeScreenState.from(
        requiresRuntimePermission = NotificationPermission.isRuntimePermissionRequired(),
        hasNotificationPermission = NotificationPermission.hasPermission(this),
        requiresExactAlarmPermission = ExactAlarmPermission.requiresSpecialAccess(),
        hasExactAlarmPermission = ExactAlarmPermission.canSchedule(this),
        hasOverlayPermission = OverlayPermission.isGranted(this),
    )
}
