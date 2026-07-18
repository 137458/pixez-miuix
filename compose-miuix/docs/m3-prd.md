# M3 PRD：核心页面 UI 与导航框架移植

## Problem Statement

M2 已完成核心数据模型与本地存储迁移。当前 `compose-miuix` 模块仅包含一个 `Hello MIUIX` 占位页面，尚未实现任何原 Flutter 应用的业务 UI。为了将应用全量移植为 MIUIX 页面，需要在 Compose Multiplatform 中重建原应用的页面层级、底部导航、常用布局组件，并至少覆盖用户日常浏览的核心路径。

## Solution

在 `compose-miuix/shared` 与 `compose-miuix/composeApp` 中建立 UI 层（`ui` 包），使用 MIUIX 组件实现：

1. 应用级导航框架（底部 5 标签 + 页面路由占位）。
2. 可复用的插画网格组件（瀑布流/等比例卡片）。
3. 核心一级页面：推荐/首页、搜索、排行榜、最新、Spotlight。
4. 核心二级页面：作品详情、用户详情。
5. 必要的辅助页面：设置、关于、登录占位。

M3 以 UI 骨架和静态/本地数据驱动为主；网络层（Ktor + OAuth + API 仓库）在 M4 接入，因此页面先用 mock 数据或本地数据库数据验证布局与交互。

## User Stories

1. 作为用户，我希望打开新应用后看到熟悉的底部 5 标签导航，以便快速切换到推荐、搜索、排行榜、最新和 Spotlight。
2. 作为用户，我希望首页以瀑布流/网格展示插画缩略图，点击可进入详情，以便浏览作品。
3. 作为用户，我希望搜索页面包含搜索栏、热门标签、历史记录，以便快速找到感兴趣的内容。
4. 作为用户，我希望作品详情页展示插画大图、标题、作者、标签、收藏数等信息，以便查看作品细节。
5. 作为用户，我希望用户详情页展示头像、昵称、关注按钮和作品列表，以便关注作者并浏览其作品。
6. 作为用户，我希望设置页面使用 MIUIX 列表项分组展示常用设置，以便调整主题、画质等选项。
7. 作为开发者，我希望导航和页面组件跨平台复用，避免为 Android/iOS/Desktop 分别实现。
8. 作为开发者，我希望 UI 层与数据层解耦，通过 ViewModel/StateHolder 持有状态，便于 M4 接入真实网络数据。

## Implementation Decisions

- **导航框架**：使用 Decompose 的 `StackNavigation` + `childStack` 实现页面栈，状态可序列化以支持进程重建；底部导航使用 MIUIX `NavigationBar`，5 个固定标签对应原 Flutter 应用的 `hello`、`search`、`ranking`、`newest`、`spotlight`。底部标签切换使用 `replaceCurrent`，避免栈深度累积。
- **页面状态管理**：使用 `rememberSaveable` + `remember` 管理 UI 状态，保持代码简洁；M3 阶段不引入 ViewModel，页面直接消费 `FakeData`；M4 再接入 Repository 与 KMP ViewModel。
- **插画网格**：基于 Compose Foundation 的 `LazyVerticalStaggeredGrid` 实现 `IllustStaggeredGrid` 组件，卡片长宽比按插画真实尺寸计算；默认 2 列，外层可通过 `columns` 参数扩展。
- **图片加载**：使用 Coil3 跨平台加载网络图片；M3 阶段使用 `FakeData` 生成 picsum 占位图 URL，M4 替换为真实 Pixiv 图片 URL。
- **主题与动态颜色**：使用 MIUIX `ThemeController` + `MiuixTheme`；在 `RootContent` 提升 `themeMode` 状态并传入 `SettingsScreen`，设置页提供浅色/深色/跟随系统切换入口，切换后即时生效。M4 接入 `SettingsRepository.themeMode` 持久化。
- **数据层边界**：UI 层在 M3 直接依赖 `FakeData` 验证布局；M4 引入 Repository 接口后，页面改为依赖 ViewModel/StateHolder，FakeData 仅保留在单元测试与预览中。
- **本地化**：M3 阶段为快速验证 UI，界面文本暂时硬编码中文；M4/M5 逐步迁移到 Compose Multiplatform `Res.string` 资源机制并补充英文。
- **未实现页面**：小说、历史记录、收藏夹、关注列表、任务管理等在 M3 仅保留导航占位或简单页面壳，M5/M6 逐步填充。
- **返回手势/物理键**：Android 使用 `BackHandler` 处理页面返回；Desktop/iOS 使用顶部导航返回按钮。

## Testing Decisions

- **编译测试**：每次新增页面后运行 `./gradlew :composeApp:compileDebugKotlinAndroid` 与 `:composeApp:compileKotlinDesktop`，确保跨平台编译通过。
- **UI 截图/布局测试**：对 `IllustWaterfallFlow` 和 `IllustDetailScreen` 使用 Compose Desktop 的 `runComposeUiTest` 或 Android 的 Compose Test 验证基本渲染。
- **导航测试**：验证底部标签切换后当前页面正确更新，返回栈行为符合预期。
- **无障碍与主题切换**：手动验证深色模式切换无闪屏，文字对比度正常。

## Out of Scope

- 网络请求层（Ktor Client、OAuth2、Token 刷新、API 错误重试）属于 M4。
- 图片保存、分享、下载任务管理属于 M5。
- 小说阅读器、Live2D、Spotlight 详情、评论页等复杂二级页面属于 M5/M6。
- IAP、小组件、DeepLink、推送等系统能力属于后续里程碑。
- 不删除旧 Flutter 代码；新旧工程并行存在。

## Further Notes

- MIUIX 组件库当前使用版本 0.8.8；所有组件使用官方文档示例中的稳定 API，避免使用实验性 API。
- 保持代码绝对简洁：每个 Composable 函数控制在合理长度，超过 50 行的逻辑必须附带中文节点注释。
- 页面命名尽量与原 Flutter 页面一一对应，方便后续对照移植。
