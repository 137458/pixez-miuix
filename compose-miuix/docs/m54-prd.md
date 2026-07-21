# M54 PRD：下载设置页

## 状态

已完成。

## 目标

原 Flutter 版「平台设置」页包含保存路径、同时下载任务数、单文件夹模式等与下载相关的设置项。当前 MIUIX 版设置页仅提供「下载历史」入口，缺少对下载偏好的独立管理。M54 补齐独立的「下载设置」二级页，将上述设置项以 MIUIX 组件重新呈现，仅做 UI 层替换与设置项读写，不新增下载引擎能力或改变保存行为。

## 必做（按最小任务量拆分）

1. **导航与路由**
   - 在 `RootComponent` 中新增 `onDownloadSettingClicked()` 导航方法与 `Config.DownloadSetting` 路由。
   - 在 `RootContent` 中处理 `Child.DownloadSetting`，渲染新的 `DownloadSettingScreen`。

2. **设置页入口调整**
   - 在 `SettingsScreen` 的「下载」分组中新增「下载设置」入口（`BasicComponent`）。
   - 点击后调用 `onDownloadSettingClick()` 跳转。

3. **下载设置页**
   - 新建 `DownloadSettingScreen`：顶部标题栏含返回按钮，主体使用 `LazyColumn` 展示设置项。
   - **保存路径**：以 `BasicComponent` 展示当前 `storePath`（未设置时显示默认文案），点击后弹出 `SuperDialog`，内含 `TextField` 供用户输入或修改路径，确认后写回 `SettingsRepository.storePath`；空字符串视为清除路径（保存为 `null`）。
   - **同时下载任务数**：以 `BasicComponent` 展示当前 `maxRunningTask`，点击后弹出 `SuperDialog`，提供 1-5 的互斥单选项，选择后立即写回 `SettingsRepository.maxRunningTask`。
   - **单文件夹模式**：使用 `BasicComponent` 展示标题与摘要，右侧放置 `Switch` 开关，切换后立即写回 `SettingsRepository.singleFolder`。

4. **依赖注入**
   - `RootContent` 向 `DownloadSettingScreen` 传递 `settingsRepository`。

## 技术决策

- 独立 `DownloadSettingScreen` 作为二级页面，与「主题设置」「网络设置」等现有二级页模式一致。
- 所有设置项全部从 `SettingsRepository` 读写，保持与旧 Flutter `user_setting.dart` 的键兼容。
- 使用 MIUIX `BasicComponent`、`Switch`、`TextField`、`SuperDialog` 实现设置项，不引入第三方库。
- 保存路径目前以纯文本形式编辑；原 Flutter 版的 SAF/目录选择器涉及原生 MethodChannel，不在本次 UI 迁移范围，后续可在该页面直接替换选择方式而不影响设置存储。
- 同时下载任务数限制为 1-5，与原应用常见取值范围一致。
- 空路径保存为 `null`，与 `SettingsRepository.storePath` 的可空类型一致。

## 验收条件

- [x] `RootComponent` 提供 `onDownloadSettingClicked` 导航，`RootContent` 正确渲染 `DownloadSettingScreen`。
- [x] `SettingsScreen` 显示「下载设置」入口，点击进入新页面。
- [x] `DownloadSettingScreen` 正确展示保存路径、同时下载任务数、单文件夹模式设置。
- [x] 修改保存路径 / 同时下载任务数 / 单文件夹模式后，设置即时保存到 `SettingsRepository`。
- [x] 保存路径输入为空时保存为 `null`，非空时去除首尾空格后保存。
- [x] Android + Desktop 双端编译通过。
- [x] M54 code review 完成，无 P0/P1 问题遗留。

## 垂直切片（Issue 拆分）

### Slice 1: 下载设置页基础导航与单文件夹模式

**Blocked by**: None - can start immediately。

**用户故事覆盖**: 1、2、3（单文件夹模式部分）。

**What to build**: 完成从设置页到下载设置二级页的路由打通，并在新页面中实现「单文件夹模式」开关。切换后立即写回 `SettingsRepository`，页面返回后状态保持。

**Acceptance criteria**:
- [ ] `RootComponent` / `RootContent` / `SettingsScreen` 已新增下载设置入口与路由。
- [ ] `DownloadSettingScreen` 可正常进入与返回。
- [ ] 页面展示「单文件夹模式」`BasicComponent` + `Switch`，开关状态与 `SettingsRepository.singleFolder` 双向同步。

### Slice 2: 保存路径与同时下载任务数

**Blocked by**: Slice 1。

**用户故事覆盖**: 4、5。

**What to build**: 在下载设置页中实现保存路径编辑与同时下载任务数选择，均通过 `SuperDialog` 交互。

**Acceptance criteria**:
- [ ] 点击保存路径项弹出 `SuperDialog`，使用 `TextField` 编辑路径；确认后去除首尾空格，空字符串保存为 `null`，否则保存到 `SettingsRepository.storePath`。
- [ ] 点击同时下载任务数项弹出 `SuperDialog`，提供 1-5 五个互斥选项；选中后保存到 `SettingsRepository.maxRunningTask` 并刷新页面显示。

## 不在范围

- 原生目录选择器 / SAF 集成（当前仅提供文本路径编辑）。
- `saveMode`（Media/SAF/Old way）三选一及其原生权限处理。
- 保存格式、R18 分文件夹、显示模式、照片选择器等其他平台设置项。
- 设置项对下载引擎的实际影响（当前下载逻辑尚未读取这些偏好）。
- 国际化文案，使用中文硬编码。
