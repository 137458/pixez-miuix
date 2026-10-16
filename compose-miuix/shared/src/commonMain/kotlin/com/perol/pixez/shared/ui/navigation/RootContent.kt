package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.perol.pixez.shared.platform.rememberScreenCornerRadius
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.theme.LocalAppExtraColors
import com.perol.pixez.shared.ui.theme.darkAppExtraColors
import com.perol.pixez.shared.ui.theme.lightAppExtraColors
import com.perol.pixez.shared.ui.navigation.animation.LocalSharedBoundsRegistry
import com.perol.pixez.shared.ui.navigation.animation.NavTransitionLog
import com.perol.pixez.shared.ui.navigation.animation.PageContainerGeometry
import com.perol.pixez.shared.ui.navigation.animation.SharedBoundsRegistry
import com.perol.pixez.shared.ui.navigation.animation.miuixCardExpandPredictiveBackAnimatable
import com.perol.pixez.shared.ui.navigation.animation.miuixSeekableStackAnimation
import com.perol.pixez.shared.ui.navigation.animation.miuixPredictiveBackAnimation

import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalBoardRepository
import com.perol.pixez.shared.ui.LocalBookmarkRepository
import com.perol.pixez.shared.ui.LocalDownloadHistoryRepository
import com.perol.pixez.shared.ui.LocalDownloadRepository
import com.perol.pixez.shared.ui.LocalHistoryRepository
import com.perol.pixez.shared.ui.LocalIllustRepository
import com.perol.pixez.shared.ui.LocalMuteRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.LocalPixivisionRepository
import com.perol.pixez.shared.ui.LocalSauceNaoRepository
import com.perol.pixez.shared.ui.LocalSearchRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUpdateRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.screens.DEFAULT_SEED_COLOR
import androidx.compose.runtime.CompositionLocalProvider
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.utils.openSafeUrl
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 根 UI：在 Decompose 页面栈外层包裹主题，并在一级页面底部显示导航栏。
 *
 * 本函数只负责：主题/语言等状态声明、更新检查副作用、Scaffold 容器骨架，
 * 以及对 [Config] 的分发编排（具体页面渲染见同包 RootChildScreens.kt 中的 render* 函数）。
 */
@OptIn(com.arkivanov.decompose.ExperimentalDecomposeApi::class)
@Composable
fun RootContent(
    component: RootComponent,
    registry: RepositoryRegistry,
    modifier: Modifier = Modifier,
) {
    val settingsRepository = registry.settingsRepository
    // 主题状态：每次重组直接从 SettingsRepository 读取当前值，
    // 监听 changeVersion 响应式自增，确保 ThemeSettingScreen 修改后即时全局重绘。
    val changeVersion = settingsRepository.changeVersion.collectAsState().value
    val themeMode = settingsRepository.themeMode
    val isAmoled = settingsRepository.isAmoled
    val useDynamicColor = settingsRepository.useDynamicColor
    val seedColor = settingsRepository.seedColor ?: DEFAULT_SEED_COLOR
    val paletteStyleIndex = settingsRepository.miuixPaletteStyle
    val useSpec2025 = settingsRepository.miuixUseSpec2025

    // 进程重建后上述状态会由 SettingsRepository 恢复（M4）。
    val themeController = remember(
        themeMode,
        isAmoled,
        useDynamicColor,
        seedColor,
        paletteStyleIndex,
        useSpec2025,
        changeVersion,
    ) {
        buildThemeController(
            themeMode = themeMode,
            isAmoled = isAmoled,
            useDynamicColor = useDynamicColor,
            seedColor = seedColor,
            paletteStyleIndex = paletteStyleIndex,
            useSpec2025 = useSpec2025,
        )
    }

    val stack by component.stack.subscribeAsState()
    val active = stack.active.instance

    val floatingBackdrop = rememberBlurBackdrop()
    // 作品卡片几何信息源：列表卡片登记矩形，二级页面转场据此播放「卡片展开/收回」动画。
    val sharedBounds = remember { SharedBoundsRegistry() }
    // SideEffect：成功应用后再同步路由锚点；被丢弃的重组不会污染不可回滚的注册表字段
    SideEffect {
        NavTransitionLog.d("root") { "syncRoute ${(stack.active.configuration as? Config.IllustDetail)?.illustId}" }
        sharedBounds.syncActiveRouteIllustId((stack.active.configuration as? Config.IllustDetail)?.illustId)
    }
    val bottomBarVisible = remember { mutableStateOf(true) }
    val currentLanguageNum = settingsRepository.languageNum
    val strings = remember(currentLanguageNum, changeVersion) {
        AppStrings.fromLanguageNum(currentLanguageNum)
    }

    var appReleaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showAppUpdateDialog by remember { mutableStateOf(false) }
    // 根级 Toast：承载更新链接等外链打开失败提示（页面内提示由各页面自持）。
    var rootToastMessage by remember { mutableStateOf<ToastData?>(null) }

    LaunchedEffect(Unit) {
        if (settingsRepository.autoCheckUpdate) {
            registry.updateRepository.fetchLatestReleaseInfo().onSuccess { info ->
                if (info.isNew && info.versionName != settingsRepository.ignoreUpdateVersion) {
                    appReleaseInfo = info
                    showAppUpdateDialog = true
                }
            }
        }
    }

    // I-26：明暗判定由 themeMode 解析（与 buildThemeController 的 ColorSchemeMode 一致，AMOLED/动态色不影响）
    val resolvedDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val appExtraColors = if (resolvedDark) darkAppExtraColors else lightAppExtraColors

    MiuixTheme(controller = themeController) {
        CompositionLocalProvider(
            LocalSettingsRepository provides settingsRepository,
            LocalIllustRepository provides registry.illustRepository,
            LocalPixivisionRepository provides registry.pixivisionRepository,
            LocalSearchRepository provides registry.searchRepository,
            LocalUserRepository provides registry.userRepository,
            LocalAccountRepository provides registry.accountRepository,
            LocalBookmarkRepository provides registry.bookmarkRepository,
            LocalHistoryRepository provides registry.historyRepository,
            LocalDownloadRepository provides registry.downloadRepository,
            LocalDownloadHistoryRepository provides registry.downloadHistoryRepository,
            LocalBanRepository provides registry.banRepository,
            LocalBoardRepository provides registry.boardRepository,
            LocalNovelHistoryRepository provides registry.novelHistoryRepository,
            LocalMuteRepository provides registry.muteRepository,
            LocalNovelRepository provides registry.novelRepository,
            LocalUpdateRepository provides registry.updateRepository,
            LocalSauceNaoRepository provides registry.sauceNaoRepository,
            LocalBottomBarVisibility provides bottomBarVisible,
            LocalStrings provides strings,
            LocalSharedBoundsRegistry provides sharedBounds,
            LocalAppExtraColors provides appExtraColors,
            LocalTabGesturePosition provides remember { mutableStateOf<Float?>(null) },
        ) {
            val updateInfo = appReleaseInfo
            if (showAppUpdateDialog && updateInfo != null) {
                UpdateDialog(
                    show = showAppUpdateDialog,
                    releaseInfo = updateInfo,
                    onDismiss = { showAppUpdateDialog = false },
                    onUpdate = { url ->
                        showAppUpdateDialog = false
                        openSafeUrl(url, strings, onError = { message ->
                            rootToastMessage = ToastData(message, ToastType.Error)
                        })
                    },
                    onIgnore = { ver ->
                        settingsRepository.ignoreUpdateVersion = ver
                        showAppUpdateDialog = false
                    },
                )
            }

            BoxWithConstraints(
                modifier = modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface),
            ) {
                val isWideScreen = maxWidth >= AppConstants.Layout.WIDE_SCREEN_MIN_WIDTH_DP.dp
                val useFloatingBottomBar = settingsRepository.useFloatingBottomBar
                val showNavigationRailInMain = isWideScreen && !useFloatingBottomBar
                val activeTab by component.selectedTab.collectAsState()
                val density = LocalDensity.current
                val screenCornerRadius = rememberScreenCornerRadius()
                val containerWidthPx = with(density) { maxWidth.toPx() }
                // 页面容器几何：矩形由布局回填，宽度与圆角由本层测量得出。
                var containerGeometry by remember {
                    mutableStateOf(PageContainerGeometry(widthPx = containerWidthPx, cornerRadius = screenCornerRadius))
                }
                // 宽度与圆角随环境变化时同步到几何快照；矩形保持布局回填的最新值。
                LaunchedEffect(containerWidthPx, screenCornerRadius) {
                    containerGeometry = containerGeometry.copy(
                        widthPx = containerWidthPx,
                        cornerRadius = screenCornerRadius,
                    )
                }

                // 转场动画器按容器几何记忆化：容器尺寸变化（旋转/分栏切换）时才重建，
                // 避免每次重组都新建 StackAnimation 而击穿 Decompose 内部的按页动画器缓存。
                // 几何以即时读取源传入：Decompose 会按 child 缓存动画器实例，
                // 快照捕获会让冷启动首帧未回填的 Rect.Zero 被过期缓存固化。
                // 各尺寸设备统一卡片展开/收回转场（双向视口裁切支持任意横纵比落点），
                // 两层按同一锚点解析路径，无锚点时统一回退 Miuix 默认侧滑。
                val stackAnimation = remember(sharedBounds) {
                    miuixSeekableStackAnimation<Config, Any>(
                        registry = sharedBounds,
                        containerGeometry = { containerGeometry },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface)
                        .onGloballyPositioned { coordinates ->
                            val newBounds = coordinates.boundsInWindow()
                            if (newBounds != containerGeometry.bounds) {
                                NavTransitionLog.d("root") { "containerGeometry bounds=$newBounds" }
                                containerGeometry = containerGeometry.withBounds(newBounds)
                            }
                        },
                ) {
                    NavTransitionLog.d("root") { "stack=[${stack.items.joinToString { it.configuration.toString() }}] recompose" }
                    Children(
                        stack = component.stack,
                        modifier = Modifier.fillMaxSize(),
                        animation = miuixPredictiveBackAnimation(
                            backHandler = component.backHandler,
                            fallbackAnimation = stackAnimation,
                            selector = { initialBackEvent, exitChild, _ ->
                                // 预测性返回手势来源页直接取自 exitChild 的 configuration，
                                // 避免依赖 child 实例类型（中-6b 起详情页实例为子组件，路由参数只在配置上）。
                                // 手势预览与常规转场共用同一锚点解析：能落点则按卡片收回，否则回退侧滑。
                                miuixCardExpandPredictiveBackAnimatable(
                                    initialBackEvent = initialBackEvent,
                                    registry = sharedBounds,
                                    illustId = (exitChild.configuration as? Config.IllustDetail)?.illustId,
                                    containerGeometry = { containerGeometry },
                                )
                            },
                            onBack = { component.onBack() },
                            // 应用内拖拽线性关页：仅作品详情页启用（平台开关见 rememberDragBackGestureEnabled）。
                            // 实例判定：详情页 child 实例为中-6b 试点引入的子组件。
                            dragBackEligible = { it is IllustDetailComponent },
                        ),
                    ) { child ->
                        RootChildContent(
                            instance = child.instance,
                            component = component,
                            activeTab = activeTab,
                            isWideScreen = isWideScreen,
                            useFloatingBottomBar = useFloatingBottomBar,
                            showNavigationRailInMain = showNavigationRailInMain,
                            bottomBarVisible = bottomBarVisible,
                            floatingBackdrop = floatingBackdrop,
                        )
                    }
                }

                // 根级 Toast：承载更新链接等外链打开失败提示（无页面级反馈通道的场景）。
                ToastMessage(
                    toast = rootToastMessage,
                    onDismiss = { rootToastMessage = null },
                )
            }
        }
    }
}

/**
 * Decompose 栈顶页面的分发：按 child 实例类型路由到对应 render* 页面。
 * 实例类型（中-6b 试点起）：作品详情页为 [IllustDetailComponent]，其余页面为 [Config]。
 * 一级页面（Main）见 [MainChildSection]，其余二级页面渲染见同包 RootChildScreens.kt。
 */
@Composable
private fun RootChildContent(
    instance: Any,
    component: RootComponent,
    activeTab: RootComponent.MainTab,
    isWideScreen: Boolean,
    useFloatingBottomBar: Boolean,
    showNavigationRailInMain: Boolean,
    bottomBarVisible: MutableState<Boolean>,
    floatingBackdrop: LayerBackdrop?,
) {
    when (instance) {
        // 中-6b 试点：作品详情页 child 实例为子组件，渲染参数与跳转回调经组件透传。
        is IllustDetailComponent -> renderIllustDetail(
            component = instance,
        )

        is Config.Main -> MainChildSection(
            tab = instance.tab,
            feedRestore = component.mainFeedRestore,
            component = component,
            activeTab = activeTab,
            showNavigationRailInMain = showNavigationRailInMain,
            bottomBarVisible = bottomBarVisible,
            isWideScreen = isWideScreen,
                            useFloatingBottomBar = useFloatingBottomBar,
                            floatingBackdrop = floatingBackdrop,
                        )

        is Config.UserDetail -> renderUserDetail(
            instance = instance,
            component = component,
        )

        Config.Login -> renderLogin(
            component = component,
        )

        is Config.Comments -> renderComments(
            instance = instance,
            component = component,
        )

        is Config.RelatedIllusts -> renderRelatedIllusts(
            instance = instance,
            component = component,
        )

        is Config.IllustSeries -> renderIllustSeries(
            instance = instance,
            component = component,
        )

        is Config.UserFollowList -> renderUserFollowList(
            instance = instance,
            component = component,
        )

        is Config.UserFollowerList -> renderUserFollowerList(
            instance = instance,
            component = component,
        )

        Config.RecomUserList -> renderRecomUserList(
            component = component,
        )

        Config.Settings -> renderSettingsPage(
            component = component,
        )

        is Config.Search -> renderSearch(
            instance = instance,
            component = component,
        )

        Config.DownloadHistory -> renderDownloadHistory(
            component = component,
        )

        Config.Shield -> renderShield(
            component = component,
        )

        is Config.AISetting -> renderAISetting(
            instance = instance,
            component = component,
        )

        Config.ThemeSetting -> renderThemeSetting(
            onBack = component::onBack,
        )

        Config.NetworkSetting -> renderNetworkSetting(
            onBack = component::onBack,
        )

        Config.DownloadSetting -> renderDownloadSetting(
            onBack = component::onBack,
        )

        Config.LayoutSetting -> renderLayoutSetting(
            onBack = component::onBack,
        )

        Config.LanguageSetting -> renderLanguageSetting(
            onBack = component::onBack,
        )

        Config.WidgetRecommendSetting -> renderWidgetRecommendSetting(
            onBack = component::onBack,
        )

        Config.InteractionSetting -> renderInteractionSetting(
            onBack = component::onBack,
        )

        Config.UpdateSetting -> renderUpdateSetting(
            onBack = component::onBack,
        )

        Config.AccountEdit -> renderAccountEdit(
            component = component,
        )

        Config.History -> renderHistory(
            component = component,
        )

        Config.DownloadTask -> renderDownloadTask(
            component = component,
        )

        Config.DataExport -> renderDataExport(
            component = component,
        )

        Config.Board -> renderBoard(
            component = component,
        )

        Config.QualitySetting -> renderQualitySetting(
            onBack = component::onBack,
        )

        Config.CopyTextSetting -> renderCopyTextSetting(
            onBack = component::onBack,
        )

        Config.WelcomePageSetting -> renderWelcomePageSetting(
            onBack = component::onBack,
        )

        Config.About -> renderAbout(
            component = component,
        )

        Config.BookTag -> renderBookTag(
            component = component,
        )

        Config.Thanks -> renderThanks(
            component = component,
        )

        is Config.SpotlightDetail -> renderSpotlightDetail(
            instance = instance,
            component = component,
        )

        Config.Guide -> renderGuide(
            component = component,
        )

        Config.AccountManage -> renderAccountManage(
            component = component,
        )

        Config.Novel -> renderNovel(
            component = component,
        )

        is Config.NovelViewer -> renderNovelViewer(
            instance = instance,
            component = component,
        )

        // 分发主体改为 Any 后失去密封 Config 的编译期穷举校验，以此兜底：
        // 新增 Config 未接线时在分发处即时抛错，而非静默白屏。
        else -> error("RootChildContent 未分发的路由实例: $instance")
    }
}

/**
 * 一级页面（Main）区块：可选侧边导航栏 + MainContent + 底部导航栏。
 *
 * 中-6a 起 Child 镜像已删除：tab 取自栈顶 [Config.Main]，跨页恢复容器（高-5）经
 * [component.mainFeedRestore] 读取（由 RootComponent.createChild 在 Main child 创建时接线）。
 */
@Composable
private fun MainChildSection(
    tab: RootComponent.MainTab,
    feedRestore: MainFeedRestore,
    component: RootComponent,
    activeTab: RootComponent.MainTab,
    showNavigationRailInMain: Boolean,
    bottomBarVisible: MutableState<Boolean>,
    isWideScreen: Boolean,
    useFloatingBottomBar: Boolean,
    floatingBackdrop: LayerBackdrop?,
) {
    // 读取 changeVersion 以订阅设置变更，保证 Main 子树在设置修改后重组刷新。
    // 中-9：设置仓储经 LocalSettingsRepository 读取（RootContent 根部 provide）。
    val settingsRepository = LocalSettingsRepository.current
    val mainChangeVersion = settingsRepository.changeVersion.collectAsState().value
    // 底栏内容边距只在 Main 子树内提供且转场全程不随 active 翻转，
    // 消除进出详情页时列表因边距变化发生的重排；二级页面无底栏，
    // 回落为 CompositionLocal 默认值 16.dp。
    CompositionLocalProvider(
        LocalBottomBarContentPadding provides if (bottomBarVisible.value) 100.dp else 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface),
        ) {
            if (showNavigationRailInMain) {
                MainNavigationRail(
                    activeTab = activeTab,
                    onTabSelected = component::onMainTabSelected,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(floatingBackdrop),
                ) {
                    MainContent(
                        initialTab = tab,
                        component = component,
                        feedRestore = feedRestore,
                    )
                }
                if (bottomBarVisible.value && (!isWideScreen || useFloatingBottomBar)) {
                    MainBottomBar(
                        activeTab = activeTab,
                        onTabSelected = component::onMainTabSelected,
                        isFloating = useFloatingBottomBar,
                        backdrop = floatingBackdrop,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/**
 * 根据当前主题偏好构建 [ThemeController]。
 *
 * @param themeMode 0 跟随系统，1 浅色，2 深色。
 * @param isAmoled 是否开启 AMOLED 纯黑深色模式。
 * @param useDynamicColor 是否使用 Monet 动态颜色。
 * @param seedColor 动态颜色/非动态颜色下的种子色。
 * @param paletteStyleIndex 调色板风格索引，对应 [ThemePaletteStyle] 枚举顺序。
 * @param useSpec2025 是否使用 Material 2025 色彩规范。
 */
private fun buildThemeController(
    themeMode: Int,
    isAmoled: Boolean,
    useDynamicColor: Boolean,
    seedColor: Int,
    paletteStyleIndex: Int,
    useSpec2025: Boolean,
): ThemeController {
    val colorSchemeMode = if (useDynamicColor) {
        when (themeMode) {
            1 -> ColorSchemeMode.MonetLight
            2 -> ColorSchemeMode.MonetDark
            else -> ColorSchemeMode.MonetSystem
        }
    } else {
        when (themeMode) {
            1 -> ColorSchemeMode.Light
            2 -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }
    }
    val keyColor = if (useDynamicColor) null else Color(seedColor)

    // 将持久化的调色板风格索引映射为 MIUIX 枚举值，越界时回退到默认 TonalSpot。
    val paletteStyle = ThemePaletteStyle.entries.getOrNull(paletteStyleIndex)
        ?: ThemePaletteStyle.TonalSpot
    val colorSpec = if (useSpec2025) ThemeColorSpec.Spec2025 else ThemeColorSpec.Spec2021

    // 自定义浅色颜色方案：背景为浅灰色（#F6F7F9），卡片/容器为纯白（Color.White），显式指定高对比度深色文本前景色。
    val lightColors = lightColorScheme(
        background = Color(0xFFF6F7F9),
        surface = Color(0xFFF6F7F9),
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color(0xFFF0F1F4),
        surfaceContainerHighest = Color(0xFFE5E7EB),
        onBackground = Color(0xFF191919),
        onSurface = Color(0xFF191919),
        onSurfaceContainer = Color(0xFF191919),
        onSurfaceVariantSummary = Color(0xFF666666),
        onSurfaceSecondary = Color(0xFF888888),
    )

    // AMOLED 模式下自定义深色颜色方案，将背景与表面颜色设为纯黑，并确保前景色为高对比度浅色。
    val darkColors = if (isAmoled) {
        darkColorScheme(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color(0xFF1E1E1E),
            surfaceContainerHighest = Color(0xFF2C2C2C),
            onBackground = Color(0xFFF3F4F6),
            onSurface = Color(0xFFF3F4F6),
            onSurfaceContainer = Color(0xFFF3F4F6),
            onSurfaceVariantSummary = Color(0xFF9CA3AF),
            onSurfaceSecondary = Color(0xFF9CA3AF),
        )
    } else {
        null
    }

    return if (darkColors != null) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            lightColors = lightColors,
            darkColors = darkColors,
            colorSpec = colorSpec,
            paletteStyle = paletteStyle,
        )
    } else {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            lightColors = lightColors,
            colorSpec = colorSpec,
            paletteStyle = paletteStyle,
        )
    }
}
