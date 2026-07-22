# M65 PRD：小部件推荐类型设置

## 状态

进行中。

## 目标

原 Flutter 版「画质设置」页中内嵌了桌面小部件推荐类型选择器，支持在「推荐 / 排行榜 / 关注」之间切换。当前 MIUIX 版缺少统一入口。M65 将小部件推荐类型抽取为独立设置页，仅做 UI 层替换，数据沿用旧版 `widget_illust_type` 键，不新增业务功能。

## 范围

- `SettingsRepository` 新增 `widgetIllustType: String` 属性，键名为 `widget_illust_type`，默认值为 `recom`。
- `RootComponent` 新增 `Config.WidgetRecommendSetting` 与 `Child.WidgetRecommendSetting`，提供 `onWidgetRecommendSettingClicked()` 导航。
- `RootContent` 映射 `Child.WidgetRecommendSetting` 到新的 `WidgetRecommendSettingScreen`。
- 创建 `WidgetRecommendSettingScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent` 等 MIUIX 0.8.8 组件。
  - 全屏展示 3 个互斥选项：推荐、排行榜、关注。
  - 行尾用对勾实现互斥单选，选择后立即写回 `SettingsRepository.widgetIllustType`。
- `SettingsScreen` 在「通用」分组新增「小部件推荐类型」入口（位于「语言设置」下方）。

## 不在范围

- 不调用任何平台桌面小部件插件更新实际小部件（MIUIX 版尚未集成 AppWidgetPlugin）。
- 不修改 `widget_illust_type` 键名。
- 不新增除推荐 / 排行榜 / 关注以外的选项。

## 技术决策

- 与 `WelcomePageSettingScreen` / `LanguageSettingScreen` 保持一致：全屏列表 + 行内单选，不通过 `SuperDialog` 二次展开。
- 选项值沿用旧版字符串：`recom`、`rank`、`news`。
- 默认值 `recom`，与旧版 `_typeList[0]` 一致。

## 实现步骤

1. `RootComponent.kt` 新增 `Config.WidgetRecommendSetting`、`Child.WidgetRecommendSetting` 与 `onWidgetRecommendSettingClicked()`。
2. `RootContent.kt` 导入 `WidgetRecommendSettingScreen` 并映射 `Child.WidgetRecommendSetting`。
3. 创建 `WidgetRecommendSettingScreen.kt`，定义 `WidgetRecommendOption` 数据类与 `WIDGET_RECOMMEND_OPTIONS` 列表。
4. `SettingsScreen.kt` 新增参数 `onWidgetRecommendSettingClick` 与「通用」分组入口。
5. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onWidgetRecommendSettingClicked`。
6. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen`「通用」分组显示「小部件推荐类型」入口。
- [ ] `WidgetRecommendSettingScreen` 正确展示 3 个选项及当前选中状态。
- [ ] 选择选项后立即写回 `SettingsRepository.widgetIllustType`。
- [ ] 返回后再次进入保持上次选择。
- [ ] Android + Desktop 双端编译通过。
- [ ] M65 code review 完成，无 P0/P1 问题遗留。
