# M58 PRD：欢迎页设置

## 状态

进行中。

## 目标

原 Flutter 版设置页允许用户选择启动应用时的默认页面（首页、排行榜、速览、搜索、设置）。当前 MIUIX 版应用启动后固定进入首页标签。M58 补齐「欢迎页设置」二级页，将 `SettingsRepository.welcomePageType` 的读写以 MIUIX 组件重新呈现，并在应用启动时读取该设置进入对应页面，仅做 UI 层替换与启动页路由调整，不新增页面类型或改变一级页面结构。

## 必做（按最小任务量拆分）

1. **数据与路由层改造**
   - 修改 `RootComponent` 构造函数，接收 `settingsRepository: SettingsRepository`。
   - 在 `RootComponent` 初始化时根据 `settingsRepository.welcomePageType` 计算初始路由 `initialConfiguration`。
   - 修改 `App.kt` 中的 `rememberRootComponent`，将 `dependencies.settingsRepository` 传入 `RootComponent`。

2. **导航与路由**
   - 在 `RootComponent` 中新增 `onWelcomePageSettingClicked()` 导航方法与 `Config.WelcomePageSetting` 路由。
   - 在 `RootContent` 中处理 `Child.WelcomePageSetting`，渲染新的 `WelcomePageSettingScreen`。

3. **设置页入口调整**
   - 在 `SettingsScreen` 中新增「欢迎页」入口（`BasicComponent`）。
   - 点击后调用 `onWelcomePageSettingClick()` 跳转。

4. **欢迎页设置页**
   - 新建 `WelcomePageSettingScreen`：顶部标题栏含返回按钮，主体使用 `LazyColumn` 展示选项。
   - 提供「首页」「排行榜」「速览」「搜索」「设置」五个互斥单选项，分别对应 `welcomePageType` 的 `home`、`rank`、`quick_view`、`search`、`setting`。
   - 点击选项后弹出 `SuperDialog` 或在页面内直接展示单选列表，选择后立即写回 `SettingsRepository.welcomePageType` 并刷新高亮。

## 技术决策

- 独立 `WelcomePageSettingScreen` 作为二级页面，与现有二级页模式一致。
- 欢迎页类型沿用旧 Flutter 版的字符串编码（`home` / `rank` / `quick_view` / `search` / `setting`），通过 `SettingsRepository.welcomePageType` 读写。
- `RootComponent` 在构造时解析 `welcomePageType` 并映射为初始 `Config`：
  - `home` → `Config.Main(MainTab.Hello)`
  - `rank` → `Config.Main(MainTab.Ranking)`
  - `quick_view` → `Config.Main(MainTab.New)`
  - `search` → `Config.Main(MainTab.Search)`
  - `setting` → `Config.Settings`
  - 未知或空值 → 默认 `Config.Main(MainTab.Hello)`
- 使用 MIUIX `BasicComponent` + `SuperDialog` 实现单选交互，不引入第三方库。
- 仅做启动页路由调整，当前各页面内部逻辑不在本次范围。

## 验收条件

- [ ] `RootComponent` 接收 `settingsRepository` 并根据 `welcomePageType` 设置初始路由。
- [ ] `App.kt` 正确向 `RootComponent` 传递 `settingsRepository`。
- [ ] `RootComponent` 提供 `onWelcomePageSettingClicked` 导航，`RootContent` 正确渲染 `WelcomePageSettingScreen`。
- [ ] `SettingsScreen` 显示「欢迎页」入口，点击进入新页面。
- [ ] `WelcomePageSettingScreen` 正确展示当前欢迎页选项并提供全部五个互斥选项。
- [ ] 修改欢迎页选项后，设置即时保存到 `SettingsRepository.welcomePageType`。
- [ ] 重启应用后，进入页面与所选欢迎页一致。
- [ ] Android + Desktop 双端编译通过。
- [ ] M58 code review 完成，无 P0/P1 问题遗留。

## 垂直切片（Issue 拆分）

### Slice 1: 启动时读取欢迎页设置

**Blocked by**: None - can start immediately。

**用户故事覆盖**: 1、6（启动行为部分）。

**What to build**: 打通 `RootComponent` 读取 `SettingsRepository.welcomePageType` 并映射为初始路由的能力。

**Acceptance criteria**:
- [ ] `RootComponent` 构造函数新增 `settingsRepository` 参数。
- [ ] `App.kt` 的 `rememberRootComponent` 传入 `dependencies.settingsRepository`。
- [ ] `RootComponent` 根据 `welcomePageType` 的值正确设置 `initialConfiguration`。
- [ ] 未知或空值时回退到 `Config.Main(MainTab.Hello)`。

### Slice 2: 欢迎页设置页 UI 与导航

**Blocked by**: Slice 1。

**用户故事覆盖**: 2、3、4、5。

**What to build**: 新增欢迎页设置二级页，提供五个选项并持久化到 `SettingsRepository`。

**Acceptance criteria**:
- [ ] `RootComponent` / `RootContent` / `SettingsScreen` 已新增欢迎页入口与路由。
- [ ] `WelcomePageSettingScreen` 可正常进入与返回。
- [ ] 页面展示五个互斥单选项，选中后保存到 `SettingsRepository.welcomePageType` 并刷新高亮。

## 不在范围

- 语言设置、布局模式等其他未实现的设置项。
- 平台专属设置（保存格式、R18 分文件夹、显示模式、照片选择器等）。
- 欢迎页选项超过原 Flutter 版支持的五种类型。
- 国际化文案，使用中文硬编码。
