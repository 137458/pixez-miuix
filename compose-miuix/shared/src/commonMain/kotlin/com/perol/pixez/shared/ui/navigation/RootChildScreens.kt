package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.perol.pixez.shared.ui.screens.AboutScreen
import com.perol.pixez.shared.ui.screens.AccountEditScreen
import com.perol.pixez.shared.ui.screens.AccountManageScreen
import com.perol.pixez.shared.ui.screens.BoardScreen
import com.perol.pixez.shared.ui.screens.BookTagScreen
import com.perol.pixez.shared.ui.screens.CommentsScreen
import com.perol.pixez.shared.ui.screens.CopyTextSettingScreen
import com.perol.pixez.shared.ui.screens.DataExportScreen
import com.perol.pixez.shared.ui.screens.DownloadHistoryScreen
import com.perol.pixez.shared.ui.screens.DownloadSettingScreen
import com.perol.pixez.shared.ui.screens.DownloadTaskScreen
import com.perol.pixez.shared.ui.screens.GuideScreen
import com.perol.pixez.shared.ui.screens.HelloScreen
import com.perol.pixez.shared.ui.screens.HistoryScreen
import com.perol.pixez.shared.ui.screens.IllustDetailScreen
import com.perol.pixez.shared.ui.screens.IllustSeriesScreen
import com.perol.pixez.shared.ui.screens.InteractionSettingScreen
import com.perol.pixez.shared.ui.screens.LanguageSettingScreen
import com.perol.pixez.shared.ui.screens.LayoutSettingScreen
import com.perol.pixez.shared.ui.screens.LoginScreen
import com.perol.pixez.shared.ui.screens.NetworkSettingScreen
import com.perol.pixez.shared.ui.screens.NewScreen
import com.perol.pixez.shared.ui.screens.NovelScreen
import com.perol.pixez.shared.ui.screens.NovelViewerScreen
import com.perol.pixez.shared.ui.screens.QualitySettingScreen
import com.perol.pixez.shared.ui.screens.RankingScreen
import com.perol.pixez.shared.ui.screens.RecomUserScreen
import com.perol.pixez.shared.ui.screens.RelatedIllustsScreen
import com.perol.pixez.shared.ui.screens.SearchScreen
import com.perol.pixez.shared.ui.screens.SettingsScreen
import com.perol.pixez.shared.ui.screens.ShieldScreen
import com.perol.pixez.shared.ui.screens.SpotlightDetailScreen
import com.perol.pixez.shared.ui.screens.SpotlightScreen
import com.perol.pixez.shared.ui.screens.ThanksScreen
import com.perol.pixez.shared.ui.screens.ThemeSettingScreen
import com.perol.pixez.shared.ui.screens.UpdateSettingScreen
import com.perol.pixez.shared.ui.screens.UserDetailScreen
import com.perol.pixez.shared.ui.screens.UserFollowListScreen
import com.perol.pixez.shared.ui.screens.UserFollowerListScreen
import com.perol.pixez.shared.ui.screens.UserShowAISettingScreen
import com.perol.pixez.shared.ui.screens.WelcomePageSettingScreen
import com.perol.pixez.shared.ui.screens.WidgetRecommendSettingScreen
import io.ktor.client.HttpClient

/**
 * [RootContent] 中各导航子页面（[Child]）的渲染函数集合。
 *
 * 这些函数由原 `RootContent.kt` 中巨型 `when (child.instance)` 的各个分支原样抽取而来，
 * 每个函数负责将某一类子页面映射到对应的 Screen 调用（参数与抽取前完全一致）。
 * 分发编排仍保留在 [RootContent] 内，此处仅承载「渲染哪一个页面」的具体实现。
 */

/**
 * 使用 HorizontalPager 管理并渲染 5 个一级主页面。
 *
 * 参考 localsend-miuix 实现：
 * 1. 5 个主页面在水平方向物理相邻排布，切换标签时通过 Pager 平滑滚动，
 *    彻底避免了栈替换导致的白边与重新挂载问题。
 * 2. 配合 beyondViewportPageCount = 4 预加载与保持状态，保留各标签的滚动位置与网络缓存。
 * 3. 双向同步：底栏点击驱动 Pager 滚动，手势滑动 Pager 同步底栏选中高亮。
 */
@Composable
internal fun MainContent(
    initialTab: RootComponent.MainTab,
    component: RootComponent,
    illustRepository: IllustRepository,
    searchRepository: SearchRepository,
    userRepository: UserRepository,
    accountRepository: AccountRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    val initialIndex = remember { RootComponent.MAIN_TAB_ORDER.indexOf(initialTab).coerceAtLeast(0) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { RootComponent.MAIN_TAB_ORDER.size },
    )
    val selectedTab by component.selectedTab.collectAsState()

    // 响应外部或底部导航栏点击驱动 Pager 平滑滚动
    LaunchedEffect(selectedTab) {
        val targetIndex = RootComponent.MAIN_TAB_ORDER.indexOf(selectedTab)
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            if (kotlin.math.abs(pagerState.currentPage - targetIndex) > 1) {
                pagerState.scrollToPage(targetIndex)
            } else {
                pagerState.animateScrollToPage(targetIndex)
            }
        }
    }

    // 响应用户手势滑动 Pager 同步组件状态
    LaunchedEffect(pagerState.currentPage) {
        val currentTab = RootComponent.MAIN_TAB_ORDER.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (component.selectedTab.value != currentTab) {
            component.onPageSwiped(currentTab)
        }
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 4,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (RootComponent.MAIN_TAB_ORDER[page]) {
            RootComponent.MainTab.Hello -> HelloScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
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
                onUserClick = component::onUserClicked,
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
}

@Composable
internal fun renderIllustDetail(
    instance: Child.IllustDetail,
    component: RootComponent,
    illustRepository: IllustRepository,
    bookmarkRepository: BookmarkRepository,
    downloadRepository: DownloadRepository,
    banRepository: BanRepository,
    historyRepository: HistoryRepository,
) {
    IllustDetailScreen(
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
        onIllustClick = component::onIllustClicked,
        onNovelClick = component::onNovelClicked,
    )
}

@Composable
internal fun renderUserDetail(
    instance: Child.UserDetail,
    component: RootComponent,
    userRepository: UserRepository,
    bookmarkRepository: BookmarkRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
    accountRepository: AccountRepository,
) {
    UserDetailScreen(
        userId = instance.userId,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        onFollowListClick = component::onUserFollowListClicked,
        onFollowerListClick = component::onUserFollowerListClicked,
        repository = userRepository,
        bookmarkRepository = bookmarkRepository,
        banRepository = banRepository,
        settingsRepository = settingsRepository,
        accountRepository = accountRepository,
        initialTab = instance.initialTab,
    )
}

@Composable
internal fun renderLogin(
    component: RootComponent,
    accountRepository: AccountRepository,
) {
    LoginScreen(
        onBack = component::onBack,
        onLoginSuccess = component::onLoginSuccess,
        onNetworkSettingClick = component::onNetworkSettingClicked,
        accountRepository = accountRepository,
    )
}

@Composable
internal fun renderComments(
    instance: Child.Comments,
    component: RootComponent,
    illustRepository: IllustRepository,
    accountRepository: AccountRepository,
) {
    CommentsScreen(
        illustId = instance.illustId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = illustRepository,
        accountRepository = accountRepository,
    )
}

@Composable
internal fun renderRelatedIllusts(
    instance: Child.RelatedIllusts,
    component: RootComponent,
    illustRepository: IllustRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    RelatedIllustsScreen(
        illustId = instance.illustId,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        repository = illustRepository,
        banRepository = banRepository,
        settingsRepository = settingsRepository,
    )
}

@Composable
internal fun renderIllustSeries(
    instance: Child.IllustSeries,
    component: RootComponent,
    illustRepository: IllustRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    IllustSeriesScreen(
        seriesId = instance.seriesId,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        repository = illustRepository,
        banRepository = banRepository,
        settingsRepository = settingsRepository,
    )
}

@Composable
internal fun renderUserFollowList(
    instance: Child.UserFollowList,
    component: RootComponent,
    userRepository: UserRepository,
) {
    UserFollowListScreen(
        userId = instance.userId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = userRepository,
    )
}

@Composable
internal fun renderUserFollowerList(
    instance: Child.UserFollowerList,
    component: RootComponent,
    userRepository: UserRepository,
) {
    UserFollowerListScreen(
        userId = instance.userId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = userRepository,
    )
}

@Composable
internal fun renderRecomUserList(
    component: RootComponent,
    userRepository: UserRepository,
) {
    RecomUserScreen(
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = userRepository,
    )
}

@Composable
internal fun renderSettingsPage(
    component: RootComponent,
    accountRepository: AccountRepository,
    boardRepository: BoardRepository,
) {
    SettingsScreen(
        onBack = component::onBack,
        onUserClick = { userId -> component.onUserClicked(userId, 0) },
        onUserBookmarksClick = { userId -> component.onUserClicked(userId, 1) },
        onAboutClick = component::onAboutClicked,
        onShieldClick = component::onShieldClicked,
        onLoginClick = component::onLoginClicked,
        onDownloadHistoryClick = component::onDownloadHistoryClicked,
        onThemeSettingClick = component::onThemeSettingClicked,
        onNetworkSettingClick = component::onNetworkSettingClicked,
        onDownloadSettingClick = component::onDownloadSettingClicked,
        onLayoutSettingClick = component::onLayoutSettingClicked,
        onLanguageSettingClick = component::onLanguageSettingClicked,
        onWidgetRecommendSettingClick = component::onWidgetRecommendSettingClicked,
        onInteractionSettingClick = component::onInteractionSettingClicked,
        onQualitySettingClick = component::onQualitySettingClicked,
        onCopyTextSettingClick = component::onCopyTextSettingClicked,
        onWelcomePageSettingClick = component::onWelcomePageSettingClicked,
        onBookTagClick = component::onBookTagClicked,
        onUpdateSettingClick = component::onUpdateSettingClicked,
        onAccountEditClick = component::onAccountEditClicked,
        onAccountManageClick = component::onAccountManageClicked,
        onNovelBrowseClick = component::onNovelBrowseClicked,
        onHistoryClick = component::onHistoryClicked,
        onDownloadTaskClick = component::onDownloadTaskClicked,
        onDataExportClick = component::onDataExportClicked,
        onBoardClick = component::onBoardClicked,
        onGuideClick = component::onGuideClicked,
        accountRepository = accountRepository,
        boardRepository = boardRepository,
    )
}

@Composable
internal fun renderSearch(
    instance: Child.Search,
    component: RootComponent,
    searchRepository: SearchRepository,
    settingsRepository: SettingsRepository,
    banRepository: BanRepository,
) {
    SearchScreen(
        onIllustClick = component::onIllustClicked,
        onUserClick = component::onUserClicked,
        repository = searchRepository,
        settingsRepository = settingsRepository,
        banRepository = banRepository,
        initialQuery = instance.query,
    )
}

@Composable
internal fun renderDownloadHistory(
    component: RootComponent,
    downloadHistoryRepository: DownloadHistoryRepository,
) {
    DownloadHistoryScreen(
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        repository = downloadHistoryRepository,
    )
}

@Composable
internal fun renderShield(
    component: RootComponent,
    settingsRepository: SettingsRepository,
    banRepository: BanRepository,
    userRepository: UserRepository,
) {
    ShieldScreen(
        onBack = component::onBack,
        onAISettingClick = component::onAISettingClicked,
        settingsRepository = settingsRepository,
        banRepository = banRepository,
        userRepository = userRepository,
    )
}

@Composable
internal fun renderAISetting(
    instance: Child.AISetting,
    component: RootComponent,
    userRepository: UserRepository,
) {
    UserShowAISettingScreen(
        showAI = instance.showAI,
        onBack = component::onBack,
        userRepository = userRepository,
    )
}

@Composable
internal fun renderThemeSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    ThemeSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderNetworkSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    NetworkSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderDownloadSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    DownloadSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderLayoutSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    LayoutSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderLanguageSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    LanguageSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderWidgetRecommendSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    WidgetRecommendSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderInteractionSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    InteractionSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderUpdateSetting(
    settingsRepository: SettingsRepository,
    updateCheckClient: HttpClient,
    onBack: () -> Unit,
) {
    UpdateSettingScreen(
        settingsRepository = settingsRepository,
        updateCheckClient = updateCheckClient,
        onBack = onBack,
    )
}

@Composable
internal fun renderAccountEdit(
    component: RootComponent,
    accountRepository: AccountRepository,
) {
    AccountEditScreen(
        onBack = component::onBack,
        accountRepository = accountRepository,
    )
}

@Composable
internal fun renderHistory(
    component: RootComponent,
) {
    HistoryScreen(
        onBack = component::onBack,
        // 历史记录使用 Long 保存作品 ID 以避免数据库溢出，
        // 导航层仍使用 Int，在此处做类型转换。
        onIllustClick = { component.onIllustClicked(it.toInt()) },
    )
}

@Composable
internal fun renderDownloadTask(
    component: RootComponent,
    downloadRepository: DownloadRepository,
    downloadHistoryRepository: DownloadHistoryRepository,
) {
    DownloadTaskScreen(
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        downloadRepository = downloadRepository,
        downloadHistoryRepository = downloadHistoryRepository,
    )
}

@Composable
internal fun renderDataExport(
    component: RootComponent,
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    novelHistoryRepository: NovelHistoryRepository,
    muteRepository: MuteRepository,
) {
    DataExportScreen(
        onBack = component::onBack,
        settingsRepository = settingsRepository,
        historyRepository = historyRepository,
        novelHistoryRepository = novelHistoryRepository,
        muteRepository = muteRepository,
    )
}

@Composable
internal fun renderBoard(
    component: RootComponent,
    boardRepository: BoardRepository,
) {
    BoardScreen(
        onBack = component::onBack,
        boardRepository = boardRepository,
    )
}

@Composable
internal fun renderQualitySetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    QualitySettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderCopyTextSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    CopyTextSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderWelcomePageSetting(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    WelcomePageSettingScreen(
        settingsRepository = settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderAbout(
    component: RootComponent,
) {
    AboutScreen(
        onBack = component::onBack,
        onThanksClick = component::onThanksClicked,
        onUpdateClick = component::onUpdateSettingClicked,
    )
}

@Composable
internal fun renderBookTag(
    component: RootComponent,
    settingsRepository: SettingsRepository,
) {
    BookTagScreen(
        settingsRepository = settingsRepository,
        onBack = component::onBack,
        onTagSearch = component::onSearchClicked,
    )
}

@Composable
internal fun renderThanks(
    component: RootComponent,
) {
    ThanksScreen(
        onBack = component::onBack,
    )
}

@Composable
internal fun renderSpotlightDetail(
    instance: Child.SpotlightDetail,
    component: RootComponent,
    illustRepository: IllustRepository,
) {
    SpotlightDetailScreen(
        article = instance.article,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        onUserClick = component::onUserClicked,
        onArticleClick = component::onSpotlightArticleClicked,
        repository = illustRepository,
    )
}

@Composable
internal fun renderGuide(
    component: RootComponent,
    settingsRepository: SettingsRepository,
    accountRepository: AccountRepository,
) {
    GuideScreen(
        settingsRepository = settingsRepository,
        accountRepository = accountRepository,
        onLoginClick = component::onLoginClicked,
        onFinish = component::onGuideFinished,
    )
}

@Composable
internal fun renderAccountManage(
    component: RootComponent,
    accountRepository: AccountRepository,
) {
    AccountManageScreen(
        accountRepository = accountRepository,
        onBack = component::onBack,
        onAddAccount = component::onLoginClicked,
    )
}

@Composable
internal fun renderNovel(
    component: RootComponent,
    novelRepository: NovelRepository?,
) {
    novelRepository?.let { repo ->
        NovelScreen(
            novelRepository = repo,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
        )
    }
}

@Composable
internal fun renderNovelViewer(
    instance: Child.NovelViewer,
    component: RootComponent,
    novelRepository: NovelRepository?,
) {
    novelRepository?.let { repo ->
        NovelViewerScreen(
            novelId = instance.novelId,
            novelRepository = repo,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
        )
    }
}