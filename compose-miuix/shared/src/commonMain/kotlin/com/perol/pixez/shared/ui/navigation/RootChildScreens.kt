package com.perol.pixez.shared.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
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

    // 点击/外部触发的滚动期间不回传手势位置，底栏滑块保持 spring 动画手感。
    var programmaticScroll by remember { mutableStateOf(false) }
    val tabGesturePosition = LocalTabGesturePosition.current

    // 响应外部或底部导航栏点击驱动 Pager 平滑滚动
    LaunchedEffect(selectedTab) {
        val targetIndex = RootComponent.MAIN_TAB_ORDER.indexOf(selectedTab)
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            programmaticScroll = true
            try {
                if (kotlin.math.abs(pagerState.currentPage - targetIndex) > 1) {
                    pagerState.scrollToPage(targetIndex)
                } else {
                    pagerState.animateScrollToPage(targetIndex)
                }
            } finally {
                programmaticScroll = false
            }
        }
    }

    // 手势翻页全程回传连续位置：底栏滑块逐帧跟手（松手落位后回写 null，滑块切回 spring 值）。
    LaunchedEffect(pagerState) {
        snapshotFlow {
            if (pagerState.isScrollInProgress && !programmaticScroll) {
                pagerState.currentPage + pagerState.currentPageOffsetFraction
            } else {
                null
            }
        }.collect { position -> tabGesturePosition.value = position }
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
    registry: RepositoryRegistry,
) {
    IllustDetailScreen(
        illustId = instance.illustId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        onCommentsClick = component::onCommentsClicked,
        onRelatedIllustsClick = component::onRelatedIllustsClicked,
        onIllustSeriesClick = component::onIllustSeriesClicked,
        onTagClick = component::onSearchClicked,
        repository = registry.illustRepository,
        bookmarkRepository = registry.bookmarkRepository,
        downloadRepository = registry.downloadRepository,
        banRepository = registry.banRepository,
        historyRepository = registry.historyRepository,
        onIllustClick = component::onIllustClicked,
        onNovelClick = component::onNovelClicked,
    )
}

@Composable
internal fun renderUserDetail(
    instance: Child.UserDetail,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    UserDetailScreen(
        userId = instance.userId,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        onFollowListClick = component::onUserFollowListClicked,
        onFollowerListClick = component::onUserFollowerListClicked,
        repository = registry.userRepository,
        bookmarkRepository = registry.bookmarkRepository,
        banRepository = registry.banRepository,
        settingsRepository = registry.settingsRepository,
        accountRepository = registry.accountRepository,
        initialTab = instance.initialTab,
    )
}

@Composable
internal fun renderLogin(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    LoginScreen(
        onBack = component::onBack,
        onLoginSuccess = component::onLoginSuccess,
        onNetworkSettingClick = component::onNetworkSettingClicked,
        accountRepository = registry.accountRepository,
    )
}

@Composable
internal fun renderComments(
    instance: Child.Comments,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    CommentsScreen(
        illustId = instance.illustId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = registry.illustRepository,
        accountRepository = registry.accountRepository,
    )
}

@Composable
internal fun renderRelatedIllusts(
    instance: Child.RelatedIllusts,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    RelatedIllustsScreen(
        illustId = instance.illustId,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        repository = registry.illustRepository,
        banRepository = registry.banRepository,
        settingsRepository = registry.settingsRepository,
    )
}

@Composable
internal fun renderIllustSeries(
    instance: Child.IllustSeries,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    IllustSeriesScreen(
        seriesId = instance.seriesId,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        repository = registry.illustRepository,
        banRepository = registry.banRepository,
        settingsRepository = registry.settingsRepository,
    )
}

@Composable
internal fun renderUserFollowList(
    instance: Child.UserFollowList,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    UserFollowListScreen(
        userId = instance.userId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = registry.userRepository,
    )
}

@Composable
internal fun renderUserFollowerList(
    instance: Child.UserFollowerList,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    UserFollowerListScreen(
        userId = instance.userId,
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = registry.userRepository,
    )
}

@Composable
internal fun renderRecomUserList(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    RecomUserScreen(
        onBack = component::onBack,
        onUserClick = component::onUserClicked,
        repository = registry.userRepository,
    )
}

@Composable
internal fun renderSettingsPage(
    component: RootComponent,
    registry: RepositoryRegistry,
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
        accountRepository = registry.accountRepository,
        boardRepository = registry.boardRepository,
    )
}

@Composable
internal fun renderSearch(
    instance: Child.Search,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    SearchScreen(
        onIllustClick = component::onIllustClicked,
        onUserClick = component::onUserClicked,
        repository = registry.searchRepository,
        settingsRepository = registry.settingsRepository,
        banRepository = registry.banRepository,
        initialQuery = instance.query,
    )
}

@Composable
internal fun renderDownloadHistory(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    DownloadHistoryScreen(
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        repository = registry.downloadHistoryRepository,
    )
}

@Composable
internal fun renderShield(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    ShieldScreen(
        onBack = component::onBack,
        onAISettingClick = component::onAISettingClicked,
        settingsRepository = registry.settingsRepository,
        banRepository = registry.banRepository,
        userRepository = registry.userRepository,
    )
}

@Composable
internal fun renderAISetting(
    instance: Child.AISetting,
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    UserShowAISettingScreen(
        showAI = instance.showAI,
        onBack = component::onBack,
        userRepository = registry.userRepository,
    )
}

@Composable
internal fun renderThemeSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    ThemeSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderNetworkSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    NetworkSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderDownloadSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    DownloadSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderLayoutSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    LayoutSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderLanguageSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    LanguageSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderWidgetRecommendSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    WidgetRecommendSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderInteractionSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    InteractionSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderUpdateSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    UpdateSettingScreen(
        settingsRepository = registry.settingsRepository,
        updateCheckClient = registry.updateCheckClient,
        onBack = onBack,
    )
}

@Composable
internal fun renderAccountEdit(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    AccountEditScreen(
        onBack = component::onBack,
        accountRepository = registry.accountRepository,
    )
}

@Composable
internal fun renderHistory(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    HistoryScreen(
        onBack = component::onBack,
        // 历史记录使用 Long 保存作品 ID 以避免数据库溢出，
        // 导航层仍使用 Int，在此处做类型转换。
        onIllustClick = { component.onIllustClicked(it) },
        repository = registry.historyRepository,
    )
}

@Composable
internal fun renderDownloadTask(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    DownloadTaskScreen(
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        downloadRepository = registry.downloadRepository,
        downloadHistoryRepository = registry.downloadHistoryRepository,
    )
}

@Composable
internal fun renderDataExport(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    DataExportScreen(
        onBack = component::onBack,
        settingsRepository = registry.settingsRepository,
        historyRepository = registry.historyRepository,
        novelHistoryRepository = registry.novelHistoryRepository,
        muteRepository = registry.muteRepository,
    )
}

@Composable
internal fun renderBoard(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    BoardScreen(
        onBack = component::onBack,
        boardRepository = registry.boardRepository,
    )
}

@Composable
internal fun renderQualitySetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    QualitySettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderCopyTextSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    CopyTextSettingScreen(
        settingsRepository = registry.settingsRepository,
        onBack = onBack,
    )
}

@Composable
internal fun renderWelcomePageSetting(
    registry: RepositoryRegistry,
    onBack: () -> Unit,
) {
    WelcomePageSettingScreen(
        settingsRepository = registry.settingsRepository,
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
    registry: RepositoryRegistry,
) {
    BookTagScreen(
        settingsRepository = registry.settingsRepository,
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
    registry: RepositoryRegistry,
) {
    SpotlightDetailScreen(
        article = instance.article,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        onUserClick = component::onUserClicked,
        onArticleClick = component::onSpotlightArticleClicked,
        repository = registry.illustRepository,
    )
}

@Composable
internal fun renderGuide(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    GuideScreen(
        settingsRepository = registry.settingsRepository,
        accountRepository = registry.accountRepository,
        onLoginClick = component::onLoginClicked,
        onFinish = component::onGuideFinished,
    )
}

@Composable
internal fun renderAccountManage(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    AccountManageScreen(
        accountRepository = registry.accountRepository,
        onBack = component::onBack,
        onAddAccount = component::onLoginClicked,
    )
}

@Composable
internal fun renderNovel(
    component: RootComponent,
    registry: RepositoryRegistry,
) {
    registry.novelRepository?.let { repo ->
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
    registry: RepositoryRegistry,
) {
    registry.novelRepository?.let { repo ->
        NovelViewerScreen(
            novelId = instance.novelId,
            novelRepository = repo,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
        )
    }
}