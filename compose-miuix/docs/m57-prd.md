# M57 PRD：隐私设置页

## 状态

进行中。

## 目标

原 Flutter 版设置页包含「NSFW 遮罩」「默认私密收藏」等与隐私、内容展示相关的开关。当前 MIUIX 版设置页尚未提供这些入口。M57 补齐独立的「隐私设置」二级页，将 `SettingsRepository.nsfwMask` 与 `SettingsRepository.defaultPrivateLike` 以 MIUIX 组件重新呈现，仅做 UI 层替换与设置项读写，不新增内容过滤或收藏行为逻辑。

## 必做（按最小任务量拆分）

1. **导航与路由**
   - 在 `RootComponent` 中新增 `onPrivacySettingClicked()` 导航方法与 `Config.PrivacySetting` 路由。
   - 在 `RootContent` 中处理 `Child.PrivacySetting`，渲染新的 `PrivacySettingScreen`。

2. **设置页入口调整**
   - 在 `SettingsScreen` 中新增「隐私设置」入口（`BasicComponent`）。
   - 点击后调用 `onPrivacySettingClick()` 跳转。

3. **隐私设置页**
   - 新建 `PrivacySettingScreen`：顶部标题栏含返回按钮，主体使用 `LazyColumn` 展示设置项。
   - **NSFW 遮罩**：以 `BasicComponent` 展示标题与摘要，右侧使用 `Switch`，切换时即时写回 `SettingsRepository.nsfwMask`。
   - **默认私密收藏**：以 `BasicComponent` 展示标题与摘要，右侧使用 `Switch`，切换时即时写回 `SettingsRepository.defaultPrivateLike`。

4. **依赖注入**
   - `RootContent` 向 `PrivacySettingScreen` 传递 `settingsRepository`。

## 技术决策

- 独立 `PrivacySettingScreen` 作为二级页面，与「主题设置」「网络设置」「下载设置」「画质设置」「分享格式」等现有二级页模式一致。
- 设置项从 `SettingsRepository` 读写，保持与旧 Flutter `user_setting.dart` 的键兼容。
- 使用 MIUIX `BasicComponent` + `Switch` 实现布尔开关，不引入第三方库。
- 仅做设置项 UI 与持久化，当前图片加载/收藏逻辑是否读取这些设置不在本次范围。

## 验收条件

- [ ] `RootComponent` 提供 `onPrivacySettingClicked` 导航，`RootContent` 正确渲染 `PrivacySettingScreen`。
- [ ] `SettingsScreen` 显示「隐私设置」入口，点击进入新页面。
- [ ] `PrivacySettingScreen` 正确展示 NSFW 遮罩与默认私密收藏两个开关。
- [ ] 修改任一开关后，设置即时保存到 `SettingsRepository`。
- [ ] Android + Desktop 双端编译通过。
- [ ] M57 code review 完成，无 P0/P1 问题遗留。

## 垂直切片（Issue 拆分）

### Slice 1: 隐私设置页基础导航与 NSFW 遮罩

**Blocked by**: None - can start immediately。

**用户故事覆盖**: 1、2、3（NSFW 遮罩部分）。

**What to build**: 完成从设置页到隐私设置二级页的路由打通，并在新页面中实现「NSFW 遮罩」开关。

**Acceptance criteria**:
- [ ] `RootComponent` / `RootContent` / `SettingsScreen` 已新增隐私设置入口与路由。
- [ ] `PrivacySettingScreen` 可正常进入与返回。
- [ ] 页面展示「NSFW 遮罩」`BasicComponent`，右侧为 `Switch`，切换时保存到 `SettingsRepository.nsfwMask` 并刷新页面显示。

### Slice 2: 默认私密收藏开关

**Blocked by**: Slice 1。

**用户故事覆盖**: 4。

**What to build**: 在隐私设置页中补全「默认私密收藏」开关。

**Acceptance criteria**:
- [ ] 页面展示「默认私密收藏」`BasicComponent`，右侧为 `Switch`，切换时保存到 `SettingsRepository.defaultPrivateLike`。

## 不在范围

- 图片加载逻辑实际读取并使用 `nsfwMask`（当前仅完成 UI 与持久化）。
- 收藏逻辑实际读取并使用 `defaultPrivateLike`（当前仅完成 UI 与持久化）。
- 语言设置、欢迎页设置、布局模式等其他未实现的设置项。
- 平台专属设置（保存格式、R18 分文件夹、显示模式、照片选择器等）。
- 国际化文案，使用中文硬编码。
