# 进度日志

## 2026-07-18：法定节假日与调休规则

- 已创建并切换至 `feature/holiday-calendar` 分支，GitHub 与 Gitee 均已同步。
- 开始实现第三方节假日 API、本地缓存与两类新重复规则：法定工作日、节假日及周末。
- 已核实 timor.tech 的日期类型：0 普通工作日、1 周末、2 法定节假日、3 调休上班日；将以每日同步和本地缓存方式实现。
- 已确认 HTTPS 单日接口可用；不使用文档中的明文 HTTP 示例。
- 已实现：`STATUTORY_WORKDAY` 与 `HOLIDAY_OR_WEEKEND` 规则、Room v5 节假日缓存、HTTPS timor.tech 客户端与离线周内/周末降级。
- 已实现：节假日规则任务每天在设定时刻同步当天类型，符合规则才启动目标 App，并始终登记下一天同一时间；网络/缓存来源会写入本地日志。
- 已通过：`NextTriggerCalculatorTest`、`TaskTriggerCoordinatorTest`、`ScheduleRuleOptionTest` 定向测试与 `assembleDebug`。
- 真机安装暂未完成：`adb` 返回 `no devices/emulators found`，待设备重新连接。

## 2026-07-15

### 阶段 1：需求与技术约束核验
- **状态：** complete
- 已确认用户需要分主题的 Markdown 文档包。
- 已确认文档以个人离线使用、MVP 优先为范围。
- 已完成新工作目录检查：目录为空、不是已初始化的 Git 工作区。
- 已创建文件：`task_plan.md`、`findings.md`、`progress.md`。
- 已以 Android Developers 官方文档核验精确闹钟、后台启动、通知、包可见性、后台限制和 API 36 SDK 约束。

### 阶段 2：文档生成
- **状态：** complete
- 已在 `outputs/安卓定时启动器开发文档包/` 生成 16 个 Markdown 文档、1 个 UI 参考图。
- 文档覆盖 PRD、FSD、流程/状态机、信息架构、原型、UI/设计系统、技术选型、架构、权限适配、API、数据库、日志、测试、路线图、部署与实施计划。

### 阶段 3：质量校验与交付
- **状态：** complete
- 已检查：17 个交付文件存在；无 `TODO`/`TBD`/占位标记；UTF-8 内部 Markdown 链接全部可解析；代码围栏成对闭合；共 8 个 Mermaid 图块。

## 测试结果

| 检查 | 预期 | 实际 | 状态 |
|---|---|---|---|
| 工作区检查 | 不覆盖已有文件 | 目录为空 | 通过 |
| 文档结构检查 | 所有交付文件存在 | 17 个文件 | 通过 |
| 链接/代码围栏 | 全部可解析/成对 | 通过 UTF-8 检查 | 通过 |

## 2026-07-15：开发执行尝试

### 工程环境核验
- **状态：** complete
- 已确认当前目录不是 Git 仓库，尚无 Android 工程。
- 已确认可用：Java 21、Git、winget。
- 缺失：Android Studio、Android SDK、Gradle、Kotlin 编译器、sdkmanager、adb。
- 影响：无法按 TDD 执行 Android 单元测试、构建 APK 或完成真机验证，暂未创建任何生产代码。
- 一次工具检查因 PowerShell 管道语法错误失败；已改写命令后成功复核环境。

### 工程初始化与 Task 1（进行中）
- **状态：** in_progress
- 已定位 Android Studio：`D:\develop\AndroidStudio`；SDK：`D:\develop\Sdk`；adb、Kotlin 插件、Platform 36.1 和 Build Tools 36.0.0 可用。
- 已初始化 Git 分支：`feature/timestart-mvp`。
- 已创建 Android Gradle 工程、Gradle 9.4.1 Wrapper、API 36 构建配置与最小 Manifest。
- AGP 9.2 已内置 Kotlin，移除了不兼容的 `org.jetbrains.kotlin.android` 插件；移除了本机不存在的 JDK 17 强制 toolchain。
- 基线 `testDebugUnitTest` 已通过；构建自动安装 SDK Platform 36。
- 已按 TDD 完成 3 个 `NextTriggerCalculator` 测试与实现：工作日当天、过点顺延、周末跳过。
- 后续以同一红—绿循环新增每日、周末、自定义每周、每月固定日规则；当前 `NextTriggerCalculatorTest` 共 7 条测试全部通过。

## 五问重启检查

| 问题 | 答案 |
|---|---|
| 我在哪里？ | 阶段 1：技术约束核验。 |
| 我要去哪里？ | 生成、校验并交付 Markdown 文档包。 |
| 目标是什么？ | 形成可支撑个人 Android 定时启动器开发的完整文档。 |
| 我学到了什么？ | 见 `findings.md`。 |
| 我做了什么？ | 见本文件。 |

## 2026-07-16：数据层与系统调度

### 已完成

- Room 数据层已实现并以 Robolectric 内存数据库覆盖：任务保存、按下次时间排序、按 ID 查询、启停、执行日志按时间倒序查询。
- `TaskRepository` 已串联保存/启停/删除与抽象调度器；删除会先取消闹钟、删除任务、再写入 `TASK_DELETED` 日志。
- `TaskRequestCode` 的失败测试已经确认缺少实现会编译失败，随后实现了受 SQLite 自增 ID 范围约束的稳定 Int 请求码。
- `AlarmManagerTaskScheduler` 已实现：任务启用且有 `nextTriggerAt` 时使用 `setExactAndAllowWhileIdle(RTC_WAKEUP)`；取消使用同一 `PendingIntent`；Android 12+ 未获精确闹钟授权时不登记。
- Manifest 已声明 `SCHEDULE_EXACT_ALARM`，并注册非导出的 `AlarmReceiver`。
- 新增调度器测试覆盖登记、取消和无精确闹钟授权三种路径；`compileDebugUnitTestKotlin --offline` 已通过。

### 待验证与阻塞

- 完整 `testDebugUnitTest` 尚未运行完成：Robolectric 4.16.1 的若干测试运行时依赖不在本地缓存，在线 Gradle 请求在此环境中卡在单次守护进程通讯后超时。已清理残留 Gradle 进程，避免并发构建。
- 下一次先单独执行定向测试；不要同时启动其他 Gradle 命令：

```powershell
& '.\work\gradle-bootstrap\gradle-9.4.1\bin\gradle.bat' :app:testDebugUnitTest --tests 'com.example.timestart.platform.alarm.*' --no-daemon
```

## 2026-07-16：任务触发与启动请求闭环

### 已完成

- `TaskEntity` 现保存 `ruleValue`，并可往返转换全部 `ScheduleRule`。数据库升级为 v3，提供 `MIGRATION_2_3`，Room schema 已导出。
- `TaskDao.updateNextTriggerAt` 支持在触发后写回下一次 epoch milliseconds。
- `TaskTriggerCoordinator` 会先计算/持久化/调度下次触发，再调用 `LaunchExecutor` 并写入 `LAUNCH_REQUEST` 本地日志；单次任务无下次时间时自动关闭和取消调度。
- `AndroidLaunchExecutor` 使用目标包的 Launcher Intent 与 `FLAG_ACTIVITY_NEW_TASK`，捕获不可用、找不到 Activity 与安全异常。
- `TimeStartApplication` 和 `AlarmReceiver.goAsync()` 已将真实系统广播接到协调器。
- 执行 `:app:assembleDebug :app:compileDebugUnitTestKotlin --offline --no-daemon`，结果为 `BUILD SUCCESSFUL`；Debug APK 已生成。

## 2026-07-16：系统恢复调度

### 已完成

- `TaskRescheduler` 读取所有启用任务，在同一恢复时刻重新计算 `nextTriggerAt` 并交给系统调度器登记；过期单次任务会被停用并取消旧闹钟。
- `RescheduleReceiver` 使用 `goAsync()` 和单线程执行器，处理开机、应用更新、手动改时与改时区事件。
- Manifest 已声明 `RECEIVE_BOOT_COMPLETED`，并注册 `BOOT_COMPLETED`、`MY_PACKAGE_REPLACED`、`TIME_SET`、`TIMEZONE_CHANGED`。
- 已完成测试先行的失败编译与实现后编译验证；待 Robolectric 运行时依赖完整下载后执行测试运行。

## 2026-07-16：通知保底与通知权限
### 已完成
- 增加 `POST_NOTIFICATIONS` Manifest 权限。
- 新增 `NotificationPermission`：Android 13（API 33）起检查运行时授权；`requestFrom(Activity)` 留给后续 `MainActivity` 在前台发起，广播接收器绝不尝试弹出授权 UI。
- 新增 `AndroidNotificationFallback`：每次闹钟触发的启动请求后，建立 Android 8+ 通知渠道并给出“点按打开目标应用”的稳定 PendingIntent 入口。
- `TaskTriggerCoordinator` 会将通知结果记为 `NOTIFICATION_FALLBACK`，区分 `DISPLAYED`、`PERMISSION_DENIED`、`TARGET_UNAVAILABLE` 和 `FAILED`；即使后台启动请求被系统限制，日志仍可诊断。
- 已按 TDD 先加入 `TaskTriggerCoordinatorTest` 与 `NotificationPermissionTest`；缺少实现时 `compileDebugUnitTestKotlin` 如预期失败，补齐后同一编译命令成功。

### 验证与阻塞
- `:app:compileDebugUnitTestKotlin --offline --no-daemon` 于 2026-07-16 成功。
- 指定 Robolectric 测试仍无法执行：离线模式缺少 `org.robolectric:nativeruntime-dist-compat:1.0.18`；在线下载在 124 秒后超时。不得将“测试源码已编译”表述为“Robolectric 测试已通过”。

## 2026-07-16：可启动 Compose 首页壳
### 已完成
- 新增 `MainActivity` 并在 Manifest 声明为 exported `MAIN`/`LAUNCHER` Activity；`aapt dump badging` 已确认 Debug APK 的 launchable activity 是 `com.example.timestart.MainActivity`。
- 引入 Compose BOM `2026.06.01`、Material 3 和 Activity Compose；AGP 内置 Kotlin 的 Compose 配置额外应用 `org.jetbrains.kotlin.plugin.compose:2.2.10`。
- 首页包含标题、空任务状态、“新建任务”占位提示和 Android 13+ 通知权限卡片。权限卡片只在用户点击按钮后调用 `NotificationPermission.requestFrom`，并在 `onResume` 刷新状态。
- `HomeScreenStateTest` 已遵循红绿过程：缺少状态模型时编译失败，补齐后定向测试通过。

### 构建环境结论
- 原 `D:\java\jdk` 运行 Gradle 时，Google Maven TLS 握手被远端中断；同一 URL 可被 PowerShell 访问。
- 仅对 Gradle 命令临时设定 `JAVA_HOME=D:\develop\AndroidStudio\jbr` 后，Compose 依赖下载、`assembleDebug` 和定向 `HomeScreenStateTest` 均成功。不要更改系统 Java；后续 Gradle 验证继续使用该临时 JBR 环境。
- 运行全量 `:app:testDebugUnitTest` 在 124 秒后超时，且未生成完整 XML 结果；已运行 `gradle --stop` 清理，不将该次运行视为通过或失败。

## 2026-07-16：首页任务列表与启停
### 已完成
- `TaskDao.observeAllOrderedByNextTrigger()` 以 Flow 返回全部任务（包含暂停任务）；有下次触发时间的任务按时间升序，无下次时间的任务置后。
- `TaskRepository.observeTasks()` 透传数据库流；`HomeViewModel` 负责将实体映射为 `HomeTaskItem`，并在 IO 协程中调用 Repository 更新启停状态。
- 首页在有任务时显示 Material 3 卡片（应用名、规则、时间、Switch）；无任务时保留空状态。开关不直接调用 `AlarmManager`。
- `HomeTaskItemTest` 与 `TaskDaoTest`（包含 Flow 排序）在 JBR 离线环境中通过。

## 2026-07-16：新建任务 MVP
### 已完成
- 首页“新建任务”进入表单，可读取 Launcher 应用、选择时间，支持每天/工作日/周末，并可取消返回。
- `AndroidLaunchableAppsProvider` 仅查询 `MAIN`/`LAUNCHER`，过滤自身包名；未申请 `QUERY_ALL_PACKAGES`。
- `TaskRepository.save` 会为尚无 `nextTriggerAt` 的启用任务调用 `NextTriggerCalculator`，持久化下次 UTC 时间戳后再调度。
- Repository 定向测试（包含新建任务调度场景）与 Debug APK 构建均通过。

## 2026-07-16：完整规则表单与首页删除

### 已完成

- 新建表单接入日期选择器与 `ScheduleRuleOption`，可把每天、工作日、周末、每周、每月、单次、每年选项转换成可持久化的 `ScheduleRule`；`ScheduleRuleOptionTest` 已通过。
- 首页任务卡片加入删除入口与确认弹窗；确认操作通过 `HomeViewModel` 调用既有 Repository 删除链路，取消闹钟、删除任务并写入 `TASK_DELETED` 日志。
- 新增 `HomeViewModelTest`，验证删除任务会移除数据并取消调度。`HomeViewModelTest` 与 `TaskRepositoryTest` 定向测试通过；`assembleDebug --offline --no-daemon` 通过。

### 后续

- 实现任务编辑与执行日志查看界面。
- 在连接的真实 Android 设备或模拟器上验证完整交互和 AlarmManager 触发。

## 2026-07-16：辰启功能收尾与验收

- 应用启动器名称、首页标题与规则文案统一为“辰启”及正确中文；新增精确闹钟授权卡片。
- 已实现编辑、复制、日志查看/复制、30 天日志清理和应用名称/包名搜索。
- `aapt dump badging` 已确认 `application-label:'辰启'` 和 `MainActivity` 启动入口；Debug APK 构建成功。
- 全量 `testDebugUnitTest` 在 244 秒超时；不可声明为全绿。模拟器 Package Manager 服务缺失，故 `adb install` 无法执行，待更换或修复设备环境。

## 2026-07-18：法定节假日与调休规则真机验证

- 已通过 USB 识别 Android 设备 `49GUFQHUZ5LBRSCI`，成功安装最新 Debug APK，并使用 Monkey 启动 `com.example.timestart`。
- 应用启动正常，Room v5 数据库迁移未导致启动崩溃。
- 节假日规则的实际联网判断发生在闹钟触发时；可创建一个两分钟后的“法定工作日”或“节假日及周末”任务，结合本地执行日志观察最终启动或跳过结果。

## 2026-07-18：全量自动化单元测试

- 已执行 `:app:testDebugUnitTest --no-daemon`，共生成 19 个测试套件、55 个测试用例。
- 测试报告统计：`FAILURES=0`、`ERRORS=0`；当前自动化单元测试全绿。
