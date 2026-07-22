# M67 PRD：动态与搜索开关

## 状态

进行中。

## 目标

原 Flutter 版「画质设置」页中包含若干 Feed / 收藏 / 搜索相关开关（Feed AI 标识、收藏后关注、使用 WebView 打开 SauceNAO）。当前 MIUIX 版缺少统一入口。M67 将这些开关抽取为独立「动态与搜索开关」页，仅做 UI 层替换，数据沿用旧版键名，不新增业务功能。

## 范围

- `SettingsRepository` 已新增以下属性（本次只做 UI 消费）：
  - `feedAIBadge`（`feed_ai_badge`，默认 `true`）
  - `followAfterStar`（`is_follow_after_star`，默认 `false`）
  - `useSaunceNaoWebview`（`use_sauce_nao_webview`，默认 `false`）
- `RootComponent` 新增 `Config.FeedSetting` 与 `Child.FeedSetting`，提供 `onFeedSettingClicked()` 导航。
- `RootContent` 映射 `Child.FeedSetting` 到新的 `FeedSettingScreen`。
- 创建 `FeedSettingScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent`、`Switch` 等 MIUIX 0.8.8 组件。
  - 展示三个开关项：
    - 显示 Feed AI 标识（`feedAIBadge`）
    - 收藏后关注画师（`followAfterStar`）
    - 使用 WebView 打开 SauceNAO（`useSaunceNaoWebview`）
  - 切换后立即写回 `SettingsRepository`。
- `SettingsScreen` 在「收藏」分组新增「动态与搜索」入口，或新建「搜索」分组。

## 不在范围

- 不实现开关在业务层的实际效果（如 AI 标识显示、自动关注、SauceNAO 打开方式）。
- 不修改旧版键名与默认值。
- 原版 SauceNAO WebView 的 Android/iOS 平台判断不再保留，MIUIX 版统一展示该开关（实际效果仍由业务层按平台能力处理）。

## 技术决策

- 使用 `BasicComponent` + `Switch` 实现，与 `PrivacySettingScreen`、`SaveSettingScreen` 风格一致。
- 开关状态使用 `remember { mutableStateOf(...) }` 初始化，切换时同步写回仓库。
- 入口暂时放在「收藏」分组下，后续若搜索相关设置继续增加，可拆分为独立「搜索」分组。

## 实现步骤

1. `RootComponent.kt` 新增 `Config.FeedSetting`、`Child.FeedSetting` 与 `onFeedSettingClicked()`。
2. `RootContent.kt` 导入 `FeedSettingScreen` 并映射 `Child.FeedSetting`。
3. 创建 `FeedSettingScreen.kt`，为每个开关定义标题与仓库属性绑定。
4. `SettingsScreen.kt` 新增「收藏 > 动态与搜索」入口，参数增加 `onFeedSettingClick`。
5. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onFeedSettingClicked`。
6. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen` 显示「动态与搜索」入口。
- [ ] `FeedSettingScreen` 正确展示 3 个开关及当前状态。
- [ ] 切换开关后立即写回 `SettingsRepository`。
- [ ] 返回后再次进入保持上次状态。
- [ ] Android + Desktop 双端编译通过。
- [ ] M67 code review 完成，无 P0/P1 问题遗留。
