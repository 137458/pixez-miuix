# 拆分 Issue：修复 MIUIX 系统使用问题

> 因仓库禁用 GitHub Issues，以下切片以本地文档形式跟踪。父文档见 `miuix-system-fix-prd.md`。

## Issue #1：统一替换 Material 图标为 MIUIX 内置图标

### Parent

`miuix-system-fix-prd.md`

### What to build

将 `shared` 模块中所有使用 `androidx.compose.material.icons.*` 的位置替换为 MIUIX 0.8.8 内置图标 `top.yukonga.miuix.kmp.icon.*`，确保图标风格与 MIUIX 组件一致，解决图标显示异常问题。

### Acceptance criteria

- [ ] 扫描并替换 `SearchScreen.kt`、`HelloScreen.kt`、`ThemeSettingScreen.kt`、`MainBottomBar.kt` 等文件中的 Material 图标引用；
- [ ] 不引入新的图标依赖 artifact；
- [ ] Android 与 Desktop 双端编译通过；
- [ ] 底部导航栏、TopAppBar、按钮等位置的图标正常显示。

### Blocked by

None - can start immediately.

## Issue #2：修复搜索功能闪退

### Parent

`miuix-system-fix-prd.md`

### What to build

`SearchScreen.kt` 中 `produceState` 的 producer 是挂起代码块，当前使用 `runCatchingNonCancel` 包裹 `repository.searchIllust()`、`repository.searchUser()`、`repository.getTrendTags()` 等挂起函数，存在异常捕获不正确或运行时崩溃风险。统一替换为 `suspendRunCatchingNonCancel`，并确保搜索流程稳定。

### Acceptance criteria

- [ ] `SearchScreen.kt` 中所有在挂起上下文调用挂起函数的地方使用 `suspendRunCatchingNonCancel`；
- [ ] 搜索页点击搜索栏、输入关键词、切换作品/画师标签后不闪退；
- [ ] 网络异常时展示错误占位并提供重试；
- [ ] Android 与 Desktop 双端编译通过。

### Blocked by

None - can start immediately.

## Issue #3：未登录状态主动提示登录

### Parent

`miuix-system-fix-prd.md`

### What to build

在 `HelloScreen.kt` 和 `NewScreen.kt` 中，检测到未登录状态时主动弹出 MIUIX `SuperDialog` 提示用户登录，提供「去登录」跳转登录页和「暂不登录」关闭对话框两个选项。

### Acceptance criteria

- [ ] `HelloScreen.kt` 未登录时弹出登录提示对话框；
- [ ] `NewScreen.kt` 未登录时弹出登录提示对话框；
- [ ] 点击「去登录」正确跳转到登录页；
- [ ] 点击「暂不登录」关闭对话框并保留当前静态登录入口作为兜底；
- [ ] Android 与 Desktop 双端编译通过。

### Blocked by

None - can start immediately.

## Issue #4：修复侧滑返回直接回到桌面

### Parent

`miuix-system-fix-prd.md`

### What to build

修正 `RootComponent.onBack()` 的返回逻辑：当前在一级主页面（`Child.Main`）时不主动 `pop()`，将返回事件交由系统处理（退出应用或触发「再次返回退出」）；在二级页面时执行 `navigation.pop()` 返回上一级。同时检查 `RootContent` 中 `Children` 的返回处理是否与组件方法一致。

### Acceptance criteria

- [ ] 从二级页面（如设置页）侧滑返回回到一级页面；
- [ ] 从一级主页面侧滑不直接回到桌面，而是交由系统默认行为；
- [ ] 若开启「再次返回退出」，一级页面连续两次返回才退出应用；
- [ ] Android 与 Desktop 双端编译通过。

### Blocked by

None - can start immediately.

## Issue #5：确认 MIUIX 主题默认启用并扩展个性化设置

### Parent

`miuix-system-fix-prd.md`

### What to build

确认 `RootContent.kt` 中 `ThemeController` 默认使用 MIUIX 主题（跟随系统、开启动态颜色）。在 `ThemeSettingScreen.kt` 中新增 MIUIX 个性化设置项（如圆角风格、组件动画、壁纸模糊等），并通过 `SettingsRepository` 持久化。

### Acceptance criteria

- [ ] 应用启动后默认使用 MIUIX 主题（跟随系统、动态颜色开启）；
- [ ] `ThemeSettingScreen.kt` 新增至少 1-2 项 MIUIX 个性化设置入口；
- [ ] 设置项变更后即时生效并持久化；
- [ ] Android 与 Desktop 双端编译通过。

### Blocked by

None - can start immediately.
