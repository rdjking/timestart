# 发现与决策记录

## 需求

- 为个人使用的 Android 定时启动器生成完整 Markdown 开发文档。
- 支持单次、每天、工作日、周末、按周、按月及每年日期等规则。
- 最终交付物全部置于一个输出目录。

## 初始决策

| 决策 | 理由 |
|---|---|
| Kotlin + Jetpack Compose + Material 3 | Android 现代开发栈，适合个人项目。 |
| Room + DataStore | 分别保存任务/执行日志与轻量设置。 |
| AlarmManager + BroadcastReceiver | 支持精确或尽量精确的定时触发。 |
| 以系统通知作为后台启动受限时的保底路径 | Android 对后台 Activity 启动有限制，不能承诺无条件自动拉起。 |

## 待核验资源

- Android Developers：精确闹钟权限与 API。
- Android Developers：后台 Activity 启动限制。
- Android Developers：通知运行时权限、开机广播、软件包可见性。

## 技术核验结论

- Android 12+ 上，`setExact()`、`setExactAndAllowWhileIdle()` 和 `setAlarmClock()` 通常都需要 `SCHEDULE_EXACT_ALARM` 特殊访问；Android 14+ 对大量新安装、target 33+ 的应用默认拒绝该访问。
- Android 10 起后台 Activity 启动受限，Android 14+ 又收紧 PendingIntent 的后台启动能力。因此闹钟投递与“目标应用一定进入前台”是两件不同的事，文档设计采用尝试启动、通知保底和可追踪日志。
- Android 13+ 的 `POST_NOTIFICATIONS` 是运行时权限；拒绝后依旧可启动前台服务但常规通知不可见，本项目不以 FGS 作为默认依赖。
- Android 11+ 的应用枚举受包可见性过滤，应查询 Launcher intent 并声明最小 `<queries>`，不申请 `QUERY_ALL_PACKAGES`。
- Android 13+ 被用户设为 restricted 的应用可能延迟收到 `BOOT_COMPLETED`，所以应用前台恢复时也要重算。
- Android 16 的官方 SDK 配置为 `compileSdk = 36`、`targetSdk = 36`。
- 对 target 26+ 的 manifest receiver，`BOOT_COMPLETED`、`TIME_SET` 与 `TIMEZONE_CHANGED` 属于 Android 8+ 隐式广播限制的例外；重启恢复仍须声明 `RECEIVE_BOOT_COMPLETED`，且用户至少启动过应用一次。
- 本项目不监听 `LOCKED_BOOT_COMPLETED`：Room 数据库存于凭据保护存储，应在用户解锁后由 `BOOT_COMPLETED` 恢复，避免在 Direct Boot 期间访问不可用数据库。
- 通知是后台启动受限时的用户可控保底，不代表系统一定让后台 `startActivity()` 将第三方应用带到前台。Android 13+ 必须先得到 `POST_NOTIFICATIONS` 运行时授权；该授权只能由前台 Activity 请求，不能在 `BroadcastReceiver` 中发起。
- AGP 9.2 的内置 Kotlin 在 Compose 启用后仍要求显式应用 `org.jetbrains.kotlin.plugin.compose`；本工程与 AGP 内置 Kotlin 2.2.10 对齐，使用同版本 Compose Compiler Gradle 插件。
- 此环境中 `D:\java\jdk` 的 Gradle HTTPS 下载 Google Maven 会发生 TLS handshake termination；Android Studio JBR 21 可成功下载。所有后续 Gradle 命令应临时设定 `JAVA_HOME=D:\develop\AndroidStudio\jbr`，不修改系统 JDK。

## 资源

- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/guide/components/activities/secure-bal
- https://developer.android.com/develop/ui/compose/notifications/notification-permission
- https://developer.android.com/training/package-visibility/declaring
- https://developer.android.com/topic/performance/background-optimization
- https://developer.android.com/about/versions/16/setup-sdk
- https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
