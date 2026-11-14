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
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.skin.CouiAboutPage
import com.perol.pixez.shared.ui.skin.CouiAccountEditPage
import com.perol.pixez.shared.ui.skin.CouiAccountManagePage
import com.perol.pixez.shared.ui.skin.CouiAiSettingPage
import com.perol.pixez.shared.ui.skin.CouiBoardPage
import com.perol.pixez.shared.ui.skin.CouiBookTagPage
import com.perol.pixez.shared.ui.skin.CouiCommentsPage
import com.perol.pixez.shared.ui.skin.CouiCopyTextSettingPage
import com.perol.pixez.shared.ui.skin.CouiDownloadHistoryPage
import com.perol.pixez.shared.ui.skin.CouiDownloadSettingPage
import com.perol.pixez.shared.ui.skin.CouiDownloadTaskPage
import com.perol.pixez.shared.ui.skin.CouiHistoryPage
import com.perol.pixez.shared.ui.skin.CouiUserListKind
import com.perol.pixez.shared.ui.skin.CouiUserListPage
import com.perol.pixez.shared.ui.skin.CouiUserNovelListPage
import com.perol.pixez.shared.ui.skin.CouiIllustDetailPage
import com.perol.pixez.shared.ui.skin.CouiMainTabPage
import com.perol.pixez.shared.ui.skin.CouiNovelViewerPage
import com.perol.pixez.shared.ui.skin.CouiNovelPage
import com.perol.pixez.shared.ui.skin.CouiNetworkSettingPage
import com.perol.pixez.shared.ui.skin.CouiRecomUserPage
import com.perol.pixez.shared.ui.skin.CouiShieldPage
import com.perol.pixez.shared.ui.skin.CouiThanksPage
import com.perol.pixez.shared.ui.skin.CouiThemeSettingPage
import com.perol.pixez.shared.ui.skin.CouiWelcomePageSettingPage
import com.perol.pixez.shared.ui.skin.CouiWidgetRecommendPage
import com.perol.pixez.shared.ui.skin.CouiRelatedIllustsPage
import com.perol.pixez.shared.ui.skin.CouiIllustSeriesPage
import com.perol.pixez.shared.ui.skin.CouiInteractionSettingPage
import com.perol.pixez.shared.ui.skin.CouiLanguageSettingPage
import com.perol.pixez.shared.ui.skin.CouiQualitySettingPage
import com.perol.pixez.shared.ui.skin.CouiLoginPage
import com.perol.pixez.shared.ui.skin.CouiSettingsPage
import com.perol.pixez.shared.ui.skin.CouiUserDetailPage
import com.perol.pixez.shared.ui.skin.LocalUiSkin
import com.perol.pixez.shared.ui.skin.UiSkin
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
import com.perol.pixez.shared.ui.screens.HistoryScreen
import com.perol.pixez.shared.ui.screens.IllustDetailScreen
import com.perol.pixez.shared.ui.screens.IllustSeriesScreen
import com.perol.pixez.shared.ui.screens.InteractionSettingScreen
import com.perol.pixez.shared.ui.screens.LanguageSettingScreen
import com.perol.pixez.shared.ui.screens.LayoutSettingScreen
import com.perol.pixez.shared.ui.screens.LoginScreen
import com.perol.pixez.shared.ui.screens.NetworkSettingScreen
import com.perol.pixez.shared.ui.screens.NovelScreen
import com.perol.pixez.shared.ui.screens.NovelViewerScreen
import com.perol.pixez.shared.ui.screens.QualitySettingScreen
import com.perol.pixez.shared.ui.screens.RecomUserScreen
import com.perol.pixez.shared.ui.screens.RelatedIllustsScreen
import com.perol.pixez.shared.ui.screens.SearchScreen
import com.perol.pixez.shared.ui.screens.SettingsScreen
import com.perol.pixez.shared.ui.screens.ShieldScreen
import com.perol.pixez.shared.ui.screens.SpotlightDetailScreen
import com.perol.pixez.shared.ui.screens.ThanksScreen
import com.perol.pixez.shared.ui.screens.ThemeSettingScreen
import com.perol.pixez.shared.ui.screens.UpdateSettingScreen
import com.perol.pixez.shared.ui.screens.UserDetailScreen
import com.perol.pixez.shared.ui.screens.UserFollowListScreen
import com.perol.pixez.shared.ui.screens.UserFollowerListScreen
import com.perol.pixez.shared.ui.screens.UserNovelListScreen
import com.perol.pixez.shared.ui.screens.UserRelatedListScreen
import com.perol.pixez.shared.ui.screens.UserShowAISettingScreen
import com.perol.pixez.shared.ui.screens.WelcomePageSettingScreen
import com.perol.pixez.shared.ui.screens.WidgetRecommendSettingScreen

/**
 * [RootContent] 中各导航子页面（[Config]）的渲染函数集合。
 *
 * 这些函数由原 `RootContent.kt` 中巨型 `when (child.instance)` 的各个分支原样抽取而来，
 * 每个函数负责将某一类子页面映射到对应的 Screen 调用。
 * 分发编排仍保留在 [RootContent] 内，此处仅承载「渲染哪一个页面」的具体实现。
 *
 * 中-9 注入单通道：render* 只传路由参数与导航回调，数据仓储一律由各 Screen 内部
 * 从 `LocalXxxRepository` 读取（RootContent 组合根统一 provide），不再逐层穿参。
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
    // 高-5：跨页恢复容器，由 createChild 在 Main child 组件初始化时接线（consume/register
    // 只执行一次）；快照存原始 ID 列表，恢复经仓库 illustsCache 回查。
    feedRestore: MainFeedRestore,
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
        val descriptor = MAIN_TAB_DESCRIPTORS[page]
        if (LocalUiSkin.current == UiSkin.COLOROS) {
            // 皮肤差异收敛在页面宿主：COUI 标签用 Android 实现，未迁移标签执行 fallback。
            CouiMainTabPage(
                tab = descriptor.tab,
                component = component,
                feedRestore = feedRestore,
            ) {
                descriptor.content(component, feedRestore)
            }
        } else {
            descriptor.content(component, feedRestore)
        }
    }
}

@Composable
internal fun renderIllustDetail(
    component: IllustDetailComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        IllustDetailScreen(
            illustId = component.illustId,
            // Screen 的 onBack 为 () -> Unit，组件回调返回消费与否的 Boolean，此处丢弃返回值。
            onBack = { component.onBack() },
            onUserClick = component.onUserClick,
            onCommentsClick = component.onCommentsClick,
            onRelatedIllustsClick = component.onRelatedIllustsClick,
            onIllustSeriesClick = component.onIllustSeriesClick,
            onTagClick = component.onTagClick,
            onIllustClick = component.onIllustClick,
            onNovelClick = component.onNovelClick,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiIllustDetailPage(
            illustId = component.illustId,
            onBack = { component.onBack() },
            onUserClick = component.onUserClick,
            onCommentsClick = component.onCommentsClick,
            onRelatedIllustsClick = component.onRelatedIllustsClick,
            onIllustSeriesClick = component.onIllustSeriesClick,
            onTagClick = component.onTagClick,
            onIllustClick = component.onIllustClick,
            onNovelClick = component.onNovelClick,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderUserDetail(
    component: UserDetailComponent,
    root: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        UserDetailScreen(
            userId = component.userId,
            pageState = component.pageState,
            onBack = root::onBack,
            onIllustClick = root::onIllustClicked,
            onFollowListClick = root::onUserFollowListClicked,
            onFollowerListClick = root::onUserFollowerListClicked,
            onRelatedUsersClick = root::onUserRelatedListClicked,
            onUserNovelsClick = root::onUserNovelListClicked,
            initialTab = component.config.initialTab,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiUserDetailPage(
            userId = component.userId,
            pageState = component.pageState,
            onBack = root::onBack,
            onIllustClick = root::onIllustClicked,
            onFollowListClick = root::onUserFollowListClicked,
            onFollowerListClick = root::onUserFollowerListClicked,
            onRelatedUsersClick = root::onUserRelatedListClicked,
            onUserNovelsClick = root::onUserNovelListClicked,
            initialTab = component.config.initialTab,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderLogin(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        LoginScreen(
            onBack = component::onBack,
            onLoginSuccess = component::onLoginSuccess,
            onNetworkSettingClick = component::onNetworkSettingClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiLoginPage(
            onBack = component::onBack,
            onLoginSuccess = component::onLoginSuccess,
            onNetworkSettingClick = component::onNetworkSettingClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderComments(
    instance: Config.Comments,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        CommentsScreen(
            illustId = instance.illustId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
            isNovel = instance.isNovel,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiCommentsPage(
            illustId = instance.illustId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
            isNovel = instance.isNovel,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderRelatedIllusts(
    instance: Config.RelatedIllusts,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        RelatedIllustsScreen(
            illustId = instance.illustId,
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiRelatedIllustsPage(
            illustId = instance.illustId,
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderIllustSeries(
    instance: Config.IllustSeries,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        IllustSeriesScreen(
            seriesId = instance.seriesId,
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiIllustSeriesPage(
            seriesId = instance.seriesId,
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderUserFollowList(
    instance: Config.UserFollowList,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        UserFollowListScreen(
            userId = instance.userId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiUserListPage(
            kind = CouiUserListKind.Following,
            userId = instance.userId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderUserFollowerList(
    instance: Config.UserFollowerList,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        UserFollowerListScreen(
            userId = instance.userId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiUserListPage(
            kind = CouiUserListKind.Followers,
            userId = instance.userId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderRecomUserList(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        RecomUserScreen(
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiRecomUserPage(
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderSettingsPage(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
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
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiSettingsPage(
            onBack = component::onBack,
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
            onHistoryClick = component::onHistoryClicked,
            onDownloadTaskClick = component::onDownloadTaskClicked,
            onDataExportClick = component::onDataExportClicked,
            onBoardClick = component::onBoardClicked,
            onGuideClick = component::onGuideClicked,
            onUserClick = { userId -> component.onUserClicked(userId, 0) },
            onUserBookmarksClick = { userId -> component.onUserClicked(userId, 1) },
            onAccountManageClick = component::onAccountManageClicked,
            onNovelBrowseClick = component::onNovelBrowseClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderSearch(
    instance: Config.Search,
    component: RootComponent,
) {
    SearchScreen(
        onIllustClick = component::onIllustClicked,
        onUserClick = component::onUserClicked,
        onNovelClick = component::onNovelClicked,
        initialQuery = instance.query,
    )
}

@Composable
internal fun renderDownloadHistory(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        DownloadHistoryScreen(
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiDownloadHistoryPage(
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderShield(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        ShieldScreen(
            onBack = component::onBack,
            onAISettingClick = component::onAISettingClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiShieldPage(
            onBack = component::onBack,
            onAISettingClick = component::onAISettingClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderAISetting(
    instance: Config.AISetting,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        UserShowAISettingScreen(
            showAI = instance.showAI,
            onBack = component::onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiAiSettingPage(
            showAI = instance.showAI,
            onBack = component::onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderThemeSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        ThemeSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiThemeSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderNetworkSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        NetworkSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiNetworkSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderDownloadSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        DownloadSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiDownloadSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderLayoutSetting(
    onBack: () -> Unit,
) {
    LayoutSettingScreen(
        onBack = onBack,
    )
}

@Composable
internal fun renderLanguageSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        LanguageSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiLanguageSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderWidgetRecommendSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        WidgetRecommendSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiWidgetRecommendPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderInteractionSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        InteractionSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiInteractionSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderUpdateSetting(
    onBack: () -> Unit,
) {
    UpdateSettingScreen(
        onBack = onBack,
    )
}

@Composable
internal fun renderAccountEdit(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        AccountEditScreen(
            onBack = component::onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiAccountEditPage(
            onBack = component::onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderHistory(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        HistoryScreen(
            onBack = component::onBack,
            // 历史记录使用 Long 保存作品 ID 以避免数据库溢出，
            // 导航层仍使用 Int，在此处做类型转换。
            onIllustClick = { component.onIllustClicked(it) },
            onNovelClick = component::onNovelClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiHistoryPage(
            onBack = component::onBack,
            onIllustClick = { component.onIllustClicked(it) },
            onNovelClick = component::onNovelClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderDownloadTask(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        DownloadTaskScreen(
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiDownloadTaskPage(
            onBack = component::onBack,
            onIllustClick = component::onIllustClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderDataExport(
    component: RootComponent,
) {
    DataExportScreen(
        onBack = component::onBack,
    )
}

@Composable
internal fun renderBoard(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        BoardScreen(
            onBack = component::onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiBoardPage(
            onBack = component::onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderQualitySetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        QualitySettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiQualitySettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderCopyTextSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        CopyTextSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiCopyTextSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderWelcomePageSetting(
    onBack: () -> Unit,
) {
    val renderMiuix: @Composable () -> Unit = {
        WelcomePageSettingScreen(
            onBack = onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiWelcomePageSettingPage(
            onBack = onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderAbout(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        AboutScreen(
            onBack = component::onBack,
            onThanksClick = component::onThanksClicked,
            onUpdateClick = component::onUpdateSettingClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiAboutPage(
            onBack = component::onBack,
            onThanksClick = component::onThanksClicked,
            onUpdateClick = component::onUpdateSettingClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderBookTag(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        BookTagScreen(
            onBack = component::onBack,
            onTagSearch = component::onSearchClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiBookTagPage(
            onBack = component::onBack,
            onTagSearch = component::onSearchClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderThanks(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        ThanksScreen(
            onBack = component::onBack,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiThanksPage(
            onBack = component::onBack,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderSpotlightDetail(
    instance: Config.SpotlightDetail,
    component: RootComponent,
) {
    SpotlightDetailScreen(
        article = instance.article,
        onBack = component::onBack,
        onIllustClick = component::onIllustClicked,
        onUserClick = component::onUserClicked,
        onArticleClick = component::onSpotlightArticleClicked,
    )
}

@Composable
internal fun renderGuide(
    component: RootComponent,
) {
    GuideScreen(
        onLoginClick = component::onLoginClicked,
        onFinish = component::onGuideFinished,
    )
}

@Composable
internal fun renderAccountManage(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        AccountManageScreen(
            onBack = component::onBack,
            onAddAccount = component::onLoginClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiAccountManagePage(
            onBack = component::onBack,
            onAddAccount = component::onLoginClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderNovel(
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        NovelScreen(
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
            onUserClick = component::onUserClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiNovelPage(
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
            onUserClick = component::onUserClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderNovelViewer(
    instance: Config.NovelViewer,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        NovelViewerScreen(
            novelId = instance.novelId,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
            onIllustClick = component::onIllustClicked,
            onUserClick = component::onUserClicked,
            onCommentsClick = component::onNovelCommentsClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiNovelViewerPage(
            novelId = instance.novelId,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
            onIllustClick = component::onIllustClicked,
            onUserClick = component::onUserClicked,
            onCommentsClick = component::onNovelCommentsClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderUserRelatedList(
    instance: Config.UserRelatedList,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        UserRelatedListScreen(
            userId = instance.userId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiUserListPage(
            kind = CouiUserListKind.Related,
            userId = instance.userId,
            onBack = component::onBack,
            onUserClick = component::onUserClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}

@Composable
internal fun renderUserNovelList(
    instance: Config.UserNovelList,
    component: RootComponent,
) {
    val renderMiuix: @Composable () -> Unit = {
        UserNovelListScreen(
            userId = instance.userId,
            authorName = instance.authorName,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
            onUserClick = component::onUserClicked,
        )
    }
    if (LocalUiSkin.current == UiSkin.COLOROS) {
        CouiUserNovelListPage(
            userId = instance.userId,
            authorName = instance.authorName,
            onBack = component::onBack,
            onNovelClick = component::onNovelClicked,
            onUserClick = component::onUserClicked,
            fallback = renderMiuix,
        )
    } else {
        renderMiuix()
    }
}