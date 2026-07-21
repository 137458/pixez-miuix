# M52 PRD：主题设置页

## 状态

已完成。

## 目标

原 Flutter 版「主题」页提供主题模式切换、AMOLED 模式、动态颜色与种子色选择。当前 MIUIX 版仅在 `SettingsScreen` 内置了简化的主题模式选择，缺少 AMOLED、动态颜色与种子色设置。M52 补齐独立的「主题设置」二级页，并将 `SettingsScreen` 中的主题模式选择迁移到该页面，保持与原应用功能一致。

## 必做（按最小任务量拆分）

1. **导航与路由**
   - 在 `RootComponent` 中新增 `onThemeSettingClicked()` 导航方法与 `Config.ThemeSetting` 路由。
   - 在 `RootContent` 中处理 `Child.ThemeSetting`，渲染新的 `ThemeSettingScreen`。

2. **设置页入口调整**
   - 在 `SettingsScreen` 的「主题」分组下新增「主题设置」条目（`BasicComponent`）。
   - 移除 `SettingsScreen` 当前内联的 `ThemeModeSelector`，将主题相关设置集中到 `ThemeSettingScreen`。
   - `SettingsScreen` 点击「主题设置」后调用 `onThemeSettingClick()` 跳转。

3. **主题设置页**
   - 新建 `ThemeSettingScreen`：顶部标题栏含返回按钮，主体使用 `LazyColumn` 展示设置项。
   - **主题模式**：展示「跟随系统」「浅色」「深色」三个互斥选项，高亮当前选中项。
   - **AMOLED 模式**：`Switch` 开关，开启后将深色模式背景替换为纯黑以节省 OLED 耗电。
   - **动态颜色**：`Switch` 开关，开启后使用 Monet 动态取色（系统壁纸/种子色）。
   - **种子色**：仅在关闭动态颜色时显示；展示当前种子色色块与名称，点击后弹出颜色选择对话框。

4. **颜色选择器**
   - 使用 `SuperDialog` 实现简易颜色选择器。
   - 提供一组预设颜色网格（对齐 Flutter 版 `ColorPickPage` 的 `skinList`：cyan、pink、green、brown、purple、blue、bilibili 粉）。
   - 支持自定义 HEX 颜色输入，格式为 `#RRGGBB`，校验失败时提示。
   - 选择预设颜色或确认自定义颜色后，更新种子色并保存。

5. **主题状态扩展**
   - 在 `RootContent` 中扩展 `ThemeController` 创建逻辑：
     - 根据 `themeMode` 选择基础模式（System/Light/Dark）。
     - 若 `useDynamicColor` 为 true，使用对应 Monet 模式并传入 `keyColor`。
     - 若 `isAmoled` 为 true，向 `ThemeController` 传入纯黑 `darkColors`。
   - 主题设置变更时实时写回 `SettingsRepository` 并更新 `RootContent` 中的状态。

6. **依赖注入**
   - `RootContent` 向 `SettingsScreen` 与 `ThemeSettingScreen` 传递 `settingsRepository`。

## 技术决策

- 独立 `ThemeSettingScreen` 作为二级页面，符合设置页中「关于」「屏蔽设置」等现有二级页模式。
- 主题模式、AMOLED、动态颜色、种子色全部从 `SettingsRepository` 读写，保持与旧 Flutter `user_setting.dart` 的键兼容。
- 动态颜色使用 MIUIX `ColorSchemeMode.MonetSystem/MonetLight/MonetDark`，关闭动态颜色时使用普通 `System/Light/Dark`。
- AMOLED 通过自定义 `darkColorScheme` 实现，将背景/表面相关颜色设为纯黑，仅在深色模式生效。
- 颜色选择器不引入第三方库，使用 MIUIX `SuperDialog` 与 Compose 基础组件实现。

## 验收条件

- [x] `RootComponent` 提供 `onThemeSettingClicked` 导航，`RootContent` 正确渲染 `ThemeSettingScreen`。
- [x] `SettingsScreen` 显示「主题设置」入口，点击进入新页面。
- [x] `ThemeSettingScreen` 正确展示主题模式、AMOLED、动态颜色、种子色设置。
- [x] 切换主题模式 / AMOLED / 动态颜色 / 种子色后，设置即时保存并反映到界面主题。
- [x] 关闭动态颜色时显示种子色选择入口，开启时隐藏。
- [x] 颜色选择器支持预设颜色与自定义 HEX 输入，非法输入给出提示。
- [x] Android + Desktop 双端编译通过。
- [x] M52 code review 完成，无 P0/P1 问题遗留。

## 不在范围

- 网络设置页、账号切换页等其他设置项（留待后续里程碑）。
- 跟随系统动态壁纸自动取色在 Desktop 平台的系统级支持（MIUIX Desktop 动态颜色行为由库决定）。
- 收藏标签管理、下载任务管理等功能。
