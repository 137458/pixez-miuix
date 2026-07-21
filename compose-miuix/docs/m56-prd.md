# M56 PRD：分享格式设置页

## 状态

进行中。

## 目标

原 Flutter 版提供独立的「分享格式」设置页，允许用户编辑复制到剪贴板时的作品信息模板，并支持插入占位符。当前 MIUIX 版设置页缺少该入口。M56 补齐独立的「分享格式」二级页，将 `SettingsRepository.copyInfoText` 的编辑与占位符插入以 MIUIX 组件重新呈现，仅做 UI 层替换与设置项读写，不新增分享渠道或改变分享行为。

## 必做（按最小任务量拆分）

1. **导航与路由**
   - 在 `RootComponent` 中新增 `onCopyTextSettingClicked()` 导航方法与 `Config.CopyTextSetting` 路由。
   - 在 `RootContent` 中处理 `Child.CopyTextSetting`，渲染新的 `CopyTextSettingScreen`。

2. **设置页入口调整**
   - 在 `SettingsScreen` 中新增「分享格式」入口（`BasicComponent`）。
   - 点击后调用 `onCopyTextSettingClick()` 跳转。

3. **分享格式设置页**
   - 新建 `CopyTextSettingScreen`：顶部标题栏含返回按钮与重置/保存操作，主体使用 `Column`/`LazyColumn` 展示编辑区与占位符 chips。
   - 使用 MIUIX `TextField` 展示多行模板文本，内容初始化为 `settingsRepository.copyInfoText`。
   - 提供占位符 chips：`{title}`、`{illust_id}`、`{user_id}`、`{user_name}`、`{tags}`，以及固定文本 chips：`https://www.pixiv.net/artworks/{illust_id}`、`https://www.pixiv.net/users/{user_id}`。点击 chip 时在当前光标位置插入对应文本。
   - 提供「重置」按钮，将文本恢复为默认模板 `title:{title}\npainter:{user_name}\nillust id:{illust_id}`。
   - 提供「保存」按钮，将当前文本写回 `settingsRepository.copyInfoText` 并返回上一级。

4. **依赖注入**
   - `RootContent` 向 `CopyTextSettingScreen` 传递 `settingsRepository`。

## 技术决策

- 独立 `CopyTextSettingScreen` 作为二级页面，与「主题设置」「网络设置」等现有二级页模式一致。
- 设置项从 `SettingsRepository.copyInfoText` 读写，保持与旧 Flutter `user_setting.dart` 的键兼容。
- 占位符插入复用光标/选区逻辑：若存在选区则用 chip 文本替换选区，否则在光标处插入。
- 重置模板使用与原 Flutter 版一致的默认字符串。
- 使用 MIUIX `TextField`、`Button`、`TopAppBar` 等官方组件，不引入第三方库。
- 保存后直接返回上一级，与 Flutter 版行为一致。
- 当前分享逻辑是否读取 `copyInfoText` 不在本次范围。

## 验收条件

- [ ] `RootComponent` 提供 `onCopyTextSettingClicked` 导航，`RootContent` 正确渲染 `CopyTextSettingScreen`。
- [ ] `SettingsScreen` 显示「分享格式」入口，点击进入新页面。
- [ ] `CopyTextSettingScreen` 正确展示当前模板文本，支持多行编辑。
- [ ] 点击占位符 chip 可在当前光标/选区位置插入对应文本。
- [ ] 重置按钮可将文本恢复为默认模板。
- [ ] 保存按钮将当前文本写回 `SettingsRepository.copyInfoText` 并返回。
- [ ] Android + Desktop 双端编译通过。
- [ ] M56 code review 完成，无 P0/P1 问题遗留。

## 垂直切片（Issue 拆分）

### Slice 1: 分享格式设置页基础导航与编辑区

**Blocked by**: None - can start immediately。

**用户故事覆盖**: 1、2、3（编辑区部分）。

**What to build**: 完成从设置页到分享格式设置二级页的路由打通，并在新页面中实现模板文本的多行编辑与保存。

**Acceptance criteria**:
- [ ] `RootComponent` / `RootContent` / `SettingsScreen` 已新增分享格式入口与路由。
- [ ] `CopyTextSettingScreen` 可正常进入与返回。
- [ ] 页面展示 `TextField`，初始化内容为 `settingsRepository.copyInfoText`，支持多行输入。
- [ ] 点击保存按钮将文本写回 `settingsRepository.copyInfoText` 并返回。

### Slice 2: 占位符 chips 与重置功能

**Blocked by**: Slice 1。

**用户故事覆盖**: 4、5。

**What to build**: 在分享格式设置页中补全占位符 chips（标题、画师、ID、标签、链接等）与重置默认模板功能。

**Acceptance criteria**:
- [ ] 页面展示占位符 chips，点击后在 `TextField` 当前光标/选区位置插入对应文本。
- [ ] 存在文本选区时，chip 文本替换选区内容。
- [ ] 重置按钮将 `TextField` 恢复为默认模板 `title:{title}\npainter:{user_name}\nillust id:{illust_id}`。

## 不在范围

- 分享/复制逻辑实际读取并使用 `copyInfoText`（当前仅完成 UI 与持久化）。
- 语言设置、欢迎页设置、布局模式等其他「画质设置」原页面中的非分享格式项。
- 平台专属设置（保存格式、R18 分文件夹、显示模式、照片选择器等）。
- 国际化文案，使用中文硬编码。
