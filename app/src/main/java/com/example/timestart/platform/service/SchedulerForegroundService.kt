package com.example.timestart.platform.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.timestart.TimeStartApplication
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Keeps user-created exact schedules alive on OEM systems that aggressively freeze background apps.
 * Its persistent notification makes this ongoing work explicit to the device owner.
 */
class SchedulerForegroundService : Service() {
    private val triggerExecutor = Executors.newSingleThreadExecutor()
    private val watchdogExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val queuedTaskIds = ConcurrentHashMap.newKeySet<Long>()
    private var watchdogStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("辰启正在运行")
            .setContentText("定时启动任务将在设定时间自动执行")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startWatchdogIfNeeded()

        if (intent?.action == ACTION_TRIGGER_TASK) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, NO_TASK_ID)
            if (taskId == NO_TASK_ID) {
                Log.w(TAG, "Ignoring foreground-service trigger without a task ID")
            } else {
                queueTrigger(taskId, "exact alarm")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        watchdogExecutor.shutdownNow()
        triggerExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun startWatchdogIfNeeded() {
        if (watchdogStarted) return
        watchdogStarted = true
        watchdogExecutor.scheduleWithFixedDelay(
            {
                val application = applicationContext as? TimeStartApplication ?: return@scheduleWithFixedDelay
                val now = System.currentTimeMillis()
                application.database.taskDao()
                    .getEnabledOrderedByNextTrigger()
                    .asSequence()
                    .filter { task -> task.nextTriggerAt?.let { it <= now } == true }
                    .forEach { task -> queueTrigger(task.id, "foreground watchdog") }
            },
            0,
            WATCHDOG_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun queueTrigger(taskId: Long, source: String) {
        if (!queuedTaskIds.add(taskId)) return
        Log.i(TAG, "Queueing $source trigger for task=$taskId")
        triggerExecutor.execute {
            try {
                val application = applicationContext as? TimeStartApplication
                if (application == null) {
                    Log.e(TAG, "Application is not configured as TimeStartApplication")
                    return@execute
                }
                application.taskTriggerCoordinator.handleAlarm(taskId)
                Log.i(TAG, "Finished trigger coordination for task=$taskId")
            } finally {
                queuedTaskIds.remove(taskId)
            }
        }
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "定时调度运行状态",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "保持辰启的定时启动任务可用"
            },
        )
    }

    companion object {
        const val ACTION_TRIGGER_TASK = "com.example.timestart.action.TRIGGER_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, SchedulerForegroundService::class.java))
        }

        private const val NO_TASK_ID = -1L
        private const val TAG = "SchedulerForegroundService"
        private const val CHANNEL_ID = "scheduler_runtime"
        private const val NOTIFICATION_ID = 7_001
        private const val WATCHDOG_INTERVAL_MILLIS = 1_000L
    }
}
