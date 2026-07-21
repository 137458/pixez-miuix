package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.navigation.RootComponent.Child
import com.perol.pixez.shared.ui.screens.AboutScreen
import com.perol.pixez.shared.ui.screens.CommentsScreen
import com.perol.pixez.shared.ui.screens.DownloadHistoryScreen
import com.perol.pixez.shared.ui.screens.HelloScreen
import com.perol.pixez.shared.ui.screens.IllustDetailScreen
import com.perol.pixez.shared.ui.screens.LoginScreen
import com.perol.pixez.shared.ui.screens.NewScreen
import com.perol.pixez.shared.ui.screens.IllustSeriesScreen
import com.perol.pixez.shared.ui.screens.RankingScreen
import com.perol.pixez.shared.ui.screens.RecomUserScreen
import com.perol.pixez.shared.ui.screens.RelatedIllustsScreen
import com.perol.pixez.shared.ui.screens.SearchScreen
import com.perol.pixez.shared.ui.screens.SettingsScreen
import com.perol.pixez.shared.ui.screens.ShieldScreen
import com.perol.pixez.shared.ui.screens.SpotlightScreen
import com.perol.pixez.shared.ui.screens.ThemeSettingScreen
import com.perol.pixez.shared.ui.screens.ThemeSettingScreen_DEFAULT_SEED_COLOR
import com.perol.pixez.shared.ui.screens.UserShowAISettingScreen
import com.perol.pixez.shared.ui.screens.UserDetailScreen
import com.perol.pixez.shared.ui.screens.UserFollowListScreen
import com.perol.pixez.shared.ui.screens.UserFollowerListScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
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
    modifier: Modifier = Modifier,
) {
    // 主题状态：从 SettingsRepository 读取，由设置页回写。
    var themeMode by rememberSaveable { mutableIntStateOf(settingsRepository.themeMode) }
    var isAmoled by rememberSaveable { mutableStateOf(settingsRepository.isAmoled) }
    var useDynamicColor by rememberSaveable { mutableStateOf(settingsRepository.useDynamicColor) }
    var seedColor by rememberSaveable {
        mutableIntStateOf(settingsRepository.seedColor ?: ThemeSettingScreen_DEFAULT_SEED_COLOR)
    }

    // 进程重建后上述状态会由 SettingsRepository 恢复（M4）。
    val themeController = remember(themeMode, isAmoled, useDynamicColor, seedColor) {
        buildThemeController(
            themeMode = themeMode,
            isAmoled = isAmoled,
            useDynamicColor = useDynamicColor,
            seedColor = seedColor,
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
                        accountRepository = accountRepository,
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

                    Child.About -> AboutScreen(
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
 */
private fun buildThemeController(
    themeMode: Int,
    isAmoled: Boolean,
    useDynamicColor: Boolean,
    seedColor: Int,
): ThemeController {
    // 统一使用 Monet 模式，使种子色在非动态颜色模式下也能生效；
    // 动态颜色开启时 keyColor 传 null，让 Monet 使用系统壁纸颜色。
    val colorSchemeMode = when (themeMode) {
        1 -> ColorSchemeMode.MonetLight
        2 -> ColorSchemeMode.MonetDark
        else -> ColorSchemeMode.MonetSystem
    }
    val keyColor = if (useDynamicColor) null else Color(seedColor)

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
        )
    } else {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
        )
    }
}
