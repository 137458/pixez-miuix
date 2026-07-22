# M73 PRD：下载任务页

## 状态

已完成。

## 目标

原 Flutter 版设置页提供「任务进度」入口（`JobPage`），展示下载队列与历史任务，支持按全部/运行中/完成/失败筛选、重试、删除、清空已完成。当前 MIUIX 版已有 `DownloadHistoryScreen` 但缺少任务管理（含运行中任务）。M73 移植下载任务页，复用现有下载相关数据源，不新增业务功能。

## 范围

- 复用现有 `TaskDatabase` / `DownloadRepository` / `DownloadHistoryRepository`。
- `RootComponent` 新增 `Config.DownloadTask` 与 `Child.DownloadTask`，提供 `onDownloadTaskClicked()` 导航。
- `RootContent` 映射 `Child.DownloadTask` 到新的 `DownloadTaskScreen`。
- 创建 `DownloadTaskScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn` 展示任务列表。
  - 顶部提供筛选标签：全部 / 运行中 / 完成 / 失败。
  - 每项展示作品缩略图、标题、画师、进度条（运行中）、状态图标。
  - 支持点击跳转作品详情、重试失败/种子任务、删除任务、清空已完成任务。
  - 定时刷新运行中任务进度。
- `SettingsScreen` 在「下载」分组新增「下载任务」入口（与「下载设置」「下载历史」并列）。

## 不在范围

- 不修改下载器核心调度逻辑；只展示与操作已有任务数据。
- 不实现后台下载保活、断点续传等增强功能。
- 不将任务页与下载历史页合并；两者职责保持分离。
- 不实现按账号筛选任务。

## 技术决策

- 与 `DownloadHistoryScreen` 保持一致的列表项样式。
- 使用 `rememberCoroutineScope` + `LaunchedEffect` 实现每秒刷新运行中任务进度。
- 状态变化（重试/删除）后立即刷新本地数据库列表。
- 运行中任务进度从下载器内存状态读取；历史任务从数据库读取。

## 实现步骤

1. 确认 `shared` 模块中已存在 `DownloadTask` 数据模型与 `TaskDatabase`。
2. `RootComponent.kt` 新增 `Config.DownloadTask`、`Child.DownloadTask` 与 `onDownloadTaskClicked()`。
3. `RootContent.kt` 导入 `DownloadTaskScreen` 并映射 `Child.DownloadTask`。
4. 创建 `DownloadTaskScreen.kt`：
   - 实现筛选标签状态与列表过滤。
   - 实现任务项 UI（缩略图、标题、进度、操作按钮）。
   - 实现重试、删除、清空已完成菜单。
   - 实现定时刷新。
5. `SettingsScreen.kt` 在「下载」分组新增「下载任务」入口，参数增加 `onDownloadTaskClick`。
6. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onDownloadTaskClicked`。
7. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen`「下载」分组显示「下载任务」入口。
- [ ] `DownloadTaskScreen` 正确展示全部 / 运行中 / 完成 / 失败 四类任务。
- [ ] 运行中任务显示实时进度并定时刷新。
- [ ] 失败任务可重试，已完成任务可清空，任意任务可删除。
- [ ] 点击任务可进入作品详情。
- [ ] Android + Desktop 双端编译通过。
- [ ] M73 code review 完成，无 P0/P1 问题遗留。
