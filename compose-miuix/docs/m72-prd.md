# M72 PRD：浏览历史页

## 状态

已完成。

## 目标

原 Flutter 版设置页提供「历史记录」入口（`HistoryPage`），展示用户浏览过的插画缩略图网格，支持搜索与清空。当前 MIUIX 版缺少该页面。M72 移植浏览历史页，复用现有本地数据库与图片组件，不新增业务功能。

## 范围

- 复用现有 `IllustPersistDatabase` / `HistoryRepository`（若已存在）或新建本地历史数据源。
- `RootComponent` 新增 `Config.History` 与 `Child.History`，提供 `onHistoryClicked()` 导航。
- `RootContent` 映射 `Child.History` 到新的 `HistoryScreen`。
- 创建 `HistoryScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyVerticalGrid` / `IllustStaggeredGrid` 展示历史记录。
  - 顶部提供搜索框，按作品 ID 或标题过滤本地历史。
  - 点击作品进入 `IllustDetailScreen`。
  - 长按作品弹出删除确认；右上角菜单提供「清空全部」确认。
- `SettingsScreen` 在「账号」或「通用」分组新增「历史记录」入口（原 Flutter 放在第一分组，MIUIX 可并入「通用」）。

## 不在范围

- 不实现小说历史（`NovelHistory`）的混合入口；M72 只处理插画历史。
- 不新增历史记录埋点；仅消费已有 `IllustPersist` 表。
- 不修改历史记录持久化表结构。
- 不实现按时间分组、按画师筛选等增强功能。

## 技术决策

- 复用 `IllustStaggeredGrid` 或 `LazyVerticalGrid` 保持与首页一致的网格体验。
- 搜索过滤在内存中完成，避免频繁查询数据库。
- 清空全部与单条删除前均需二次确认。
- 空状态时展示占位提示。

## 实现步骤

1. 确认 `shared` 模块中已存在插画历史数据源（`IllustPersistDatabase` / `HistoryRepository`）；若无则补充轻量级 Repository。
2. `RootComponent.kt` 新增 `Config.History`、`Child.History` 与 `onHistoryClicked()`。
3. `RootContent.kt` 导入 `HistoryScreen` 并映射 `Child.History`。
4. 创建 `HistoryScreen.kt`：
   - 使用 `LazyVerticalGrid` 或复用 `IllustStaggeredGrid`。
   - 实现搜索框状态与过滤逻辑。
   - 实现点击跳转与长按删除。
   - 实现清空全部菜单。
5. `SettingsScreen.kt` 新增「历史记录」入口，参数增加 `onHistoryClick`。
6. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onHistoryClicked`。
7. 编译验证与 code review。

## 验收条件

- [x] `SettingsScreen` 显示「历史记录」入口。
- [x] `HistoryScreen` 正确加载并展示本地插画历史网格。
- [x] 点击作品可进入作品详情。
- [x] 搜索框可按关键词过滤历史。
- [x] 长按单条历史可删除；清空全部需二次确认。
- [x] Desktop 编译通过（Android 因 M75 `BoardRepository` 无关错误暂失败）。
- [ ] M72 code review 完成，无 P0/P1 问题遗留。
