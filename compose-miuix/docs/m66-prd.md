# M66 PRD：交互习惯开关

## 状态

进行中。

## 目标

原 Flutter 版「画质设置」页中包含多个交互相关开关（异形屏、H 限制、返回再退出、滑动切换作品）。当前 MIUIX 版缺少统一入口。M66 将这些开关抽取为独立「交互习惯开关」页，仅做 UI 层替换，数据沿用旧版键名，不新增业务功能。

## 范围

- `SettingsRepository` 已新增以下属性（本次只做 UI 消费）：
  - `isBangs`（`is_bangs`，默认 `false`）
  - `hIsNotAllow`（`h_is_not_allow`，默认 `false`）
  - `isReturnAgainToExit`（`return_again_to_exit`，默认 `false`）
  - `swipeChangeArtwork`（`swipe_change_artwork`，默认 `true`）
- `RootComponent` 新增 `Config.InteractionSetting` 与 `Child.InteractionSetting`，提供 `onInteractionSettingClicked()` 导航。
- `RootContent` 映射 `Child.InteractionSetting` 到新的 `InteractionSettingScreen`。
- 创建 `InteractionSettingScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent`、`Switch` 等 MIUIX 0.8.8 组件。
  - 展示四个开关项：
    - 异形屏适配（`isBangs`）
    - H 是不行的（`hIsNotAllow`）
    - 再次返回退出（`isReturnAgainToExit`）
    - 滑动切换作品（`swipeChangeArtwork`）
  - 切换后立即写回 `SettingsRepository`。
- `SettingsScreen` 新增「交互」分组，放置「交互习惯」入口。

## 不在范围

- 不实现开关在业务层的实际效果（如返回键拦截、滑动翻页等）。
- 不修改旧版键名与默认值。
- H 限制关闭时不再弹出原版提示 Toast（MIUIX 版仅持久化状态）。

## 技术决策

- 使用 `BasicComponent` + `Switch` 实现，与 `PrivacySettingScreen`、`SaveSettingScreen` 风格一致。
- 开关状态使用 `remember { mutableStateOf(...) }` 初始化，切换时同步写回仓库。
- 「H 是不行的」文案沿用原应用中文标题；其余项使用中文展示文案。

## 实现步骤

1. `RootComponent.kt` 新增 `Config.InteractionSetting`、`Child.InteractionSetting` 与 `onInteractionSettingClicked()`。
2. `RootContent.kt` 导入 `InteractionSettingScreen` 并映射 `Child.InteractionSetting`。
3. 创建 `InteractionSettingScreen.kt`，为每个开关定义标题与仓库属性绑定。
4. `SettingsScreen.kt` 新增「交互」分组与「交互习惯」入口，参数增加 `onInteractionSettingClick`。
5. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onInteractionSettingClicked`。
6. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen` 显示「交互 > 交互习惯」入口。
- [ ] `InteractionSettingScreen` 正确展示 4 个开关及当前状态。
- [ ] 切换开关后立即写回 `SettingsRepository`。
- [ ] 返回后再次进入保持上次状态。
- [ ] Android + Desktop 双端编译通过。
- [ ] M66 code review 完成，无 P0/P1 问题遗留。
