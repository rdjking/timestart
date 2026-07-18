# 辰启（TimeStart）交接文档

> 更新日期：2026-07-16  
> 工程目录：`D:\selfProject\Timestart`  
> 当前分支：`feature/timestart-mvp`  
> Android 包名：`com.example.timestart`；对用户显示的应用名：**辰启**。

## 1. 正在做什么

这是一个仅供个人使用的 Android 定时启动器：用户选择已安装应用及触发规则，到点后由 `AlarmManager` 唤醒，尝试启动目标应用；受 Android 后台 Activity 限制时，同时发出可点击的通知作为保底入口。应用无账号、无服务端、无分析埋点；执行情况仅保存在本地 Room 数据库。

当前目标是完成 MVP 的真机验收：创建任务、精确闹钟、触发、目标应用启动请求/通知保底、重启恢复和日志查看。

## 2. 已完成的功能

- Kotlin + Jetpack Compose + Material 3 + MVVM + Room 的 Android 项目。
- 首页任务列表：新建、编辑、复制、启停、删除及查看单任务执行日志。
- 规则引擎：每天、工作日、周末、指定星期、每月固定日、单次、每年固定月日；可计算并持久化下一次触发时间。
- 应用选择：只查询具有 `MAIN/LAUNCHER` 入口的应用，支持按应用名或包名搜索，不申请 `QUERY_ALL_PACKAGES`。
- Room：`TaskEntity`、`ExecutionLogEntity`、DAO、数据库迁移、任务 Repository 和 30 天日志清理。
- 调度：稳定 `PendingIntent` 请求码、精确闹钟授权检测、登记/取消 AlarmManager 闹钟、闹钟触发 Receiver。
- 恢复：开机、应用更新、时间更改、时区更改后重新计算所有启用任务。
- 通知保底：Android 13+ 通知权限入口；后台启动受阻时写执行日志并提供通知点击入口。
- 精确闹钟权限入口：Android 12+ 在首页显示引导卡；用户授权后，卡片自动消失。
- 产品名和启动器标签均为“辰启”。

## 3. 真机验收状态

已连接并授权一台真实 Android 设备，已完成以下验证：

1. Debug APK 成功覆盖安装。
2. 冷启动成功（约 0.9 秒），进程存活，当前前台窗口为 `com.example.timestart/.MainActivity`。
3. UI 自动化层级确认首屏已渲染“辰启”、空任务引导和“新建”按钮。
4. 精确闹钟权限卡未显示，表示该特殊权限已授权；通知权限卡仍显示，需由用户在首页点击“允许通知”完成授权。
5. 曾发现并修复首启崩溃：`MainActivity` 的属性初始化阶段调用了 `NotificationPermission.hasPermission(this)`。此时 Android 尚未向 Activity 附加 Context，真机报 `NullPointerException`。现已改为在 `onCreate()`、`super.onCreate()` 之后计算状态，真机复测通过。
6. 修复并验证新建任务表单的纵向滚动：原先“单次”规则和保存按钮会被较长应用列表挤出屏幕。表单根 Column 现使用 `verticalScroll(rememberScrollState())`，真机确认“单次”“取消”“保存”均可达。
7. 已执行一次完整的真机单次任务验收（临时目标为 ChatGPT，23:50）：任务保存后系统登记了精确 `RTC_WAKEUP` 闹钟；23:50 系统唤醒辰启并消费闹钟；本地日志记录 `LAUNCH_REQUEST · REQUESTED` 和 `NOTIFICATION_FALLBACK · DISPLAYED`；系统通知包含可点击的 `contentIntent`。该设备把目标启动请求交给系统选择器，因此不把这次结果称为“ChatGPT 已被强制显示到前台”。
8. 单次任务触发后已自动关闭，系统无下一次 `TRIGGER_TASK` 闹钟；临时任务已从首页删除，首页恢复为空状态。通知与本地日志保留为诊断记录。

尚未完成的是真实重启后的恢复验收。

## 4. 当前状态 / 是否卡住

当前**没有工程阻塞**。真机已经可用；单次任务、调度、日志和通知保底已验收，下一步可只验证真实重启恢复。

测试环境需要注意：`gradle-wrapper.properties` 已要求 Gradle 9.5；旧的 `work/gradle-bootstrap/gradle-9.4.1` 已不兼容当前 Android Gradle 插件，不能再用。2026-07-17 已以 Gradle 9.5 成功运行完整 `:app:testDebugUnitTest`：19 个测试套件、48 项测试、0 failures、0 errors。

## 5. 下一步计划（按顺序）

1. 如需额外真机覆盖，创建一个未来 2–3 分钟的启用任务，重启手机，解锁后确认该任务重新登记到 `AlarmManager`。这会中断手机使用，须由用户自行决定。
2. 维护时使用 Gradle 9.5 运行完整单元测试，并记录真实结果。

## 6. 常用命令

在工程根目录运行：

```powershell
$env:JAVA_HOME = 'D:\develop\AndroidStudio\jbr'
.\gradlew.bat :app:assembleDebug --no-daemon
```

安装并启动：

```powershell
$adb = 'D:\develop\Sdk\platform-tools\adb.exe'
& $adb install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -W -n com.example.timestart/.MainActivity
```

查看当前窗口及崩溃：

```powershell
& $adb shell dumpsys window | Select-String mCurrentFocus
& $adb logcat -d -v brief | Select-String 'FATAL EXCEPTION|AndroidRuntime|com.example.timestart'
```

## 7. 绝对不要再踩的坑

- **不要在 Activity 的属性初始化器中使用 `this` 调用 Context API。** Activity 构造时 Context 尚未附加；只在 `onCreate()` 且 `super.onCreate()` 之后读取权限、系统服务或 application。
- **不要用 Gradle 9.4.1。** 当前插件要求 Gradle 9.5，必须用 `gradlew.bat`。
- **不要声称后台启动目标 App 一定会显示到前台。** Android 10+ 可能拦截后台 `startActivity()`；当前产品的正确承诺是“发出启动请求，并以通知保底”。
- **不要跳过精确闹钟权限检查。** 调度前必须通过 `canScheduleExactAlarms()`；Android 12+ 常需要用户授予特殊访问权限。
- **不要申请 `QUERY_ALL_PACKAGES`、AccessibilityService 或默认常驻前台服务。** 均超出本项目最小权限范围。
- **不要并行执行多个 Gradle 测试。** 会竞争 Kotlin/Gradle 缓存，并放大首次依赖下载耗时。
- **不要把完整测试超时说成测试通过。** 应明确写“超时”，并列出实际已通过的定向测试或真机验证。

## 8. 关键代码位置

- 启动与导航：`app/src/main/java/com/example/timestart/MainActivity.kt`
- 首页：`app/src/main/java/com/example/timestart/ui/home/HomeScreen.kt`
- 新建/编辑任务：`app/src/main/java/com/example/timestart/ui/create/CreateTaskScreen.kt`
- Repository：`app/src/main/java/com/example/timestart/data/repository/TaskRepository.kt`
- 调度器：`app/src/main/java/com/example/timestart/platform/alarm/AlarmManagerTaskScheduler.kt`
- 闹钟/恢复 Receiver：`app/src/main/java/com/example/timestart/receiver/`
- 规则计算：`app/src/main/java/com/example/timestart/domain/scheduling/NextTriggerCalculator.kt`
- Room 数据库：`app/src/main/java/com/example/timestart/data/local/`
- 详细设计文档包：`outputs/安卓定时启动器开发文档包/`
