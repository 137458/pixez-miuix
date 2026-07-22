# M64 PRD：语言设置页

## 状态

进行中。

## 目标

原 Flutter 版「画质设置」页中内嵌了语言选择器，支持在 11 种语言间切换，并在选择器下方展示当前语言对应的 Sponsor（贡献者）头像与名称。当前 MIUIX 版缺少统一入口。M64 将语言选择抽取为独立「语言设置页」，仅做 UI 层替换，数据沿用旧版 `language_num` 键，不新增业务功能。

## 范围

- 复用 `lib/page/about/languages.dart` 中的语言列表与 Sponsor 数据。
- 直接使用现有 `SettingsRepository.languageNum`，不新增存储键。
- `RootComponent` 新增 `Config.LanguageSetting` 与 `Child.LanguageSetting`，提供 `onLanguageSettingClicked()` 导航。
- `RootContent` 映射 `Child.LanguageSetting` 到新的 `LanguageSettingScreen`。
- 创建 `LanguageSettingScreen`：
  - 使用 `Scaffold`、`TopAppBar`、`LazyColumn`、`BasicComponent` 等 MIUIX 0.8.8 组件。
  - 全屏展示 11 个语言选项，行尾用对勾实现互斥单选。
  - 点击选项后立即写回 `SettingsRepository.languageNum`。
  - 列表底部展示当前选中语言的 Sponsor 列表（头像 + 名称），点击行为与原实现一致：在 Android 端调用系统浏览器打开 Sponsor 链接；Desktop 端暂不支持外部浏览器跳转。
- `SettingsScreen` 新增「通用」分组，放置「语言设置」入口（位于「启动」与「主题」之间）。

## 不在范围

- 不新增 `Languages` 列表以外的语言。
- 不修改 `SettingsKeys.LANGUAGE_NUM` 键名。
- 不实现应用内实时切换语言、更新 `Accept-Language` 请求头或刷新 UI 文案（MIUIX 多语言框架尚未就绪，本次仅持久化 `languageNum`）。
- Sponsor 跳转范围保持与原实现一致；Desktop 端暂不支持外部浏览器跳转。
- 不将 Sponsor 数据迁移到独立文件，继续在 `LanguageSettingScreen` 内按原数组硬编码。

## 技术决策

- 与 `WelcomePageSettingScreen` 保持一致：全屏列表 + 行内单选，不通过 `SuperDialog` 二次展开，更适合「语言设置」这种选项多、需要一览的页面。
- 语言选项顺序与展示标签完全复用原 `Languages` 数组：
  `en-US`、`zh-CN`、`zh-TW`、`ja`、`ko`、`ru`、`es`、`tr`、`id`、`fil`、`de`。
- `languageNum` 默认值 0 对应 `en-US`；若读取到越界值，回退到 0，与旧版 `languageList[languageNum]` 行为一致。
- Sponsor 区域仅展示当前选中语言的 contributors，横向排列 Avatar + Name；点击事件沿用原版的平台限制。
- 入口分组新增「通用」，使语言、启动页、主题等偏好分类更清晰，且与「主题」分组并列。

## 实现步骤

1. `RootComponent.kt` 新增 `Config.LanguageSetting`、`Child.LanguageSetting` 与 `onLanguageSettingClicked()`。
2. `RootContent.kt` 导入 `LanguageSettingScreen` 并映射 `Child.LanguageSetting`。
3. 创建 `LanguageSettingScreen.kt`：
   - 定义 `LanguageOption` 与 `Sponsor` 数据类，按原 `Languages` 数组列出语言代码与 Sponsor 信息。
   - 使用 `LazyColumn` 渲染语言列表，每项 `BasicComponent` 通过 `endActions` 显示 `SelectionIndicator`。
   - 点击后更新本地状态并写回 `settingsRepository.languageNum`。
   - 列表底部添加「语言贡献者」区域，展示当前选中语言的 sponsors。
4. `SettingsScreen.kt` 新增「通用」分组与「语言设置」入口，参数增加 `onLanguageSettingClick`。
5. 在 `RootContent` 中 `SettingsScreen` 调用处传入 `component::onLanguageSettingClicked`。
6. 编译验证与 code review。

## 验收条件

- [ ] `SettingsScreen` 显示「通用」分组及「语言设置」入口。
- [ ] `LanguageSettingScreen` 正确展示 11 种语言选项及当前选中状态。
- [ ] 选择语言后立即写回 `SettingsRepository.languageNum`。
- [ ] 返回 `SettingsScreen` 后再次进入，`LanguageSettingScreen` 保持上次选择。
- [ ] 当前选中语言对应的 Sponsor 头像与名称正确展示。
- [ ] Android + Desktop 双端编译通过。
- [ ] M64 code review 完成，无 P0/P1 问题遗留。
