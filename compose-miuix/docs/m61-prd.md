# M61 PRD：保存设置页

## 状态

已完成。

## 目标

原 Flutter 版设置中存在多项与保存/下载行为相关的开关（收藏后保存、保存后收藏、长按保存确认、插画详情页跳过长按、收藏时自动添加标签），当前 MIUIX 版设置页缺少统一入口。M61 补齐保存设置页，仅做 UI 层替换，数据沿用旧版键名，不新增业务功能。

## 范围

- `SettingsRepository` 新增以下保存相关属性的读写，兼容旧版 `flutter.` 前缀键：
  - `saveAfterStar`：收藏后自动保存作品。
  - `starAfterSave`：保存后自动收藏作品。
  - `longPressSaveConfirm`：长按保存时显示确认。
  - `illustDetailSaveSkipLongPress`：插画详情页点击保存按钮直接保存，无需长按。
  - `autoTagWhenStar`：收藏作品时自动使用收藏标签。
- `RootComponent` 新增 `Config.SaveSetting` 与 `Child.SaveSetting`，提供 `onSaveSettingClicked()` 导航。
- `RootContent` 映射 `Child.SaveSetting` 到新的 `SaveSettingScreen`。
- 创建 `SaveSettingScreen`：
  - 使用 `BasicComponent` + `Switch` 展示上述五个布尔开关。
  - 切换时即时写回 `SettingsRepository`。
- `SettingsScreen` 新增「保存」分组与「保存设置」入口（位于「画质」与「下载」分组之间）。

## 不在范围

- 文件命名模板（`format`、`fileNameEval`）与保存格式页（`SaveFormatPage` / `SaveEvalPage`），将在后续里程碑单独处理。
- 保存行为在业务层的实际调用逻辑（本次仅做设置项持久化与 UI）。

## 技术决策

- 与 `PrivacySettingScreen` 保持一致：使用 `remember { mutableStateOf(...) }` 缓存状态，切换时同步写回 `SettingsRepository`。
- 各开关按语义分组展示：「保存联动」「交互确认」「收藏标签」。
- 默认值与旧版 `lib/store/user_setting.dart` 保持一致，全部默认 `false`。

## 实现步骤

1. `SettingsRepository.kt` 新增 `saveAfterStar`、`starAfterSave`、`longPressSaveConfirm`、`illustDetailSaveSkipLongPress`、`autoTagWhenStar` 属性。
2. `RootComponent.kt` 新增 `Config.SaveSetting`、`Child.SaveSetting` 与 `onSaveSettingClicked()`。
3. `RootContent.kt` 映射并传递 `settingsRepository` 与 `onBack`。
4. 创建 `SaveSettingScreen.kt`。
5. `SettingsScreen.kt` 新增「保存」分组入口。
6. 编译验证与 code review。

## 验收条件

- [x] `SettingsRepository` 可正确读写五个保存相关设置项。
- [x] `SettingsScreen` 显示「保存 > 保存设置」入口。
- [x] `SaveSettingScreen` 正确展示五个开关及其当前状态。
- [x] 切换任一开关后即时写回设置并刷新 UI。
- [x] Android + Desktop 双端编译通过。
- [x] M61 code review 完成，无 P0/P1 问题遗留。
