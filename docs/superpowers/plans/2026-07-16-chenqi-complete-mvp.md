# 辰启完整 MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成离线 Android 定时启动器 MVP，并将可见应用名称统一为“辰启”。

**Architecture:** 保持单 Activity + Compose + MVVM。Room 仍是任务与诊断日志的唯一数据源；Repository 负责持久化与调度的一致性，界面只通过 ViewModel 调用 Repository。每项周期任务只登记下一次精确闹钟；后台启动仅承诺“请求启动 + 通知保底 + 日志”。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Room、Kotlin Coroutines/Flow、AlarmManager、BroadcastReceiver、Robolectric/JUnit。

## 全局约束

- `minSdk=26`、`targetSdk=36`；仅在 Gradle 命令中临时使用 `JAVA_HOME=D:\develop\AndroidStudio\jbr`。
- 不申请 `QUERY_ALL_PACKAGES`，只查询 `ACTION_MAIN` + `CATEGORY_LAUNCHER` 可见应用。
- 不实现 AccessibilityService、云同步、远程控制或默认前台服务。
- 后台 Activity 启动的结果只能描述为“已请求”，不得描述为“已成功显示”。
- 每个行为改变先写并运行失败测试，再写最小实现；验证用 `--offline --no-daemon`。

---

### Task 1：补齐任务 Repository 的更新、复制、启停日志与调度一致性

**Files:**
- Modify: `app/src/main/java/com/example/timestart/data/local/TaskDao.kt`
- Modify: `app/src/main/java/com/example/timestart/data/local/ExecutionLogDao.kt`
- Modify: `app/src/main/java/com/example/timestart/data/repository/TaskRepository.kt`
- Test: `app/src/test/java/com/example/timestart/data/repository/TaskRepositoryTest.kt`

**Interfaces:**
- Produces `TaskRepository.update(task: TaskEntity)`, `TaskRepository.copy(taskId: Long): Long`, `TaskRepository.observeLogs(taskId: Long?): Flow<List<ExecutionLogEntity>>`, and `TaskRepository.clearLogsOlderThan(cutoffMillis: Long): Int`.
- A task update must cancel the old alarm, persist a recalculated `nextTriggerAt`, schedule only the enabled valid task, and write `TASK_UPDATED`.

- [ ] Write failing tests for update cancellation/re-registration and `TASK_UPDATED`, copy preserving rule while creating a new ID, and log retention cleanup.
- [ ] Run `:app:testDebugUnitTest --tests 'com.example.timestart.data.repository.TaskRepositoryTest' --offline --no-daemon`; confirm the missing API causes failure.
- [ ] Add DAO update, log Flow/filter and deletion queries; implement the Repository methods with task ID preservation and fresh trigger calculation.
- [ ] Re-run the same targeted tests; confirm all pass.

### Task 2：实现编辑与复制任务的表单流程

**Files:**
- Modify: `app/src/main/java/com/example/timestart/ui/create/CreateTaskScreen.kt`
- Modify: `app/src/main/java/com/example/timestart/ui/create/CreateTaskViewModel.kt`
- Modify: `app/src/main/java/com/example/timestart/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/timestart/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/example/timestart/MainActivity.kt`
- Test: `app/src/test/java/com/example/timestart/ui/create/CreateTaskViewModelTest.kt`

**Interfaces:**
- Consumes `TaskRepository.update` and `TaskRepository.copy` from Task 1.
- `CreateTaskScreen` receives an optional existing `TaskEntity`; save uses the original ID for edit and ID `0` for new/duplicate tasks.

- [ ] Write a failing ViewModel test proving that edit persists the same ID and a duplicate uses a new ID.
- [ ] Run the specific ViewModel test and confirm it fails before the new edit API exists.
- [ ] Add form prefill for target app, time, date and rule; make card tap edit, and add a copy action; reuse one form rather than creating a second editor.
- [ ] Run the ViewModel and Repository tests; build the debug APK.

### Task 3：实现本地诊断日志与 30 天清理

**Files:**
- Create: `app/src/main/java/com/example/timestart/ui/logs/ExecutionLogItem.kt`
- Create: `app/src/main/java/com/example/timestart/ui/logs/ExecutionLogViewModel.kt`
- Create: `app/src/main/java/com/example/timestart/ui/logs/ExecutionLogScreen.kt`
- Modify: `app/src/main/java/com/example/timestart/MainActivity.kt`
- Modify: `app/src/main/java/com/example/timestart/TimeStartApplication.kt`
- Test: `app/src/test/java/com/example/timestart/data/local/ExecutionLogDaoTest.kt`

**Interfaces:**
- Consumes `TaskRepository.observeLogs(taskId)` and `clearLogsOlderThan`.
- The screen shows localized time, event, result and message; all logs or one task’s logs; a visible clear action only removes records older than 30 days.

- [ ] Write failing DAO tests for newest-first logs, all-log retrieval and deleting only records older than a supplied cutoff.
- [ ] Run the DAO test and confirm it fails for missing queries.
- [ ] Implement DAO/Repository methods, ViewModel state and a Material 3 log screen with optional task filter, copyable text and safe retention cleanup.
- [ ] Invoke retention cleanup once on application start and run targeted tests plus `assembleDebug`.

### Task 4：权限状态、应用选择可用性与“辰启”品牌

**Files:**
- Create: `app/src/main/java/com/example/timestart/platform/alarm/ExactAlarmPermission.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/timestart/MainActivity.kt`
- Modify: `app/src/main/java/com/example/timestart/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/timestart/ui/create/CreateTaskScreen.kt`
- Modify: `app/src/main/java/com/example/timestart/platform/apps/AndroidLaunchableAppsProvider.kt`
- Test: `app/src/test/java/com/example/timestart/platform/alarm/ExactAlarmPermissionTest.kt`

**Interfaces:**
- `ExactAlarmPermission.canSchedule(context)` and `requestFrom(activity)` return or open `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` only on Android 12+ when permission is missing.
- All visible app labels, home titles, package launcher label and documentation handoff identify the application as “辰启”.

- [ ] Write a failing SDK-gated unit test for the exact-alarm permission policy.
- [ ] Run it and confirm failure before implementation.
- [ ] Implement exact-alarm status card with user-initiated system-settings entry; show app package name in picker results and add text filtering; change manifest and UI label to “辰启”.
- [ ] Run targeted tests and inspect the built APK manifest with `aapt dump badging`.

### Task 5：完成验证、安装准备与交接

**Files:**
- Modify: `HANDOFF.md`
- Modify: `progress.md`
- Modify: `outputs/安卓定时启动器开发文档包/14_发布与自用部署.md`

- [ ] Run all local unit tests using `:app:testDebugUnitTest --offline --no-daemon`.
- [ ] Build `:app:assembleDebug --offline --no-daemon` and verify its launcher label/activity with SDK Build Tools `aapt.exe`.
- [ ] Query `adb devices`; if a device exists, install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, launch it, and run the documented short-term alarm checklist. If no device exists, record this external verification gap explicitly instead of claiming success.
- [ ] Update handoff, progress and deployment checklist with exact test/build/installation evidence.
