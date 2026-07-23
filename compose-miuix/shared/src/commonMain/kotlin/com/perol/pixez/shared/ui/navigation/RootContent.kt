package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
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
import com.perol.pixez.shared.ui.screens.BookTagScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme

/**
 * 根 UI：在 Decompose 页面栈外层包裹主题，并在一级页面底部显示导航栏。
 */
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
    // 确保 ThemeSettingScreen 回写主题偏好后返回即可生效。
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

    MiuixTheme(controller = themeController) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                // 仅在一级主页面显示底部导航栏。
                val stack by component.stack.subscribeAsState()
                val active = stack.active.instance
                if (active is Child.Main) {
                    MainBottomBar(
                        activeTab = active.tab,
                        onTabSelected = component::onMainTabSelected,
                    )
                }
            },
        ) { paddingValues ->
            Children(
                stack = component.stack,
                modifier = Modifier.padding(paddingValues),
                animation = stackAnimation(slide()),
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
                        onLoginSuccess = {
                            component.onBack()
                            // 登录成功后重置首页，触发 HelloScreen 重新加载并刷新登录态。
                            component.onMainTabSelected(RootComponent.MainTab.Hello)
                        },
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
                    )

                    Child.BookTag -> BookTagScreen(
                        settingsRepository = settingsRepository,
                        onBack = component::onBack,
                        onTagSearch = component::onSearchClicked,
                    )

                    Child.Thanks -> ThanksScreen(
                        onBack = component::onBack,
                    )
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
        )

        RootComponent.MainTab.Spotlight -> SpotlightScreen(
            onUserClick = component::onUserClicked,
            onRecomUserListClick = component::onRecomUserListClicked,
            repository = illustRepository,
            userRepository = userRepository,
            accountRepository = accountRepository,
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
    // 统一使用 Monet 模式，使种子色在非动态颜色模式下也能生效；
    // 动态颜色开启时 keyColor 传 null，让 Monet 使用系统壁纸颜色。
    val colorSchemeMode = when (themeMode) {
        1 -> ColorSchemeMode.MonetLight
        2 -> ColorSchemeMode.MonetDark
        else -> ColorSchemeMode.MonetSystem
    }
    val keyColor = if (useDynamicColor) null else Color(seedColor)

    // 将持久化的调色板风格索引映射为 MIUIX 枚举值，越界时回退到默认 TonalSpot。
    val paletteStyle = ThemePaletteStyle.entries.getOrNull(paletteStyleIndex)
        ?: ThemePaletteStyle.TonalSpot
    val colorSpec = if (useSpec2025) ThemeColorSpec.Spec2025 else ThemeColorSpec.Spec2021

    // AMOLED 模式下自定义深色颜色方案，将背景与表面颜色设为纯黑。
    val darkColors = if (isAmoled) {
        darkColorScheme(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
        )
    } else {
        null
    }

    return if (darkColors != null) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            darkColors = darkColors,
            colorSpec = colorSpec,
            paletteStyle = paletteStyle,
        )
    } else {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            colorSpec = colorSpec,
            paletteStyle = paletteStyle,
        )
    }
}
