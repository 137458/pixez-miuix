# M55 PRD：画质设置页

## 状态

已完成。

## 目标

原 Flutter 版「画质设置」页包含 Feed 预览画质、插画详情页画质、漫画详情页画质、大图预览缩放画质等与图片质量相关的设置项。当前 MIUIX 版设置页尚未提供这些入口。M55 补齐独立的「画质设置」二级页，将上述设置项以 MIUIX 组件重新呈现，仅做 UI 层替换与设置项读写，不新增图片加载解码能力或改变画质枚举含义。

## 必做（按最小任务量拆分）

1. **导航与路由**
   - 在 `RootComponent` 中新增 `onQualitySettingClicked()` 导航方法与 `Config.QualitySetting` 路由。
   - 在 `RootContent` 中处理 `Child.QualitySetting`，渲染新的 `QualitySettingScreen`。

2. **设置页入口调整**
   - 在 `SettingsScreen` 中新增「画质设置」入口（`BasicComponent`）。
   - 点击后调用 `onQualitySettingClick()` 跳转。

3. **画质设置页**
   - 新建 `QualitySettingScreen`：顶部标题栏含返回按钮，主体使用 `LazyColumn` 展示设置项。
   - **Feed 预览画质**：以 `BasicComponent` 展示当前 `feedPreviewQuality` 对应文案，点击后弹出 `SuperDialog`，提供「标准 / 高画质 / 原图」互斥单选项，选择后立即写回 `SettingsRepository.feedPreviewQuality`。
   - **插画详情页画质**：同上，对应 `pictureQuality`。
   - **漫画详情页画质**：同上，对应 `mangaQuality`。
   - **大图预览缩放画质**：以 `BasicComponent` 展示当前 `zoomQuality` 对应文案，点击后弹出 `SuperDialog`，提供「高画质 / 原图」互斥单选项，选择后立即写回 `SettingsRepository.zoomQuality`。

4. **依赖注入**
   - `RootContent` 向 `QualitySettingScreen` 传递 `settingsRepository`。

## 技术决策

- 独立 `QualitySettingScreen` 作为二级页面，与「主题设置」「网络设置」「下载设置」等现有二级页模式一致。
- 所有设置项全部从 `SettingsRepository` 读写，保持与旧 Flutter `user_setting.dart` 的键兼容。
- 使用 MIUIX `BasicComponent`、`SuperDialog` 实现设置项，不引入第三方库。
- 画质枚举值沿用旧版整型约定：Feed/插画/漫画为 `0=medium`、`1=large`、`2=source`；缩放画质为 `0=large`、`1=source`。
- 每个选项使用独立的 `SuperDialog` 实现单选列表，与 NetworkSettingScreen / DownloadSettingScreen 的选择交互风格保持一致。
- 仅做设置项 UI 与持久化，当前图片加载逻辑是否读取这些设置不在本次范围。

## 验收条件

- [x] `RootComponent` 提供 `onQualitySettingClicked` 导航，`RootContent` 正确渲染 `QualitySettingScreen`。
- [x] `SettingsScreen` 显示「画质设置」入口，点击进入新页面。
- [x] `QualitySettingScreen` 正确展示 Feed 预览画质、插画详情页画质、漫画详情页画质、大图预览缩放画质。
- [x] 修改任一画质选项后，设置即时保存到 `SettingsRepository`。
- [x] Android + Desktop 双端编译通过。
- [x] M55 code review 完成，无 P0/P1 问题遗留。

## 垂直切片（Issue 拆分）

### Slice 1: 画质设置页基础导航与 Feed 预览画质

**Blocked by**: None - can start immediately。

**用户故事覆盖**: 1、2、3（Feed 预览画质部分）。

**What to build**: 完成从设置页到画质设置二级页的路由打通，并在新页面中实现「Feed 预览画质」选择。

**Acceptance criteria**:
- [ ] `RootComponent` / `RootContent` / `SettingsScreen` 已新增画质设置入口与路由。
- [ ] `QualitySettingScreen` 可正常进入与返回。
- [ ] 页面展示「Feed 预览画质」`BasicComponent`，点击后弹出 `SuperDialog` 提供「标准 / 高画质 / 原图」三个互斥选项，选中后保存到 `SettingsRepository.feedPreviewQuality` 并刷新页面显示。

### Slice 2: 插画 / 漫画 / 缩放画质

**Blocked by**: Slice 1。

**用户故事覆盖**: 4、5、6。

**What to build**: 在画质设置页中补全插画详情页画质、漫画详情页画质、大图预览缩放画质三个设置项，均通过 `SuperDialog` 交互。

**Acceptance criteria**:
- [ ] 页面展示「插画详情页画质」`BasicComponent`，点击后弹出 `SuperDialog` 提供「标准 / 高画质 / 原图」三个互斥选项，选中后保存到 `SettingsRepository.pictureQuality`。
- [ ] 页面展示「漫画详情页画质」`BasicComponent`，点击后弹出 `SuperDialog` 提供「标准 / 高画质 / 原图」三个互斥选项，选中后保存到 `SettingsRepository.mangaQuality`。
- [ ] 页面展示「大图预览缩放画质」`BasicComponent`，点击后弹出 `SuperDialog` 提供「高画质 / 原图」两个互斥选项，选中后保存到 `SettingsRepository.zoomQuality`。

## 不在范围

- 图片加载逻辑实际读取并使用这些画质设置（当前仅完成 UI 与持久化）。
- 布局模式、横竖屏列数、语言、欢迎页、分享格式等其他「画质设置」原页面中的非画质项。
- 平台专属设置（保存格式、R18 分文件夹、显示模式、照片选择器等）。
- 国际化文案，使用中文硬编码。
