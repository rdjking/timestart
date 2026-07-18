package com.example.timestart.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timestart.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionLogViewModel(
    private val repository: TaskRepository,
) : ViewModel() {
    private val selectedTaskId = MutableStateFlow<Long?>(null)
    val logs: StateFlow<List<ExecutionLogItem>> = selectedTaskId
        .flatMapLatest(repository::observeLogs)
        .map { logs -> logs.map(ExecutionLogItem::from) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun showLogsFor(taskId: Long?) {
        selectedTaskId.value = taskId
    }

    fun clearExpiredLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearExpiredLogs()
        }
    }
}

class ExecutionLogViewModelFactory(
    private val repository: TaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ExecutionLogViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return ExecutionLogViewModel(repository) as T
    }
}
