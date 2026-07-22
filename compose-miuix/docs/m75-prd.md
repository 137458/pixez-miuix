# M75 PRD：公告板页

## 状态

规划中。

## 目标

原 Flutter 版设置页在公告数据非空时展示「公告板」入口（`BoardPage`），从远端拉取 HTML 公告列表并渲染。当前 MIUIX 版缺少该页面。M75 移植公告板页，复用现有 `BoardInfo` 模型与加载逻辑，不新增业务功能。

## 范围

- 复用现有 `BoardInfo` 数据模型与远程加载逻辑（若不存在则按原实现补充轻量级 Repository）。
- `RootComponent` 新增 `Config.Board` 与 `Child.Board`，提供 `onBoardClicked()` 导航。
- `RootContent` 映射 `Child.Board` 到新的 `BoardScreen`。
- 创建 `BoardScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`。
  - 支持下拉刷新重新加载公告。
  - 每条公告展示标题与 HTML 内容；HTML 中的链接点击后在系统浏览器或 WebView 中打开。
- `SettingsScreen` 在「关于」分组新增「公告板」入口，仅在公告列表非空时显示（与原 Flutter 行为一致）。

## 不在范围

- 不修改公告数据源 URL 或格式。
- 不实现公告推送、红点提醒、已读未读状态。
- 不实现富文本编辑或公告评论。
- 不在 Desktop 端内嵌 WebView；链接打开方式回退到系统浏览器。

## 技术决策

- 与 `AboutScreen` 同处「关于」分组，入口动态显示。
- HTML 渲染使用 Compose Multiplatform 可用的 HTML 组件或 `Text` + `AnnotatedString` 做简单富文本；若 KMP HTML 库未接入，先展示纯文本内容并保留链接点击。
- 下拉刷新使用 MIUIX / Compose 的刷新指示器或手动刷新按钮。
- 空状态或加载失败时展示占位提示。

## 实现步骤

1. 确认 `shared` 模块中 `BoardInfo` 模型与加载逻辑已存在；若无则补充。
2. `RootComponent.kt` 新增 `Config.Board`、`Child.Board` 与 `onBoardClicked()`。
3. `RootContent.kt` 导入 `BoardScreen` 并映射 `Child.Board`。
4. 创建 `BoardScreen.kt`：
   - 进入时异步加载公告列表。
   - 使用 `LazyColumn` 渲染标题与 HTML 内容。
   - 实现下拉刷新。
   - 链接点击调用 `openBrowser`。
5. `SettingsScreen.kt`：
   - 新增 `onBoardClick` 参数。
   - 在「关于」分组新增「公告板」入口，根据公告数据动态显示。
6. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onBoardClicked`。
7. 编译验证与 code review。

## 验收条件

- [ ] 公告数据非空时 `SettingsScreen`「关于」分组显示「公告板」入口；空时不显示。
- [ ] `BoardScreen` 正确加载并展示公告标题与内容。
- [ ] 下拉刷新可重新加载公告。
- [ ] 公告内链接可点击并在浏览器中打开。
- [ ] 加载失败时展示错误占位。
- [ ] Android + Desktop 双端编译通过。
- [ ] M75 code review 完成，无 P0/P1 问题遗留。
