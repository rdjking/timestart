# 安卓定时启动器：开发文档包

> 版本：1.0 · 日期：2026-07-15 · 目标：个人离线使用

## 产品一句话

让用户按单次、每天、工作日、周末、按周、按月或年度日期等规则，在指定时间**尝试启动**一个已安装的 Android 应用。

## 关键现实约束

Android 10 起限制后台 Activity 启动，Android 14+ 进一步收紧；因此本项目不能承诺在所有设备与所有时刻都能把目标应用带到前台。产品策略是：精确闹钟触发后先尝试启动，记录结果；若系统阻止或无法确认结果，则以高优先级本地通知提供“点按打开”保底。请先阅读 [08_权限与系统适配.md](08_权限与系统适配.md)。

## 文档导航

| 文件 | 用途 |
|---|---|
| [01_项目概述与PRD.md](01_项目概述与PRD.md) | 目标、范围、需求与验收标准 |
| [02_功能规格说明.md](02_功能规格说明.md) | 功能细节、规则定义与异常策略 |
| [03_流程与状态机.md](03_流程与状态机.md) | 用户/业务流程与任务状态机 |
| [04_信息架构与低保真原型.md](04_信息架构与低保真原型.md) | 页面结构、导航和线框图 |
| [05_UI设计稿与设计规范.md](05_UI设计稿与设计规范.md) | 高保真视觉参考、Design System 与交互规范 |
| [06_技术选型.md](06_技术选型.md) | 语言、SDK、库与版本策略 |
| [07_系统架构与模块设计.md](07_系统架构与模块设计.md) | 分层、组件边界、数据流与包结构 |
| [08_权限与系统适配.md](08_权限与系统适配.md) | Android 版本、ROM 与后台限制 |
| [09_外部API接口.md](09_外部API接口.md) | 可选节假日数据接口契约 |
| [10_数据库设计与ER图.md](10_数据库设计与ER图.md) | Room 表、索引、枚举、ER 图 |
| [11_本地日志与数据埋点.md](11_本地日志与数据埋点.md) | 本地诊断事件与隐私边界 |
| [12_测试方案.md](12_测试方案.md) | 单元、集成、真机与回归测试 |
| [13_开发路线图.md](13_开发路线图.md) | MVP 到增强版的里程碑 |
| [14_发布与自用部署.md](14_发布与自用部署.md) | 签名、安装、权限设置与排障 |
| [15_开发实施计划.md](15_开发实施计划.md) | 可执行的工程实现顺序 |

## 建议阅读顺序

1. `01`、`02`、`08`：先确认做什么、哪些事情系统不保证。
2. `06`、`07`、`10`：建立工程骨架和核心领域模型。
3. `03`、`04`、`05`：实现交互与界面。
4. `11`、`12`、`13`、`14`、`15`：测试、调试和交付。

## 官方依据（已于 2026-07-15 核验）

- [精确闹钟与 Android 14 默认拒绝策略](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [闹钟调度指南](https://developer.android.com/develop/background-work/services/alarms)
- [后台 Activity 启动限制](https://developer.android.com/guide/components/activities/secure-bal)
- [Android 13 通知运行时权限](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [软件包可见性](https://developer.android.com/training/package-visibility/declaring)
- [Android 16 SDK 配置](https://developer.android.com/about/versions/16/setup-sdk)

## 交付边界

- 本文档包不包含应用源代码或 APK。
- 默认不接入账号、云同步、分析 SDK、广告或自建服务端。
- 中国法定节假日/调休是可选增强功能；无网络或接口不可用时必须有本地星期规则兜底。
