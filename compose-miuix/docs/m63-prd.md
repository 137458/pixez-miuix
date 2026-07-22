# M63 PRD：布局设置页

## 状态

已完成。

## 目标

原 Flutter 版「画质设置」页包含平板模式、竖屏列数、横屏列数三项布局相关设置，当前 MIUIX 版缺少统一入口。M63 补布局设置页，仅做 UI 层替换，数据沿用旧版键名，不新增业务功能。

## 范围

- `SettingsRepository` 新增以下布局相关属性的读写，兼容旧版 `flutter.` 前缀键：
  - `padMode`：平板模式，取值 0/1/2，对应 "V:H" / "V:V" / "H:H"。
  - `crossCount`：竖屏固定网格列数，取值 2/3/4。
  - `hCrossCount`：横屏固定网格列数，取值 2/3/4。
- `RootComponent` 新增 `Config.LayoutSetting` 与 `Child.LayoutSetting`，提供 `onLayoutSettingClicked()` 导航。
- `RootContent` 映射 `Child.LayoutSetting` 到新的 `LayoutSettingScreen`。
- 创建 `LayoutSettingScreen`：
  - 使用 `BasicComponent` 展示「平板模式」「竖屏列数」「横屏列数」三个设置项。
  - 点击后通过 `SuperDialog` 提供互斥单选项。
  - 选择后立即写回 `SettingsRepository`。
- `SettingsScreen` 在「显示」分组新增「布局设置」入口（与「跨适配设置」并列）。

## 不在范围

- 跨适配设置（`crossAdapt` / `crossAdapterWidth` / `hCrossAdapt` / `hCrossAdapterWidth`）已在 M62 实现。
- 布局模式/列数在真实网格列表中的读取逻辑不在本次范围。
- 重启提示（原 Flutter 切换列数后提示需重启应用），MIUIX 版暂以即时保存替代。

## 技术决策

- 与 `QualitySettingScreen` 保持一致：使用 `BasicComponent` + `SuperDialog` 实现互斥选项。
- `padMode` 选项映射：0 → "V:H"、1 → "V:V"、2 → "H:H"。
- `crossCount` / `hCrossCount` 选项：2 / 3 / 4。
- 默认值：padMode = 0，crossCount = 2，hCrossCount = 2，与旧版 `lib/store/user_setting.dart` 一致。

## 实现步骤

1. `SettingsRepository.kt` 新增 `padMode`、`crossCount`、`hCrossCount` 属性。
2. `RootComponent.kt` 新增 `Config.LayoutSetting`、`Child.LayoutSetting` 与 `onLayoutSettingClicked()`。
3. `RootContent.kt` 映射并传递 `settingsRepository` 与 `onBack`。
4. 创建 `LayoutSettingScreen.kt`。
5. `SettingsScreen.kt` 在「显示」分组新增「布局设置」入口。
6. 编译验证与 code review。

## 验收条件

- [x] `SettingsRepository` 可正确读写三个布局相关设置项。
- [x] `SettingsScreen` 在「显示」分组显示「布局设置」入口。
- [x] `LayoutSettingScreen` 正确展示平板模式、竖屏列数、横屏列数。
- [x] 点击设置项弹出 SuperDialog，选择后立即写回设置并刷新 UI。
- [x] Android + Desktop 双端编译通过。
- [x] M63 code review 完成，无 P0/P1 问题遗留。
