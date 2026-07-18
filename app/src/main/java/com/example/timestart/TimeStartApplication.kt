package com.example.timestart

import android.app.Application
import androidx.room.Room
import com.example.timestart.data.local.TimeStartDatabase
import com.example.timestart.data.local.TimeStartMigrations
import com.example.timestart.data.repository.TaskRepository
import com.example.timestart.domain.execution.TaskTriggerCoordinator
import com.example.timestart.domain.recovery.TaskRescheduler
import com.example.timestart.platform.alarm.AlarmManagerTaskScheduler
import com.example.timestart.platform.launch.AndroidLaunchExecutor
import com.example.timestart.platform.notification.AndroidNotificationFallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeStartApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: TimeStartDatabase by lazy {
        Room.databaseBuilder(this, TimeStartDatabase::class.java, "timestart.db")
            .addMigrations(TimeStartMigrations.MIGRATION_2_3)
            .build()
    }

    val taskScheduler: AlarmManagerTaskScheduler by lazy {
        AlarmManagerTaskScheduler(this, database.taskDao())
    }

    val taskTriggerCoordinator: TaskTriggerCoordinator by lazy {
        TaskTriggerCoordinator(
            taskDao = database.taskDao(),
            executionLogDao = database.executionLogDao(),
            scheduler = taskScheduler,
            launchExecutor = AndroidLaunchExecutor(this),
            notificationFallback = AndroidNotificationFallback(this),
        )
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao(), database.executionLogDao(), taskScheduler)
    }

    val taskRescheduler: TaskRescheduler by lazy {
        TaskRescheduler(database.taskDao(), taskScheduler)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            taskRepository.clearExpiredLogs()
        }
    }
}
