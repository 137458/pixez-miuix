# M69 PRD：Android 平台专属设置

## 状态

已完成（bdd9ab8a）。

## 目标

原 Flutter 版「平台设置」页（`PlatformPage`）包含若干 Android 专属选项：屏幕显示模式、图片选择器类型、默认打开链接设置。当前 MIUIX 版缺少对应入口。M69 新建「平台专属设置」页，仅做 UI 层替换与持久化，不新增业务功能。

## 范围

- `SettingsRepository` 已存在或新增以下属性的读写，沿用旧版键名：
  - `displayMode`：`display_mode`（旧版遗留键），默认 `null`。
  - `imagePickerType`：`image_picker_type_renew`，默认 `0`。
- 新增平台能力入口（无对应持久化键，调用系统设置）：
  - 「默认打开链接」：在 Android 12+ 调用 `OpenSettingPlugin` 打开系统「默认打开方式」设置页。
- `RootComponent` 新增 `Config.PlatformSetting` 与 `Child.PlatformSetting`，提供 `onPlatformSettingClicked()` 导航。
- `RootContent` 映射 `Child.PlatformSetting` 到新的 `PlatformSettingScreen`。
- 创建 `PlatformSettingScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent`、`Switch` 等 MIUIX 组件。
  - 「显示模式」：列出系统支持的刷新率/分辨率模式（需接入 `FlutterDisplayMode` 的 KMP 等价能力或平台 expect/actual），当前选中项用对勾标识；选择后立即调用平台接口并持久化 `displayMode`。
  - 「图片选择器」：开关，开启时使用系统 Photo Picker（`imagePickerType = 1`），关闭时使用传统方式（`0`）。
  - 「默认打开链接」（Android 12+ 可见）：点击跳转系统设置。
- `SettingsScreen` 在「下载」或「通用」分组新增「平台专属设置」入口，仅在 Android 端显示；Desktop 端隐藏。

## 不在范围

- 不实现 iOS / Desktop 平台专属设置（原 Flutter 页即为 Android only）。
- 不实现 SAF 目录授权、存储权限申请等运行时权限流程。
- 不接入 In-App Purchase、Google Play 支付相关入口。
- 不修改旧版键名。

## 技术决策

- 参考 `LayoutSettingScreen` / `LanguageSettingScreen`：全屏列表 + 行内单选/开关。
- 显示模式需要读取系统支持列表，优先在 `shared/src/androidMain` 提供 expect/actual 或平台接口；若 KMP 库未就绪，先完成 UI 与持久化，平台调用留 TODO。
- 入口仅在 Android 端展示，通过 `LocalPlatformContext` 或编译期 source set 判断。
- 图片选择器开关沿用旧版 `image_picker_type_renew` 键，避免与旧应用行为不一致。

## 实现步骤

1. `SettingsRepository.kt` 新增 `displayMode: Int?`、`imagePickerType: Int` 属性，支持旧键回退。
2. `RootComponent.kt` 新增 `Config.PlatformSetting`、`Child.PlatformSetting` 与 `onPlatformSettingClicked()`。
3. `RootContent.kt` 导入 `PlatformSettingScreen` 并映射 `Child.PlatformSetting`。
4. 创建 `PlatformSettingScreen.kt`：
   - 使用 `LazyColumn` 展示显示模式、图片选择器、默认打开链接三项。
   - 显示模式通过 `SuperDialog` 列出可选模式并持久化。
   - 图片选择器使用 `BasicComponent` + `Switch`。
   - 默认打开链接仅在 Android 12+ 显示，点击调用平台接口。
5. `SettingsScreen.kt` 新增「平台专属设置」入口（Android only），参数增加 `onPlatformSettingClick`。
6. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onPlatformSettingClicked`。
7. 编译验证与 code review。

## 验收条件

- [ ] Android 端 `SettingsScreen` 显示「平台专属设置」入口；Desktop 端不显示。
- [ ] `PlatformSettingScreen` 正确展示显示模式、图片选择器、默认打开链接（Android 12+）。
- [ ] 显示模式选择后持久化 `displayMode`，返回后保持选中。
- [ ] 图片选择器开关状态正确并持久化。
- [ ] 默认打开链接点击能跳转系统设置（或给出未实现提示）。
- [ ] Android + Desktop 双端编译通过。
- [ ] M69 code review 完成，无 P0/P1 问题遗留。
