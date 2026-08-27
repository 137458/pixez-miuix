package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.perol.pixez.shared.ui.navigation.animation.miuixSlidePredictiveBackAnimatable
import com.perol.pixez.shared.ui.navigation.animation.miuixSlideStackAnimation

import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.navigation.RootComponent.Child
import io.ktor.client.HttpClient
import com.perol.pixez.shared.ui.screens.AboutScreen
import com.perol.pixez.shared.ui.screens.AccountEditScreen
import com.perol.pixez.shared.ui.screens.BoardScreen
import com.perol.pixez.shared.ui.screens.CommentsScreen
import com.perol.pixez.shared.ui.screens.DataExportScreen
import com.perol.pixez.shared.ui.screens.DownloadHistoryScreen
import com.perol.pixez.shared.ui.screens.DownloadSettingScreen
import com.perol.pixez.shared.ui.screens.DownloadTaskScreen
import com.perol.pixez.shared.ui.screens.HelloScreen
import com.perol.pixez.shared.ui.screens.HistoryScreen
import com.perol.pixez.shared.ui.screens.IllustDetailScreen
import com.perol.pixez.shared.ui.screens.LoginScreen
import com.perol.pixez.shared.ui.screens.NetworkSettingScreen
import com.perol.pixez.shared.ui.screens.NewScreen
import com.perol.pixez.shared.ui.screens.PlatformSettingScreen
import com.perol.pixez.shared.ui.screens.SaveSettingScreen
import com.perol.pixez.shared.ui.screens.CrossAdapterSettingScreen
import com.perol.pixez.shared.ui.screens.GuideScreen
import com.perol.pixez.shared.ui.screens.LayoutSettingScreen
import com.perol.pixez.shared.ui.screens.LanguageSettingScreen
import com.perol.pixez.shared.ui.screens.WidgetRecommendSettingScreen
import com.perol.pixez.shared.ui.screens.InteractionSettingScreen
import com.perol.pixez.shared.ui.screens.FeedSettingScreen
import com.perol.pixez.shared.ui.screens.QualitySettingScreen
import com.perol.pixez.shared.ui.screens.CopyTextSettingScreen
import com.perol.pixez.shared.ui.screens.PrivacySettingScreen
import com.perol.pixez.shared.ui.screens.IllustSeriesScreen
import com.perol.pixez.shared.ui.screens.RankingScreen
import com.perol.pixez.shared.ui.screens.RecomUserScreen
import com.perol.pixez.shared.ui.screens.RelatedIllustsScreen
import com.perol.pixez.shared.ui.screens.SearchScreen
import com.perol.pixez.shared.ui.screens.SettingsScreen
import com.perol.pixez.shared.ui.screens.ShieldScreen
import com.perol.pixez.shared.ui.screens.SpotlightDetailScreen
import com.perol.pixez.shared.ui.screens.SpotlightScreen
import com.perol.pixez.shared.ui.screens.ThemeSettingScreen
import com.perol.pixez.shared.ui.screens.DEFAULT_SEED_COLOR
import com.perol.pixez.shared.ui.screens.UpdateSettingScreen
import com.perol.pixez.shared.ui.screens.UserShowAISettingScreen
import com.perol.pixez.shared.ui.screens.UserDetailScreen
import com.perol.pixez.shared.ui.screens.UserFollowListScreen
import com.perol.pixez.shared.ui.screens.UserFollowerListScreen
import com.perol.pixez.shared.ui.screens.WelcomePageSettingScreen
import com.perol.pixez.shared.ui.screens.ThanksScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.components.UpdateDialog
import com.perol.pixez.shared.ui.screens.ReleaseInfo
import com.perol.pixez.shared.ui.screens.fetchLatestReleaseInfo
import com.perol.pixez.shared.ui.screens.BookTagScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 根 UI：在 Decompose 页面栈外层包裹主题，并在一级页面底部显示导航栏。
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

    val backdrop = rememberLayerBackdrop()
    val bottomBarVisible = remember { mutableStateOf(true) }
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
            com.perol.pixez.shared.ui.i18n.LocalStrings provides strings,
        ) {
            if (showAppUpdateDialog && appReleaseInfo != null) {
                UpdateDialog(
                    show = showAppUpdateDialog,
                    releaseInfo = appReleaseInfo!!,
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

            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp
                    val isMainTab = active is Child.Main
                    val useFloatingBottomBar = settingsRepository.useFloatingBottomBar
                    val showNavigationRail = isWideScreen && isMainTab && !useFloatingBottomBar
                    val showBottomBar = isMainTab && bottomBarVisible.value && (!isWideScreen || useFloatingBottomBar)
                    val density = LocalDensity.current
                    val containerWidthPx = with(density) {
                        val availableWidth = if (showNavigationRail) (maxWidth - 80.dp).coerceAtLeast(0.dp) else maxWidth
                        availableWidth.toPx()
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // 在平板/桌面宽屏且关闭悬浮底栏模式下，一级主页面在左侧展示 MIUIX 官方 NavigationRail 侧边栏
                        if (showNavigationRail) {
                            MainNavigationRail(
                                activeTab = active.tab,
                                onTabSelected = component::onMainTabSelected,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            Children(
                                stack = component.stack,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(backdrop),
                                animation = predictiveBackAnimation(
                                    backHandler = component.backHandler,
                                    fallbackAnimation = miuixSlideStackAnimation(),
                                    selector = { initialBackEvent, _, _ ->
                                        miuixSlidePredictiveBackAnimatable(
                                            initialBackEvent = initialBackEvent,
                                            containerWidthPx = containerWidthPx,
                                        )
                                    },
                                    onBack = { component.onBack() },
                                ),
                            ) { child ->

                    when (val instance = child.instance) {
                        is Child.Main -> MainContent(
                            tab = instance.tab,
                            component = component,
                            illustRepository = illustRepository,
                            searchRepository = searchRepository,
                            userRepository = userRepository,
                        accountRepository = accountRepository,
                        banRepository = banRepository,
                        settingsRepository = settingsRepository,
                    )

                    is Child.IllustDetail -> IllustDetailScreen(
                        illustId = instance.illustId,
                        onBack = component::onBack,
                        onUserClick = component::onUserClicked,
                        onCommentsClick = component::onCommentsClicked,
                        onRelatedIllustsClick = component::onRelatedIllustsClicked,
                        onIllustSeriesClick = component::onIllustSeriesClicked,
                        onTagClick = component::onSearchClicked,
                        repository = illustRepository,
                        bookmarkRepository = bookmarkRepository,
                        downloadRepository = downloadRepository,
                        banRepository = banRepository,
                        historyRepository = historyRepository,
                    )

                    is Child.UserDetail -> UserDetailScreen(
                        userId = instance.userId,
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        onFollowListClick = component::onUserFollowListClicked,
                        onFollowerListClick = component::onUserFollowerListClicked,
                        repository = userRepository,
                        bookmarkRepository = bookmarkRepository,
                        banRepository = banRepository,
                        settingsRepository = settingsRepository,
                    )

                    Child.Login -> LoginScreen(
                        onBack = component::onBack,
                        onLoginSuccess = component::onLoginSuccess,
                        accountRepository = accountRepository,
                    )


                    is Child.Comments -> CommentsScreen(
                        illustId = instance.illustId,
                        onBack = component::onBack,
                        onUserClick = component::onUserClicked,
                        repository = illustRepository,
                        accountRepository = accountRepository,
                    )

                    is Child.RelatedIllusts -> RelatedIllustsScreen(
                        illustId = instance.illustId,
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        repository = illustRepository,
                        banRepository = banRepository,
                        settingsRepository = settingsRepository,
                    )

                    is Child.IllustSeries -> IllustSeriesScreen(
                        seriesId = instance.seriesId,
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        repository = illustRepository,
                        banRepository = banRepository,
                        settingsRepository = settingsRepository,
                    )

                    is Child.UserFollowList -> UserFollowListScreen(
                        userId = instance.userId,
                        onBack = component::onBack,
                        onUserClick = component::onUserClicked,
                        repository = userRepository,
                    )

                    is Child.UserFollowerList -> UserFollowerListScreen(
                        userId = instance.userId,
                        onBack = component::onBack,
                        onUserClick = component::onUserClicked,
                        repository = userRepository,
                    )

                    Child.RecomUserList -> RecomUserScreen(
                        onBack = component::onBack,
                        onUserClick = component::onUserClicked,
                        repository = userRepository,
                    )

                    Child.Settings -> SettingsScreen(
                        onBack = component::onBack,
                        onAboutClick = component::onAboutClicked,
                        onShieldClick = component::onShieldClicked,
                        onLoginClick = component::onLoginClicked,
                        onDownloadHistoryClick = component::onDownloadHistoryClicked,
                        onThemeSettingClick = component::onThemeSettingClicked,
                        onNetworkSettingClick = component::onNetworkSettingClicked,
                        onDownloadSettingClick = component::onDownloadSettingClicked,
                        onSaveSettingClick = component::onSaveSettingClicked,
                        onCrossAdapterSettingClick = component::onCrossAdapterSettingClicked,
                        onLayoutSettingClick = component::onLayoutSettingClicked,
                        onLanguageSettingClick = component::onLanguageSettingClicked,
                        onWidgetRecommendSettingClick = component::onWidgetRecommendSettingClicked,
                        onInteractionSettingClick = component::onInteractionSettingClicked,
                        onFeedSettingClick = component::onFeedSettingClicked,
                        onQualitySettingClick = component::onQualitySettingClicked,
                        onCopyTextSettingClick = component::onCopyTextSettingClicked,
                        onPrivacySettingClick = component::onPrivacySettingClicked,
                        onWelcomePageSettingClick = component::onWelcomePageSettingClicked,
                        onPlatformSettingClick = component::onPlatformSettingClicked,
                        onBookTagClick = component::onBookTagClicked,
                        onUpdateSettingClick = component::onUpdateSettingClicked,
                        onAccountEditClick = component::onAccountEditClicked,
                        onHistoryClick = component::onHistoryClicked,
                        onDownloadTaskClick = component::onDownloadTaskClicked,
                        onDataExportClick = component::onDataExportClicked,
                        onBoardClick = component::onBoardClicked,
                        onGuideClick = component::onGuideClicked,
                        accountRepository = accountRepository,
                        boardRepository = boardRepository,
                    )

                    is Child.Search -> SearchScreen(
                        onIllustClick = component::onIllustClicked,
                        onUserClick = component::onUserClicked,
                        repository = searchRepository,
                        settingsRepository = settingsRepository,
                        banRepository = banRepository,
                        initialQuery = instance.query,
                    )

                    Child.DownloadHistory -> DownloadHistoryScreen(
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        repository = downloadHistoryRepository,
                    )

                    Child.Shield -> ShieldScreen(
                        onBack = component::onBack,
                        onAISettingClick = component::onAISettingClicked,
                        settingsRepository = settingsRepository,
                        banRepository = banRepository,
                        userRepository = userRepository,
                    )

                    is Child.AISetting -> UserShowAISettingScreen(
                        showAI = instance.showAI,
                        onBack = component::onBack,
                        userRepository = userRepository,
                    )

                    Child.ThemeSetting -> ThemeSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.NetworkSetting -> NetworkSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.DownloadSetting -> DownloadSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.SaveSetting -> SaveSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.CrossAdapterSetting -> CrossAdapterSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.LayoutSetting -> LayoutSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.LanguageSetting -> LanguageSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.WidgetRecommendSetting -> WidgetRecommendSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.InteractionSetting -> InteractionSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.FeedSetting -> FeedSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.UpdateSetting -> UpdateSettingScreen(
                        settingsRepository = settingsRepository,
                        updateCheckClient = updateCheckClient,
                        onBack = component::onBack,
                    )

                    Child.AccountEdit -> AccountEditScreen(
                        onBack = component::onBack,
                        accountRepository = accountRepository,
                    )

                    Child.History -> HistoryScreen(
                        onBack = component::onBack,
                        // 历史记录使用 Long 保存作品 ID 以避免数据库溢出，
                        // 导航层仍使用 Int，在此处做类型转换。
                        onIllustClick = { component.onIllustClicked(it.toInt()) },
                    )

                    Child.DownloadTask -> DownloadTaskScreen(
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        downloadRepository = downloadRepository,
                        downloadHistoryRepository = downloadHistoryRepository,
                    )

                    Child.DataExport -> DataExportScreen(
                        onBack = component::onBack,
                        settingsRepository = settingsRepository,
                        historyRepository = historyRepository,
                        novelHistoryRepository = novelHistoryRepository,
                        muteRepository = muteRepository,
                    )

                    Child.Board -> BoardScreen(
                        onBack = component::onBack,
                        boardRepository = boardRepository,
                    )

                    Child.QualitySetting -> QualitySettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.CopyTextSetting -> CopyTextSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.PrivacySetting -> PrivacySettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.WelcomePageSetting -> WelcomePageSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.PlatformSetting -> PlatformSettingScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                    )

                    Child.About -> AboutScreen(
                        onBack = component::onBack,
                        onThanksClick = component::onThanksClicked,
                        onUpdateClick = component::onUpdateSettingClicked,
                    )

                    Child.BookTag -> BookTagScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                        onTagSearch = component::onSearchClicked,
                    )

                    Child.Thanks -> ThanksScreen(
                        onBack = component::onBack,
                    )

                    is Child.SpotlightDetail -> SpotlightDetailScreen(
                        article = instance.article,
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        onUserClick = component::onUserClicked,
                        onArticleClick = component::onSpotlightArticleClicked,
                        repository = illustRepository,
                    )

                    Child.Guide -> GuideScreen(
                        settingsRepository = settingsRepository,
                        accountRepository = accountRepository,
                        onLoginClick = component::onLoginClicked,
                        onFinish = component::onGuideFinished,
                    )
                }
            }

                            // 仅在符合条件的展示场景下渲染底部导航栏
                            val _changeVersion = settingsRepository.changeVersion
                            if (showBottomBar && active is Child.Main) {
                                MainBottomBar(
                                    activeTab = active.tab,
                                    onTabSelected = component::onMainTabSelected,
                                    isFloating = useFloatingBottomBar,
                                    backdrop = backdrop,
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
 * 根据当前底部标签渲染对应一级页面。
 */
@Composable
private fun MainContent(
    tab: RootComponent.MainTab,
    component: RootComponent,
    illustRepository: IllustRepository,
    searchRepository: SearchRepository,
    userRepository: UserRepository,
    accountRepository: AccountRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    when (tab) {
        RootComponent.MainTab.Hello -> HelloScreen(
            onIllustClick = component::onIllustClicked,
            onSettingsClick = component::onSettingsClicked,
            onLoginClick = component::onLoginClicked,
            onRecomUserClick = component::onRecomUserListClicked,
            repository = illustRepository,
            accountRepository = accountRepository,
            banRepository = banRepository,
            settingsRepository = settingsRepository,
            reselectFlow = remember(component) {
                kotlinx.coroutines.flow.flow {
                    component.tabReselectEvents.collect { tab ->
                        if (tab == RootComponent.MainTab.Hello) emit(Unit)
                    }
                }
            },
        )

        RootComponent.MainTab.Search -> SearchScreen(
            onIllustClick = component::onIllustClicked,
            onUserClick = component::onUserClicked,
            repository = searchRepository,
            settingsRepository = settingsRepository,
            banRepository = banRepository,
        )

        RootComponent.MainTab.Ranking -> RankingScreen(
            onIllustClick = component::onIllustClicked,
            repository = illustRepository,
            banRepository = banRepository,
            settingsRepository = settingsRepository,
        )

        RootComponent.MainTab.New -> NewScreen(
            onIllustClick = component::onIllustClicked,
            onLoginClick = component::onLoginClicked,
            repository = illustRepository,
            accountRepository = accountRepository,
            banRepository = banRepository,
            settingsRepository = settingsRepository,
            reselectFlow = remember(component) {
                kotlinx.coroutines.flow.flow {
                    component.tabReselectEvents.collect { tab ->
                        if (tab == RootComponent.MainTab.New) emit(Unit)
                    }
                }
            },
        )

        RootComponent.MainTab.Spotlight -> SpotlightScreen(
            repository = illustRepository,
            onArticleClick = component::onSpotlightArticleClicked,
        )
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
