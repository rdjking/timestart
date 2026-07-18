# 可启动首页壳设计

## 目标

为 TimeStart 提供一个可从桌面启动的 Compose `MainActivity`，在首个前台界面处理 Android 13+ 通知权限，并展示任务列表尚未接入时的清晰空状态。

## 范围

- Manifest 声明单个 exported launcher Activity。
- Material 3 首页：标题、通知权限状态、授权按钮、空任务说明和“新建任务”占位操作。
- Android 13+：仅在用户点击“允许通知”后调用既有 `NotificationPermission.requestFrom`；Android 12 及以下不显示授权操作。
- UI 使用可测试的 `HomeScreenState` 推导授权横幅状态。

## 非范围

- 本阶段不实现 Room 任务列表绑定、创建/编辑任务、应用选择、导航或精确闹钟设置。
- 不在 BroadcastReceiver 或首次进入页面时自动弹出运行时权限对话框。

## 数据流与错误处理

`MainActivity.onResume` 调用 `NotificationPermission.hasPermission`，将结果映射为 `HomeScreenState` 并重组页面。授权按钮只发起系统权限请求；用户拒绝或关闭系统对话框后，返回首页时继续显示解释性横幅。新建任务按钮显示“下一阶段提供”的短暂提示，避免伪装成功功能。

## 验收

1. APK 安装后在系统启动器中可见且可打开。
2. Android 13+ 未授权时显示授权入口；低版本或已授权时不显示。
3. UI 仅将通知描述为启动受限时的备用入口，不承诺第三方应用已进入前台。
