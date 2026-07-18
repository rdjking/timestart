package com.example.timestart.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timestart.data.local.TaskEntity
import com.example.timestart.data.repository.TaskRepository
import com.example.timestart.domain.model.ScheduleTask
import com.example.timestart.platform.apps.LaunchableApp
import com.example.timestart.platform.apps.LaunchableAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class CreateTaskViewModel(
    private val repository: TaskRepository,
    private val appsProvider: LaunchableAppsProvider,
) : ViewModel() {
    private val mutableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = mutableApps.asStateFlow()
    private val mutableEditingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = mutableEditingTask.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            mutableApps.value = appsProvider.getApps()
        }
    }

    fun save(
        app: LaunchableApp,
        hour: Int,
        minute: Int,
        ruleOption: ScheduleRuleOption,
        date: LocalDate,
        selections: RuleSelections = RuleSelections(),
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.save(
                    TaskEntity.from(
                        ScheduleTask(
                            packageName = app.packageName,
                            appLabel = app.label,
                            hour = hour,
                            minute = minute,
                            rule = ruleOption.toScheduleRule(date, hour, minute, ZoneId.systemDefault(), selections),
                        ),
                    ),
                )
            }
            onSaved()
        }
    }

    fun update(
        taskId: Long,
        app: LaunchableApp,
        hour: Int,
        minute: Int,
        ruleOption: ScheduleRuleOption,
        date: LocalDate,
        selections: RuleSelections = RuleSelections(),
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = requireNotNull(repository.getTask(taskId)) { "Task $taskId does not exist" }
                repository.update(
                    TaskEntity.from(
                        ScheduleTask(
                            packageName = app.packageName,
                            appLabel = app.label,
                            hour = hour,
                            minute = minute,
                            rule = ruleOption.toScheduleRule(date, hour, minute, ZoneId.systemDefault(), selections),
                        ),
                    ).copy(
                        id = taskId,
                        enabled = existing.enabled,
                    ),
                )
            }
            onSaved()
        }
    }

    fun beginEditing(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            mutableEditingTask.value = repository.getTask(taskId)
        }
    }

    fun stopEditing() {
        mutableEditingTask.value = null
    }
}

class CreateTaskViewModelFactory(
    private val repository: TaskRepository,
    private val appsProvider: LaunchableAppsProvider,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CreateTaskViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return CreateTaskViewModel(repository, appsProvider) as T
    }
}
