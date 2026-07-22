# M74 PRD：应用数据导入导出页

## 状态

已完成。

## 目标

原 Flutter 版设置页提供「应用数据」入口（`DataExportPage`），支持导出/导入搜索标签历史、收藏标签、插画历史、小说历史、屏蔽数据，以及清除全部缓存。当前 MIUIX 版 `SettingsScreen` 已有「清除缓存」功能，但缺少导入导出入口。M74 移植应用数据导入导出页，复用现有各 Store 的导入导出能力，不新增业务功能。

## 范围

- 复用现有各数据源的导入导出方法：
  - 搜索标签历史（`tagHistoryStore`）
  - 收藏标签（`bookTagStore`）
  - 插画历史（`historyProvider`）
  - 小说历史（`novelHistoryStore`）
  - 屏蔽数据（`muteStore`）
- `RootComponent` 新增 `Config.DataExport` 与 `Child.DataExport`，提供 `onDataExportClicked()` 导航。
- `RootContent` 映射 `Child.DataExport` 到新的 `DataExportScreen`。
- 创建 `DataExportScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent`。
  - 为每类数据提供「导出」「导入」两个入口。
  - 底部保留「清除全部缓存」入口（若 `SettingsScreen` 已保留，则此处可只展示数据操作）。
  - 操作结果通过 `ToastMessage` 提示。
- `SettingsScreen` 在「存储」分组新增「应用数据」入口（与「清除缓存」并列）。

## 不在范围

- 不新增数据格式；沿用原 Flutter 的 JSON/CSV 格式与文件选择器。
- 不实现跨应用数据迁移或云同步。
- 不修改各 Store 的导入导出业务逻辑。
- 不实现自动备份计划任务。

## 技术决策

- 与 `SettingsScreen`「存储」分组风格保持一致。
- 每类数据独立成行，左右分别放置导出/导入按钮或点击区域。
- 导入导出文件路径通过平台文件选择器（DocumentPlugin 的 KMP 等价）获取；若平台选择器未就绪，先使用文本路径输入兜底。
- 清除缓存功能仍保留在 `SettingsScreen`，避免重复。

## 实现步骤

1. 确认 `shared` 模块中各 Store（`TagHistoryStore`、`BookTagStore`、`HistoryRepository`、`NovelHistoryStore`、`MuteStore`）已提供导入导出方法。
2. `RootComponent.kt` 新增 `Config.DataExport`、`Child.DataExport` 与 `onDataExportClicked()`。
3. `RootContent.kt` 导入 `DataExportScreen` 并映射 `Child.DataExport`。
4. 创建 `DataExportScreen.kt`：
   - 使用 `LazyColumn` 列出五类数据操作入口。
   - 点击导出/导入调用对应 Store 方法并展示结果提示。
5. `SettingsScreen.kt` 在「存储」分组新增「应用数据」入口，参数增加 `onDataExportClick`。
6. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onDataExportClicked`。
7. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen`「存储」分组显示「应用数据」入口。
- [ ] `DataExportScreen` 展示搜索标签历史、收藏标签、插画历史、小说历史、屏蔽数据五类导入导出入口。
- [ ] 点击导出/导入调用对应 Store 方法并给出成功/失败提示。
- [ ] 导入导出文件路径处理与错误提示符合平台能力现状。
- [ ] Android + Desktop 双端编译通过。
- [ ] M74 code review 完成，无 P0/P1 问题遗留。
