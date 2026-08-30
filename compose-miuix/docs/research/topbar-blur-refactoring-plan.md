# MIUIX 顶部毛玻璃模糊全链路重构规划与实施手册

- **目标**: 将当前项目中基于硬编码截断与均一高斯模糊的 `FrostedTopAppBar` 方案，彻底重构为 Xiaomi HyperOS / MIUIX 官方标准的渐进式纹理毛玻璃架构（`BlurredBar` + `ProgressiveBlur.Top` + `rememberBlurBackdrop`）。
- **执行原则**: 分步推进、双轨兼容、每步必审、全平台零编译报错与视觉无破坏性回归。

---

## 一、 重构背景与核心痛点

1. **生硬截断与手动补线**: 现有 `FrostedTopAppBar` 将模糊区域硬编码限制在 `statusBars + 56.dp`，在大标题模式下下方大片区域出现严重镂空，且在 56dp 处产生光学断层，甚至通过人工绘制分割线掩盖。
2. **通透度退化**: 为掩盖文字穿行时的黑边脏块，现有实现将磨砂层不透明度提高至 96%（`0.96f`），几乎退化为纯色矩形，失去毛玻璃质感。
3. **缺少采样底色保护与硬件熔断**: 裸 `rememberLayerBackdrop` 在透明边界采样时会引发 Skia 溢色黑边；在 Android < API 33 时缺少主动能力熔断机制。
4. **组件侵入性过高**: 自行封装了平行组件 `FrostedTopAppBar`，导致全工程 40+ 个界面产生耦合依赖，脱离官方原生 `TopAppBar` 的生命周期演进。

---

## 二、 目标架构设计

### 1. 核心基础设施 (`BlurredBar.kt`)
- `rememberBlurBackdrop`:
  - 检查 `isRuntimeShaderSupported()`，低于 API 33 或环境不支持时返回 `null`，自动降级实色；
  - 内部配置 `onDraw = { drawRect(surfaceColor); drawContent() }`，保证捕获图层拥有坚实底色。
- `BlurredBar`:
  - 纯组合容器，使用 `Modifier.matchParentSize()` 自适应包裹官方原生 `TopAppBar` 或 `SmallTopAppBar`；
  - 滚动联动：利用 `graphicsLayer { alpha = (-contentOffset / 48.dp).coerceIn(0f, 1f) }` 实现顶部完全透明、滚动 48dp 动态涌现；
  - 渐进模糊：配置 `ProgressiveBlur.Top.copy(curve = 2.2f)`，半径 10dp，表面色透明度恢复至自然的 30%（`0.3f`）。

### 2. 标准页面组合范式
```kotlin
Scaffold(
    topBar = {
        BlurredBar(
            backdrop = backdrop,
            blurEnabled = blurActive,
            scrollBehavior = scrollBehavior,
        ) {
            TopAppBar(
                title = title,
                color = if (blurActive) Color.Transparent else colorScheme.surface,
                scrollBehavior = scrollBehavior,
                navigationIcon = ...,
                actions = ...,
            )
        }
    }
) { innerPadding ->
    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        // 页面可滚动列表
    }
}
```

---

## 三、 重构实施步骤与检查核销标准

### 步骤 1：构建核心基础设施与工具函数
- [x] 1.1 在 `com.perol.pixez.shared.ui.components` 下创建 `BlurredBar.kt`。
- [x] 1.2 实现 `rememberBlurBackdrop(enableBlur: Boolean = true): LayerBackdrop?`，注入底色保护与 `isRuntimeShaderSupported` 熔断。
- [x] 1.3 实现 `BlurredBar`，集成自适应高度、滚动涌现 `alpha`、渐进式模糊与标准颜色混合。
- [x] 1.4 **步骤审查**：执行 Desktop/Android 编译检查，验证参数与接口准确性。

### 步骤 2：重构 `FrostedTopAppBar.kt` 为平滑双轨兼容层
- [x] 2.1 修改 `FrostedTopAppBar.kt`，将其内部底层实现直接切换为 `BlurredBar` + 原生 `TopAppBar`，移除 `56.dp` 硬编码、`0.96f` 遮盖与手工画线。
- [x] 2.2 同步改造 `FrostedSmallTopAppBar`。
- [x] 2.3 清理 `LiquidGlass.kt` 中未被引用的残留 `topAppBarBlur`。
- [x] 2.4 **步骤审查**：全工程零改动情况下编译通过，确认 40+ 个现有页面视觉缺陷即刻修复。

### 步骤 3：分批次迁移业务页面至标准官方组合范式
- [x] 3.1 **批次 1：核心主屏与导航页 (7 个页面)**
  - `HelloScreen.kt`, `RankingScreen.kt`, `NewScreen.kt`, `BoardScreen.kt`, `SpotlightScreen.kt`, `SpotlightDetailScreen.kt`, `HistoryScreen.kt`
  - 接入 `BlurredBar` + 原生 `TopAppBar`，使用 `rememberBlurBackdrop()`，移除旧 import。
  - **审查**：编译验证与语法检查。
- [x] 3.2 **批次 2：设置中心与系统级子页面 (18 个页面)**
  - `SettingsScreen.kt`, `ThemeSettingScreen.kt`, `DownloadSettingScreen.kt`, `NetworkSettingScreen.kt`, `QualitySettingScreen.kt`, `SaveSettingScreen.kt`, `LayoutSettingScreen.kt`, `LanguageSettingScreen.kt`, `InteractionSettingScreen.kt`, `FeedSettingScreen.kt`, `PrivacySettingScreen.kt`, `PlatformSettingScreen.kt`, `CopyTextSettingScreen.kt`, `CrossAdapterSettingScreen.kt`, `UserShowAISettingScreen.kt`, `WidgetRecommendSettingScreen.kt`, `WelcomePageSettingScreen.kt`, `DataExportScreen.kt`
  - **审查**：编译验证与语法检查。
- [x] 3.3 **批次 3：用户、插画详情与辅助功能页面 (17 个页面)**
  - `AboutScreen.kt`, `AccountEditScreen.kt`, `LoginScreen.kt`, `BookTagScreen.kt`, `DownloadHistoryScreen.kt`, `DownloadTaskScreen.kt`, `GuideScreen.kt`, `IllustSeriesScreen.kt`, `RecomUserScreen.kt`, `RelatedIllustsScreen.kt`, `ShieldScreen.kt`, `ThanksScreen.kt`, `UserFollowListScreen.kt`, `UserFollowerListScreen.kt`, `IllustDetailScreen.kt`, `SearchScreen.kt` 等剩余页面。
  - **审查**：编译验证与语法检查。

### 步骤 4：统一底栏视觉规范与废弃清理
- [x] 4.1 审查 `MainBottomBar.kt:89`，将其磨砂与颜色方案与 `BlurredBar` 保持体系化一致。
- [x] 4.2 确认全工程无 `FrostedTopAppBar` 引用后，标记 `@Deprecated` 或清理安全删除。
- [x] 4.3 **步骤审查**：全量符号检索，确保无悬空引用与多余 import。

### 步骤 5：文档同步、更新日志维护与多端最终验证
- [x] 5.1 检查并更新 `CHANGELOG.md` 顶部的 `## [未发布]` 区块（精简规范，不含编译考据）。
- [x] 5.2 执行三项严格的多平台构建验证：
  - `./gradlew :shared:compileKotlinDesktop`
  - `./gradlew :composeApp:compileKotlinDesktop`
  - `./gradlew :composeApp:compileDebugKotlinAndroid`
- [x] 5.3 自动执行语义化 Git 提交与推送。
