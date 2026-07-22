# M62 PRD：跨适配设置页

## 状态

已完成。

## 目标

原 Flutter 版在「画质设置」中提供竖屏/横屏的网格列数选择，当选择「Adapt」时进入 `SettingCrossAdpaterPage`，通过滑块调整每个网格项的最小宽度，并实时预览列数。当前 MIUIX 版缺少该入口与页面。M62 补齐跨适配设置页，仅做 UI 层替换，数据沿用旧版键名，不新增业务功能。

## 范围

- `SettingsRepository` 新增以下跨适配相关属性的读写，兼容旧版 `flutter.` 前缀键：
  - `crossAdapt`：竖屏是否启用按宽度自适应列数。
  - `crossAdapterWidth`：竖屏自适应宽度阈值（100-2160）。
  - `hCrossAdapt`：横屏是否启用按宽度自适应列数。
  - `hCrossAdapterWidth`：横屏自适应宽度阈值（100-2160）。
- `RootComponent` 新增 `Config.CrossAdapterSetting` 与 `Child.CrossAdapterSetting`，提供 `onCrossAdapterSettingClicked()` 导航。
- `RootContent` 映射 `Child.CrossAdapterSetting` 到新的 `CrossAdapterSettingScreen`。
- 创建 `CrossAdapterSettingScreen`：
  - 使用 `BasicComponent` + `Switch` 控制竖屏/横屏自适应开关。
  - 开关开启后显示滑块（`Slider`）调整宽度阈值，范围 100-2160。
  - 实时显示当前阈值、屏幕宽度与计算列数。
  - 提供简易网格预览（纯色方块），展示当前阈值下列数效果。
  - 变更在滑动结束时写回 `SettingsRepository`。
- `SettingsScreen` 新增「显示」分组与「跨适配设置」入口（位于「保存」与「下载」分组之间）。

## 不在范围

- 固定列数选择（`crossCount` / `hCrossCount` 的 2/3/4 选项）以及平板模式（`padMode`）不在本次范围，将在后续「显示/布局设置」里程碑处理。
- 跨适配阈值在真实网格列表中的实际读取逻辑不在本次范围。

## 技术决策

- 与原页面一致：使用 Compose `Slider` 调整宽度阈值，范围 100-2160，默认值 100。
- 预览网格使用 `LazyVerticalGrid` 展示 20 个 1:1 方块，列数按 `screenWidth / width` 计算，最小为 1。
- 竖屏与横屏设置在同一页面分开展示，避免原 Flutter 需要两个独立页面的冗余。
- 写入时机：滑块拖动结束后（`onValueChangeFinished`）写回 `SettingsRepository`，减少持久化次数。

## 实现步骤

1. `SettingsRepository.kt` 新增 `crossAdapt`、`crossAdapterWidth`、`hCrossAdapt`、`hCrossAdapterWidth` 属性。
2. `RootComponent.kt` 新增 `Config.CrossAdapterSetting`、`Child.CrossAdapterSetting` 与 `onCrossAdapterSettingClicked()`。
3. `RootContent.kt` 映射并传递 `settingsRepository` 与 `onBack`。
4. 创建 `CrossAdapterSettingScreen.kt`。
5. `SettingsScreen.kt` 新增「显示」分组入口。
6. 编译验证与 code review。

## 验收条件

- [x] `SettingsRepository` 可正确读写四个跨适配相关设置项。
- [x] `SettingsScreen` 显示「显示 > 跨适配设置」入口。
- [x] `CrossAdapterSettingScreen` 正确展示竖屏/横屏开关、滑块与预览网格。
- [x] 开关关闭时隐藏对应滑块与预览。
- [x] 滑块调整结束后写回设置并刷新列数显示与预览。
- [x] Android + Desktop 双端编译通过。
- [x] M62 code review 完成，无 P0/P1 问题遗留。
