# PRD：修复 MIUIX 系统使用问题

> 父文档：本文件替代因仓库禁用 Issues 而无法创建的 GitHub Issue，作为本次修复的 PRD。

## Problem Statement

用户在 MIUI/HyperOS 设备上使用移植后的 MIUIX 版本时，遇到 6 类影响基础体验的稳定性与交互问题：

1. 系统界面疑似未默认启用 MIUIX 设计语言；
2. 图标显示异常（当前页面混用 Material 图标与 MIUIX 组件，风格不统一）；
3. 搜索功能点击后直接闪退；
4. 未登录状态下系统不会主动提示用户登录；
5. 侧滑返回手势会直接回到桌面，而非返回上一级界面；
6. 尚未接入更多 MIUIX 个性化功能。

需要在不新增原 Flutter 应用未支持功能的前提下，修复上述问题，确保 MIUIX 主题默认生效、图标风格统一、搜索稳定运行、登录提示机制正常工作、侧滑返回按预期返回上一级，并补充 MIUIX 个性化设置入口。

## Solution

- 统一将页面中的 Material 图标替换为 MIUIX 0.8.8 内置图标，确保与 MIUIX 组件风格一致；
- 在 `produceState` / 挂起代码块中使用 `suspendRunCatchingNonCancel` 替代 `runCatchingNonCancel`，正确捕获搜索与热门标签请求的异常，避免闪退；
- 在 `HelloScreen`、`NewScreen` 等依赖登录态的关键页面，未登录时主动弹出登录提示对话框（SuperDialog），并提供一键跳转登录；
- 修正 Decompose 导航返回逻辑：二级页面调用 `pop()` 返回上一级；一级主页面不拦截系统返回，交由系统处理退出或触发「再次返回退出」；
- 在 `ThemeSettingScreen` 中补充/确认 MIUIX 主题默认启用逻辑，并新增个性化设置项（如圆角、动画、壁纸模糊等 MIUIX 原生能力入口）。

## User Stories

1. 作为用户，我希望应用启动后默认使用 MIUIX 主题，以保证视觉风格与 MIUI/HyperOS 一致；
2. 作为用户，我希望所有图标风格统一，避免部分图标显示异常或风格割裂；
3. 作为用户，我希望点击搜索栏并输入关键词后能稳定展示结果，而不是直接闪退；
4. 作为用户，我希望在未登录状态下进入需要登录的页面时，能主动收到登录提示，而不是只看到空白或静态入口；
5. 作为用户，我希望在二级页面侧滑返回时能回到上一级页面，而不是直接回到桌面；
6. 作为用户，我希望在主题设置中能使用更多 MIUIX 个性化选项（如动态颜色、AMOLED、圆角、壁纸模糊等），以满足个人偏好；
7. 作为开发者，我希望所有异常处理都使用 `suspendRunCatchingNonCancel`，避免协程取消语义被破坏；
8. 作为开发者，我希望图标替换只涉及 import 与 imageVector 改动，不引入额外的第三方图标库。

## Implementation Decisions

- 图标层：MIUIX 0.8.8 以单包发布，内部已包含 `top.yukonga.miuix.kmp.icon` 下的图标集合，直接替换 `androidx.compose.material.icons.*` 引用，无需新增 artifact；
- 异常处理层：`produceState` 的 producer 为挂起代码块，必须使用 `suspendRunCatchingNonCancel`。统一扫描 `shared` 模块中在挂起上下文使用 `runCatchingNonCancel` 调用挂起函数的位置并替换；
- 登录提示层：在 `HelloScreen`/`NewScreen` 的 `LaunchedEffect` 中检测登录态，未登录时通过状态变量触发 `SuperDialog`，提供「去登录」与「暂不登录」两个操作；
- 导航返回层：修改 `RootComponent.onBack()`，先判断当前栈顶是否为 `Child.Main`（一级页面）。若是，不调用 `pop()`，直接返回 false/交由系统；若不是，执行 `navigation.pop()`。同时 `RootContent` 中如已使用 `Children` 的默认返回处理，需与组件方法对齐；
- 主题与个性化层：保留现有 `ThemeController` 构建逻辑，确保默认 `themeMode = 0`（跟随系统）、`useDynamicColor = true`；新增设置项复用 `BasicComponent` + `Switch`/`SuperDialog`，写入 `SettingsRepository`。

## Testing Decisions

- 编译验证：Android `:composeApp:assembleDebug` 与 Desktop `:shared:compileKotlinDesktop` 双端必须通过；
- 搜索稳定性：在搜索页输入关键词、点击热门标签、切换作品/画师标签，均应正常进入结果页或展示错误占位，不得闪退；
- 登录提示：清空账号数据后进入首页/最新页，应弹出登录提示对话框；
- 侧滑返回：从首页进入设置页再侧滑，应回到首页；从首页侧滑，应触发系统默认退出行为或「再次返回退出」；
- 图标检查：所有 TopAppBar、BottomBar、按钮图标均应渲染，无缺失或风格不一致。

## Out of Scope

- 升级 MIUIX 到 0.9.x（受 compileSdk 限制，不在本次修复范围）；
- 新增原 Flutter 应用未支持的全新功能（如全新页面、新的网络接口）；
- 修改 Pixiv 业务 API 协议或数据模型。

## Further Notes

- 本次修复拆分为 `miuix-system-fix-issues.md` 中的垂直切片 Issue；
- 修复完成后需同步检查 `compose-miuix/docs/` 中相关 PRD 文档是否过期。
