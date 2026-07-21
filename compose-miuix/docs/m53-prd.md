# M53 PRD：网络设置页

## 状态

进行中。

## 目标

原 Flutter 版「网络」页提供网络模式切换（OAuth / API 服务）与图片源选择。当前 MIUIX 版设置页缺少该入口与对应二级页。M53 补齐独立的「网络设置」二级页，保持与原应用功能一致，仅做 UI 层替换与设置项读写，不新增网络引擎能力。

## 必做（按最小任务量拆分）

1. **导航与路由**
   - 在 `RootComponent` 中新增 `onNetworkSettingClicked()` 导航方法与 `Config.NetworkSetting` 路由。
   - 在 `RootContent` 中处理 `Child.NetworkSetting`，渲染新的 `NetworkSettingScreen`。

2. **设置页入口调整**
   - 在 `SettingsScreen` 新增「网络设置」分组与入口（`BasicComponent`）。
   - 点击后调用 `onNetworkSettingClick()` 跳转。

3. **网络设置页**
   - 新建 `NetworkSettingScreen`：顶部标题栏含返回按钮，主体使用 `LazyColumn` 展示设置项。
   - **网络模式**：展示「OAuth 网络模式」与「API 服务网络模式」两个分组；每个分组提供 `ech` / `compat` / `standard` 三个互斥单选项。
     - 选中项即时写回 `SettingsRepository` 的 `oauthNetworkMode` / `apiNetworkMode`。
   - **图片源**：仅在当前 API 网络模式不是 `standard` 时显示；提供「默认 (`i.pximg.net`)」、「`i.pixiv.re`」与「自定义 Host」三个选项。
     - 切换前两项即时写回 `SettingsRepository.pictureSource`。
     - 自定义 Host 使用 `TextField` 输入，确认后校验不含空格且非空，再写回仓库；非法输入给出 `ToastMessage` 提示。
     - 当用户将 API 网络模式切换为 `standard` 时，自动将图片源重置为默认 Host（与 Flutter 版 `network_mode.dart` 的 `allowsImageSource` 行为一致）。

4. **常量与默认值对齐**
   - 在 `NetworkSettingScreen` 中定义与 Flutter 版一致的 Host 常量：`DEFAULT_IMAGE_HOST = "i.pximg.net"`、`MIRROR_IMAGE_HOST = "i.pixiv.re"`。
   - 网络模式可选值与 Flutter 版 `NetworkMode.selectableValues` 顺序一致：`ech`、`compat`、`standard`。

5. **依赖注入**
   - `RootContent` 向 `NetworkSettingScreen` 传递 `settingsRepository`。

## 技术决策

- 独立 `NetworkSettingScreen` 作为二级页面，与「主题设置」「屏蔽设置」等现有二级页模式一致。
- 网络模式与图片源全部从 `SettingsRepository` 读写，保持与旧 Flutter `user_setting.dart` 的键兼容。
- 使用 MIUIX `BasicComponent` 与单选/文本输入组合实现设置项，不引入第三方库。
- 图片源自定义输入校验仅做非空与不含空格检查，与 Flutter 版行为一致；更复杂的 URL 校验不在本次范围。
- 网络模式对图片源可见性的控制逻辑直接复刻 Flutter 版：仅 `standard` 模式隐藏图片源，其余模式均显示。

## 验收条件

- [ ] `RootComponent` 提供 `onNetworkSettingClicked` 导航，`RootContent` 正确渲染 `NetworkSettingScreen`。
- [ ] `SettingsScreen` 显示「网络设置」入口，点击进入新页面。
- [ ] `NetworkSettingScreen` 正确展示 OAuth 网络模式、API 服务网络模式、图片源设置。
- [ ] 切换网络模式 / 图片源后，设置即时保存到 `SettingsRepository`。
- [ ] API 网络模式为 `standard` 时隐藏图片源分组；切换为 `ech`/`compat` 时显示，且图片源重置为默认 Host。
- [ ] 自定义 Host 输入为空或含空格时给出错误提示，不保存。
- [ ] Android + Desktop 双端编译通过。
- [ ] M53 code review 完成，无 P0/P1 问题遗留。

## 垂直切片（Issue 拆分）

### Slice 1: 网络设置页基础导航与网络模式选择

**Blocked by**: None - can start immediately.

**用户故事覆盖**: 1、2、3

**What to build**: 完成从设置页到网络设置二级页的路由打通，并在新页面中实现 OAuth 网络模式与 API 服务网络模式的单选设置。切换后立即写回 `SettingsRepository`，页面返回后状态保持。

**Acceptance criteria**:
- [ ] `RootComponent` / `RootContent` / `SettingsScreen` 已新增网络设置入口与路由。
- [ ] `NetworkSettingScreen` 可正常进入与返回。
- [ ] 页面展示 OAuth 网络模式与 API 服务网络模式两个分组，每个分组提供 `ech` / `compat` / `standard` 三个互斥选项。
- [ ] 选中项与 `SettingsRepository.oauthNetworkMode` / `apiNetworkMode` 双向同步。

### Slice 2: 图片源选择与自定义 Host

**Blocked by**: Slice 1

**用户故事覆盖**: 4、5

**What to build**: 在网络设置页中，根据当前 API 网络模式决定是否显示图片源分组；提供默认 Host、镜像 Host 与自定义 Host 三个选项，并实现自定义输入校验。

**Acceptance criteria**:
- [ ] API 网络模式为 `standard` 时隐藏图片源分组，切换为 `ech`/`compat` 时显示并将图片源重置为默认 Host。
- [ ] 默认 Host 与镜像 Host 选项可正常切换并保存到 `SettingsRepository.pictureSource`。
- [ ] 自定义 Host 输入为空或含空格时通过 `ToastMessage` 提示错误，不保存；合法输入保存后刷新当前选中状态。

## 不在范围

- 网络模式实际生效到 HTTP/DNS 层（当前共享模块尚未接入该逻辑，仅持久化设置值）。
- 图片源切换后的实时刷新、预加载或缓存清理。
- 账号切换页、下载设置页等其他设置项。
- 国际化文案，使用中文硬编码与 Flutter 版一致。
