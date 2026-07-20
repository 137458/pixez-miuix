package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.perol.pixez.shared.data.repository.AccountRepository
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
import com.perol.pixez.shared.ui.screens.SpotlightScreen
import com.perol.pixez.shared.ui.screens.UserDetailScreen
import com.perol.pixez.shared.ui.screens.UserFollowListScreen
import com.perol.pixez.shared.ui.screens.UserFollowerListScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

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
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    // 主题模式：0 跟随系统，1 浅色，2 深色。从 SettingsRepository 读取并回写。
    var themeMode by rememberSaveable { mutableIntStateOf(settingsRepository.themeMode) }
    val colorSchemeMode = when (themeMode) {
        1 -> ColorSchemeMode.Light
        2 -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.System
    }
    // 使用 remember 即可；进程重建后 themeMode 会由 SettingsRepository 恢复（M4）。
    val themeController = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode)
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
                    )

                    is Child.UserDetail -> UserDetailScreen(
                        userId = instance.userId,
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        onFollowListClick = component::onUserFollowListClicked,
                        onFollowerListClick = component::onUserFollowerListClicked,
                        repository = userRepository,
                        bookmarkRepository = bookmarkRepository,
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
                    )

                    is Child.IllustSeries -> IllustSeriesScreen(
                        seriesId = instance.seriesId,
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        repository = illustRepository,
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
                        onLoginClick = component::onLoginClicked,
                        onDownloadHistoryClick = component::onDownloadHistoryClicked,
                        themeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            settingsRepository.themeMode = it
                        },
                        accountRepository = accountRepository,
                    )

                    is Child.Search -> SearchScreen(
                        onIllustClick = component::onIllustClicked,
                        onUserClick = component::onUserClicked,
                        repository = searchRepository,
                        settingsRepository = settingsRepository,
                        initialQuery = instance.query,
                    )

                    Child.DownloadHistory -> DownloadHistoryScreen(
                        onBack = component::onBack,
                        onIllustClick = component::onIllustClicked,
                        repository = downloadHistoryRepository,
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
        )

        RootComponent.MainTab.Search -> SearchScreen(
            onIllustClick = component::onIllustClicked,
            onUserClick = component::onUserClicked,
            repository = searchRepository,
            settingsRepository = settingsRepository,
        )

        RootComponent.MainTab.Ranking -> RankingScreen(
            onIllustClick = component::onIllustClicked,
            repository = illustRepository,
        )

        RootComponent.MainTab.New -> NewScreen(
            onIllustClick = component::onIllustClicked,
            onLoginClick = component::onLoginClicked,
            repository = illustRepository,
            accountRepository = accountRepository,
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
