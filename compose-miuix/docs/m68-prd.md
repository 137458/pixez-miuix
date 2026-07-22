# M68 PRD：下载设置补充

## 状态

规划中。

## 目标

原 Flutter 版「平台设置」与「画质设置」中分散了保存模式、保存格式、文件名脚本、Sanity 文件夹等与下载命名/存储相关的设置。当前 MIUIX 版「下载设置」页仅包含保存路径、同时下载任务数、单文件夹模式。M68 将这些剩余下载相关设置聚合到「下载设置」页，完成下载设置收尾，仅做 UI 层替换与持久化，不新增业务功能。

## 范围

- `SettingsRepository` 已存在或新增以下属性的读写，沿用旧版键名：
  - `saveMode`：`save_mode`，默认 `0`（Media），取值 0=Media、1=SAF、2=旧模式。
  - `format`：`save_format`，默认 `{illust_id}_p{part}`。
  - `fileNameEval` / `nameEval`：`file_name_eval` / `name_eval`，默认 `0` / 空，用于脚本文件名。
  - `overSanityLevelFolder`：`is_over_sanity_level_folder`，默认 `false`。
- `DownloadSettingScreen` 新增以下设置项：
  - 「保存模式」：使用 `SuperDialog` 提供 Media / SAF / 旧模式 三选一。
  - 「保存格式」：文本编辑，支持变量占位符 `{illust_id}`、`{part}`、`{title}`、`{user_id}`、`{user_name}`，通过快捷 Chip 插入。
  - 「使用脚本文件名」：开关，开启后保存格式由 `nameEval` 脚本计算；关闭后使用 `format`。
  - 「Sanity 单独文件夹」：开关，R18 作品保存到独立文件夹。
- 所有修改即时写回 `SettingsRepository`。

## 不在范围

- 不在业务层实际调用保存逻辑或修改下载器行为。
- 不实现脚本编辑器的语法高亮或实时运行测试（仅支持从剪贴板读取 `pixez://` scheme 脚本并保存）。
- 不修改旧版键名与默认值。
- 不实现 SAF 目录选择器（MIUIX 平台文件选择器尚未接入时，保存路径仍使用文本输入）。

## 技术决策

- 与现有 `DownloadSettingScreen` 风格保持一致：使用 `BasicComponent` + `SuperDialog`/`Switch`。
- 保存格式输入框采用 `TextField`，下方展示可用变量 Chip，点击 Chip 在光标处插入占位符。
- 脚本文件名开关仅持久化 `file_name_eval` 状态；脚本代码编辑通过入口跳转（若未来 M69 拆分脚本编辑器，则此处仅保留开关）。
- 为保持小粒度，M68 只完成「下载设置」页内聚合；Android 平台专属设置（显示模式、图片选择器等）留给 M69。

## 实现步骤

1. `SettingsKeys.kt` 确认 `SAVE_MODE`、`SAVE_FORMAT`、`NAME_EVAL` 及 `FILE_NAME_EVAL_LEGACY`、`IS_OVER_SANITY_LEVEL_FOLDER` 键名已存在。
2. `SettingsRepository.kt` 新增 `saveMode`、`format`、`fileNameEval`、`nameEval`、`overSanityLevelFolder` 属性，带旧版 `flutter.` 前缀回退。
3. 扩展 `DownloadSettingScreen.kt`：
   - 新增「保存模式」项，点击弹出三选一 `SuperDialog`。
   - 新增「保存格式」项，点击弹出含 `TextField` 与变量 Chip 的 `SuperDialog`。
   - 新增「使用脚本文件名」开关。
   - 新增「Sanity 单独文件夹」开关。
4. 在 `SettingsScreen.kt`「下载」分组保持「下载设置」入口，无需新增导航。
5. 编译验证与 code review。

## 验收条件

- [ ] `DownloadSettingScreen` 显示保存模式、保存格式、脚本文件名开关、Sanity 单独文件夹四项。
- [ ] 保存模式可在 Media / SAF / 旧模式 之间切换并持久化。
- [ ] 保存格式输入支持变量插入，保存后写回 `SettingsRepository.format`。
- [ ] 脚本文件名开关状态正确，切换后立即持久化。
- [ ] Sanity 单独文件夹开关状态正确，切换后立即持久化。
- [ ] 返回后再次进入保持上次状态。
- [ ] Android + Desktop 双端编译通过。
- [ ] M68 code review 完成，无 P0/P1 问题遗留。
