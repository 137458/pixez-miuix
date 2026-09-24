package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.perol.pixez.shared.platform.rememberScreenCornerRadius
import com.perol.pixez.shared.ui.navigation.animation.miuixSlidePredictiveBackAnimatable
import com.perol.pixez.shared.ui.navigation.animation.miuixSlideStackAnimation

import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BoardRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.MuteRepository
import com.perol.pixez.shared.data.repository.NovelHistoryRepository
import com.perol.pixez.shared.data.repository.NovelRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.navigation.RootComponent.Child
import io.ktor.client.HttpClient
import com.perol.pixez.shared.ui.screens.DEFAULT_SEED_COLOR
import androidx.compose.runtime.CompositionLocalProvider
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.screens.ReleaseInfo
import com.perol.pixez.shared.ui.screens.fetchLatestReleaseInfo
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
 * 以及对 [Child] 的分发编排（具体页面渲染见同包 RootChildScreens.kt 中的 render* 函数）。
 */
@OptIn(com.arkivanov.decompose.ExperimentalDecomposeApi::class)
@Composable
fun RootContent(
    component: RootComponent,
    illustRepository: IllustRepository,
    searchRepository: SearchRepository,
    userRepository: UserRepository,
    accountRepository: AccountRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    downloadHistoryRepository: DownloadHistoryRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    boardRepository: BoardRepository,
    historyRepository: HistoryRepository,
    novelHistoryRepository: NovelHistoryRepository,
    muteRepository: MuteRepository,
    updateCheckClient: HttpClient,
    novelRepository: NovelRepository? = null,
    modifier: Modifier = Modifier,
) {
    // 主题状态：每次重组直接从 SettingsRepository 读取当前值，
    // 监听 changeVersion 响应式自增，确保 ThemeSettingScreen 修改后即时全局重绘。
    val changeVersion = settingsRepository.changeVersion
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
    val bottomBarVisible = remember { mutableStateOf(true) }
    val mainContentBottomPadding = if (active is Child.Main && bottomBarVisible.value) 100.dp else 16.dp
    val currentLanguageNum = settingsRepository.languageNum
    val strings = remember(currentLanguageNum, settingsRepository.changeVersion) {
        com.perol.pixez.shared.ui.i18n.AppStrings.fromLanguageNum(currentLanguageNum)
    }

    var appReleaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showAppUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (settingsRepository.autoCheckUpdate) {
            fetchLatestReleaseInfo().onSuccess { info ->
                if (info.isNew && info.versionName != settingsRepository.ignoreUpdateVersion) {
                    appReleaseInfo = info
                    showAppUpdateDialog = true
                }
            }
        }
    }

    MiuixTheme(controller = themeController) {
        CompositionLocalProvider(
            LocalSettingsRepository provides settingsRepository,
            LocalBottomBarVisibility provides bottomBarVisible,
            LocalBottomBarContentPadding provides mainContentBottomPadding,
            com.perol.pixez.shared.ui.i18n.LocalStrings provides strings,
        ) {
            val updateInfo = appReleaseInfo
            if (showAppUpdateDialog && updateInfo != null) {
                UpdateDialog(
                    show = showAppUpdateDialog,
                    releaseInfo = updateInfo,
                    onDismiss = { showAppUpdateDialog = false },
                    onUpdate = { url ->
                        showAppUpdateDialog = false
                        openBrowser(url)
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
                val isWideScreen = maxWidth >= 600.dp
                val isMainTab = active is Child.Main
                val useFloatingBottomBar = settingsRepository.useFloatingBottomBar
                val showNavigationRail = isWideScreen && isMainTab && !useFloatingBottomBar
                val showBottomBar = isMainTab && bottomBarVisible.value && (!isWideScreen || useFloatingBottomBar)
                val activeTab by component.selectedTab.collectAsState()
                val density = LocalDensity.current
                val screenCornerRadius = rememberScreenCornerRadius()
                val containerWidthPx = with(density) {
                    val availableWidth = if (showNavigationRail) (maxWidth - 80.dp).coerceAtLeast(0.dp) else maxWidth
                    availableWidth.toPx()
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface),
                ) {
                    // 在平板/桌面宽屏且关闭悬浮底栏模式下，一级主页面在左侧展示 MIUIX 官方 NavigationRail 侧边栏
                    if (showNavigationRail) {
                        MainNavigationRail(
                            activeTab = activeTab,
                            onTabSelected = component::onMainTabSelected,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MiuixTheme.colorScheme.surface),
                    ) {
                        Children(
                            stack = component.stack,
                            modifier = Modifier
                                .fillMaxSize()
                                .blurBackdropSource(floatingBackdrop),
                            animation = predictiveBackAnimation(
                                backHandler = component.backHandler,
                                fallbackAnimation = miuixSlideStackAnimation(),
                                selector = { initialBackEvent, _, _ ->
                                    miuixSlidePredictiveBackAnimatable(
                                        initialBackEvent = initialBackEvent,
                                        containerWidthPx = containerWidthPx,
                                        deviceCornerRadius = screenCornerRadius,
                                    )
                                },
                                onBack = { component.onBack() },
                            ),
                        ) { child ->
                            when (val instance = child.instance) {
                                is Child.Main -> MainContent(
                                    initialTab = instance.tab,
                                    component = component,
                                    illustRepository = illustRepository,
                                    searchRepository = searchRepository,
                                    userRepository = userRepository,
                                    accountRepository = accountRepository,
                                    banRepository = banRepository,
                                    settingsRepository = settingsRepository,
                                )

                                is Child.IllustDetail -> renderIllustDetail(
                                    instance = instance,
                                    component = component,
                                    illustRepository = illustRepository,
                                    bookmarkRepository = bookmarkRepository,
                                    downloadRepository = downloadRepository,
                                    banRepository = banRepository,
                                    historyRepository = historyRepository,
                                )

                                is Child.UserDetail -> renderUserDetail(
                                    instance = instance,
                                    component = component,
                                    userRepository = userRepository,
                                    bookmarkRepository = bookmarkRepository,
                                    banRepository = banRepository,
                                    settingsRepository = settingsRepository,
                                    accountRepository = accountRepository,
                                )

                                Child.Login -> renderLogin(
                                    component = component,
                                    accountRepository = accountRepository,
                                )

                                is Child.Comments -> renderComments(
                                    instance = instance,
                                    component = component,
                                    illustRepository = illustRepository,
                                    accountRepository = accountRepository,
                                )

                                is Child.RelatedIllusts -> renderRelatedIllusts(
                                    instance = instance,
                                    component = component,
                                    illustRepository = illustRepository,
                                    banRepository = banRepository,
                                    settingsRepository = settingsRepository,
                                )

                                is Child.IllustSeries -> renderIllustSeries(
                                    instance = instance,
                                    component = component,
                                    illustRepository = illustRepository,
                                    banRepository = banRepository,
                                    settingsRepository = settingsRepository,
                                )

                                is Child.UserFollowList -> renderUserFollowList(
                                    instance = instance,
                                    component = component,
                                    userRepository = userRepository,
                                )

                                is Child.UserFollowerList -> renderUserFollowerList(
                                    instance = instance,
                                    component = component,
                                    userRepository = userRepository,
                                )

                                Child.RecomUserList -> renderRecomUserList(
                                    component = component,
                                    userRepository = userRepository,
                                )

                                Child.Settings -> renderSettingsPage(
                                    component = component,
                                    accountRepository = accountRepository,
                                    boardRepository = boardRepository,
                                )

                                is Child.Search -> renderSearch(
                                    instance = instance,
                                    component = component,
                                    searchRepository = searchRepository,
                                    settingsRepository = settingsRepository,
                                    banRepository = banRepository,
                                )

                                Child.DownloadHistory -> renderDownloadHistory(
                                    component = component,
                                    downloadHistoryRepository = downloadHistoryRepository,
                                )

                                Child.Shield -> renderShield(
                                    component = component,
                                    settingsRepository = settingsRepository,
                                    banRepository = banRepository,
                                    userRepository = userRepository,
                                )

                                is Child.AISetting -> renderAISetting(
                                    instance = instance,
                                    component = component,
                                    userRepository = userRepository,
                                )

                                Child.ThemeSetting -> renderThemeSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.NetworkSetting -> renderNetworkSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.DownloadSetting -> renderDownloadSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.LayoutSetting -> renderLayoutSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.LanguageSetting -> renderLanguageSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.WidgetRecommendSetting -> renderWidgetRecommendSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.InteractionSetting -> renderInteractionSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.UpdateSetting -> renderUpdateSetting(
                                    settingsRepository = settingsRepository,
                                    updateCheckClient = updateCheckClient,
                                    onBack = component::onBack,
                                )

                                Child.AccountEdit -> renderAccountEdit(
                                    component = component,
                                    accountRepository = accountRepository,
                                )

                                Child.History -> renderHistory(
                                    component = component,
                                )

                                Child.DownloadTask -> renderDownloadTask(
                                    component = component,
                                    downloadRepository = downloadRepository,
                                    downloadHistoryRepository = downloadHistoryRepository,
                                )

                                Child.DataExport -> renderDataExport(
                                    component = component,
                                    settingsRepository = settingsRepository,
                                    historyRepository = historyRepository,
                                    novelHistoryRepository = novelHistoryRepository,
                                    muteRepository = muteRepository,
                                )

                                Child.Board -> renderBoard(
                                    component = component,
                                    boardRepository = boardRepository,
                                )

                                Child.QualitySetting -> renderQualitySetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.CopyTextSetting -> renderCopyTextSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.WelcomePageSetting -> renderWelcomePageSetting(
                                    settingsRepository = settingsRepository,
                                    onBack = component::onBack,
                                )

                                Child.About -> renderAbout(
                                    component = component,
                                )

                                Child.BookTag -> renderBookTag(
                                    component = component,
                                    settingsRepository = settingsRepository,
                                )

                                Child.Thanks -> renderThanks(
                                    component = component,
                                )

                                is Child.SpotlightDetail -> renderSpotlightDetail(
                                    instance = instance,
                                    component = component,
                                    illustRepository = illustRepository,
                                )

                                Child.Guide -> renderGuide(
                                    component = component,
                                    settingsRepository = settingsRepository,
                                    accountRepository = accountRepository,
                                )

                                Child.AccountManage -> renderAccountManage(
                                    component = component,
                                    accountRepository = accountRepository,
                                )

                                Child.Novel -> renderNovel(
                                    component = component,
                                    novelRepository = novelRepository,
                                )

                                is Child.NovelViewer -> renderNovelViewer(
                                    instance = instance,
                                    component = component,
                                    novelRepository = novelRepository,
                                )
                            }
                        }

                        // 仅在符合条件的展示场景下渲染底部导航栏
                        val _changeVersion = settingsRepository.changeVersion
                        if (showBottomBar && active is Child.Main) {
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