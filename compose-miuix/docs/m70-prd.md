# M70 PRD：更新设置

## 状态

规划中。

## 目标

原 Flutter 版「画质设置」页底部提供「忽略当前版本更新」开关，用于在有新版本时跳过提醒。当前 MIUIX 版缺少该入口。M70 将更新忽略逻辑抽取为独立「更新设置」页（或并入「关于」页），仅做 UI 层替换与持久化，不新增业务功能。

## 范围

- `SettingsRepository` 已存在 `ignoreUpdateVersion: String?` 属性，键名 `ignore_update_version`，默认 `null`。
- 新增 `Updater` / `UpdateChecker` 能力接入（若尚未存在）：
  - 从 GitHub Release 或项目配置读取最新版本号。
  - 对比 `AppInfo.VERSION_NAME` 判断是否有新版本。
- `RootComponent` 新增 `Config.UpdateSetting` 与 `Child.UpdateSetting`，提供 `onUpdateSettingClicked()` 导航。
- `RootContent` 映射 `Child.UpdateSetting` 到新的 `UpdateSettingScreen`。
- 创建 `UpdateSettingScreen`：
  - 展示当前版本号、最新版本号（若已获取）。
  - 提供「忽略当前版本更新」开关：开启时若存在新版本，将版本号写入 `ignoreUpdateVersion`；关闭时清空该键。
  - 提供「检查更新」按钮：手动触发版本检查。
- `SettingsScreen` 在「关于」分组新增「更新设置」入口；`AboutScreen` 也可保留版本信息展示。

## 不在范围

- 不实现应用内自动下载 APK / 增量更新。
- 不实现 iOS App Store 版本检查（原 Flutter 也只在 Android / Debug 模式展示）。
- 不修改 `ignore_update_version` 键名。
- 不将更新检查逻辑与下载器耦合。

## 技术决策

- 复用 `AppInfo.VERSION_NAME` 作为当前版本。
- 版本检查优先通过 GitHub Releases API 或项目内置的更新 JSON；若网络不可用则静默失败。
- 与 `PrivacySettingScreen` 风格一致：使用 `BasicComponent` + `Switch`。
- 入口放在「关于」分组，与「关于 PixEz」并列，符合原应用将版本信息放在关于页的习惯。

## 实现步骤

1. `SettingsRepository.kt` 确认 `ignoreUpdateVersion` 属性可读写（若未实现则补充）。
2. 在 `shared` 模块新增或复用版本检查工具（如 `UpdateChecker`），返回 `latestVersion: String?` 与是否有新版。
3. `RootComponent.kt` 新增 `Config.UpdateSetting`、`Child.UpdateSetting` 与 `onUpdateSettingClicked()`。
4. `RootContent.kt` 导入 `UpdateSettingScreen` 并映射 `Child.UpdateSetting`。
5. 创建 `UpdateSettingScreen.kt`：
   - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent`、`Switch`、`Button`。
   - 进入时异步检查版本并缓存结果。
   - 展示当前版本、最新版本、忽略开关。
6. `SettingsScreen.kt` 新增「关于 > 更新设置」入口，参数增加 `onUpdateSettingClick`。
7. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onUpdateSettingClicked`。
8. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen`「关于」分组显示「更新设置」入口。
- [ ] `UpdateSettingScreen` 正确展示当前版本号。
- [ ] 有新版时「忽略当前版本更新」开关可操作；开启后 `ignoreUpdateVersion` 写入最新版本号，关闭后清空。
- [ ] 返回后再次进入保持上次状态。
- [ ] Android + Desktop 双端编译通过。
- [ ] M70 code review 完成，无 P0/P1 问题遗留。
