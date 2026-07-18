# 新建任务 MVP 设计

## 目标

让用户从首页创建一条会被立即调度的任务：选择已安装的启动器应用、时间及每天/工作日/周末规则。

## 架构

`AndroidLaunchableAppsProvider` 只查询 `MAIN`/`LAUNCHER` 活动，遵守现有最小包可见性声明。`TaskRepository.save` 在保存一个没有 `nextTriggerAt` 的启用任务时，使用 `NextTriggerCalculator` 写入下次 UTC 时间戳后再调度。编辑屏幕通过 ViewModel 在后台读取应用列表并保存；UI 不操作 PackageManager、Room 或 AlarmManager。

## 范围

- 选择可启动应用、Android 系统时间选择器、每天/工作日/周末。
- 保存、取消和必填校验。
- 不实现编辑既有任务、单次/周/月/年规则、搜索、图标、节假日模式或应用排序。

## 验收

1. 保存启用的每日任务时，任务记录有下次触发 UTC 时间戳且调度器收到该任务 ID。
2. 应用列表只来自 Launcher intent 查询，且不包含 TimeStart 自身。
3. 未选择应用时不可保存；保存成功回到首页并显示新任务。
