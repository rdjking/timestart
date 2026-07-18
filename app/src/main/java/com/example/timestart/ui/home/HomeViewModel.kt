package com.example.timestart.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timestart.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TaskRepository,
) : ViewModel() {
    val tasks: StateFlow<List<HomeTaskItem>> = repository.observeTasks()
        .map { tasks -> tasks.map(HomeTaskItem::from) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTaskEnabled(taskId: Long, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setEnabled(taskId, enabled)
        }
    }

    fun skipTaskCurrentOccurrence(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.skipCurrentOccurrence(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(taskId)
        }
    }

    fun copyTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.copy(taskId)
        }
    }
}

class HomeViewModelFactory(
    private val repository: TaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
