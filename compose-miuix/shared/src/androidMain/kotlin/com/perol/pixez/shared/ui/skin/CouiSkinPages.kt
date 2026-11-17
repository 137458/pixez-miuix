package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.ui.coui.page.CouiAboutScreen
import com.perol.pixez.shared.ui.coui.page.CouiAccountEditScreen
import com.perol.pixez.shared.ui.coui.page.CouiAccountManageScreen
import com.perol.pixez.shared.ui.coui.page.CouiAiSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiBoardScreen
import com.perol.pixez.shared.ui.coui.page.CouiBookTagScreen
import com.perol.pixez.shared.ui.coui.page.CouiCommentsScreen
import com.perol.pixez.shared.ui.coui.page.CouiCopyTextSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiDataExportScreen
import com.perol.pixez.shared.ui.coui.page.CouiDownloadHistoryScreen
import com.perol.pixez.shared.ui.coui.page.CouiDownloadSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiDownloadTaskScreen
import com.perol.pixez.shared.ui.coui.page.CouiGuideScreen
import com.perol.pixez.shared.ui.coui.page.CouiHelloScreen
import com.perol.pixez.shared.ui.coui.page.CouiHistoryScreen
import com.perol.pixez.shared.ui.coui.page.CouiIllustDetailScreen
import com.perol.pixez.shared.ui.coui.page.CouiIllustSeriesScreen
import com.perol.pixez.shared.ui.coui.page.CouiInteractionSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiLanguageSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiLayoutSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiLoginScreen
import com.perol.pixez.shared.ui.coui.page.CouiNetworkSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiNewScreen
import com.perol.pixez.shared.ui.coui.page.CouiNovelScreen
import com.perol.pixez.shared.ui.coui.page.CouiNovelViewerScreen
import com.perol.pixez.shared.ui.coui.page.CouiQualitySettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiRankingScreen
import com.perol.pixez.shared.ui.coui.page.CouiRecomUserScreen
import com.perol.pixez.shared.ui.coui.page.CouiRelatedIllustsScreen
import com.perol.pixez.shared.ui.coui.page.CouiSearchScreen
import com.perol.pixez.shared.ui.coui.page.CouiSettingsScreen
import com.perol.pixez.shared.ui.coui.page.CouiShieldScreen
import com.perol.pixez.shared.ui.coui.page.CouiSpotlightDetailScreen
import com.perol.pixez.shared.ui.coui.page.CouiSpotlightScreen
import com.perol.pixez.shared.ui.coui.page.CouiThanksScreen
import com.perol.pixez.shared.ui.coui.page.CouiThemeSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiUpdateSettingScreen
import com.perol.pixez.shared.ui.coui.page.CouiUserDetailScreen
import com.perol.pixez.shared.ui.coui.page.CouiUserListScreen
import com.perol.pixez.shared.ui.coui.page.CouiUserNovelListScreen
import com.perol.pixez.shared.ui.coui.page.CouiWelcomePageScreen
import com.perol.pixez.shared.ui.coui.page.CouiWidgetRecommendScreen
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.state.UserDetailPageState

/**
 * COUI 皮肤的 [SkinPages] 实现。
 *
 * 每个方法只做一件事：把接缝入参转投给既有的 CouiXxxScreen，
 * 于是皮肤差异只体现在「由谁实现」上，导航骨架与页面实现互不知情。
 */
internal object CouiSkinPages : SkinPages {
    /** 底栏绘制细节放在 UiSkinNavigationBar.kt，这里只委托，免得皮肤对象同时承担接缝与绘制两件事。 */
    @Composable
    override fun navigationBar(
        items: List<SkinNavItem>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    ) {
        CouiSkinNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier,
        )
    }

    @Composable
    override fun mainTabPage(
        tab: RootComponent.MainTab,
        component: RootComponent,
        feedRestore: MainFeedRestore,
    ) {
        when (tab) {
            RootComponent.MainTab.Hello -> {
                val reselectFlow = remember(tab) { component.reselectFlowOf(tab) }
                CouiHelloScreen(
                    onIllustClick = component::onIllustClicked,
                    onUserClick = component::onUserClicked,
                    onSettingsClick = component::onSettingsClicked,
                    onLoginClick = component::onLoginClicked,
                    onRecomUserClick = component::onRecomUserListClicked,
                    feedRestore = feedRestore.hello,
                    reselectFlow = reselectFlow,
                )
            }
            RootComponent.MainTab.Ranking -> {
                CouiRankingScreen(
                    onIllustClick = component::onIllustClicked,
                    feedRestore = feedRestore.ranking,
                )
            }
            RootComponent.MainTab.Spotlight -> {
                CouiSpotlightScreen(
                    onArticleClick = component::onSpotlightArticleClicked,
                )
            }
            RootComponent.MainTab.New -> {
                val reselectFlow = remember(tab) { component.reselectFlowOf(tab) }
                CouiNewScreen(
                    onIllustClick = component::onIllustClicked,
                    onUserClick = component::onUserClicked,
                    onLoginClick = component::onLoginClicked,
                    onSeriesClick = component::onIllustSeriesClicked,
                    reselectFlow = reselectFlow,
                )
            }
            RootComponent.MainTab.Search -> {
                CouiSearchScreen(
                    onIllustClick = component::onIllustClicked,
                    onUserClick = component::onUserClicked,
                    onNovelClick = component::onNovelClicked,
                )
            }
        }
    }

    @Composable
    override fun illustDetail(
        illustId: Long,
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
        onCommentsClick: (Long) -> Unit,
        onRelatedIllustsClick: (Long) -> Unit,
        onIllustSeriesClick: (Long) -> Unit,
        onTagClick: (String) -> Unit,
        onIllustClick: ((Long) -> Unit)?,
        onNovelClick: ((Long) -> Unit)?,
    ) {
        CouiIllustDetailScreen(
            illustId = illustId,
            onBack = onBack,
            onUserClick = onUserClick,
            onCommentsClick = onCommentsClick,
            onRelatedIllustsClick = onRelatedIllustsClick,
            onIllustSeriesClick = onIllustSeriesClick,
            onTagClick = onTagClick,
            onIllustClick = onIllustClick,
            onNovelClick = onNovelClick,
        )
    }

    @Composable
    override fun userDetail(
        userId: Long,
        pageState: UserDetailPageState,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
        onFollowListClick: (Long) -> Unit,
        onFollowerListClick: (Long) -> Unit,
        onRelatedUsersClick: ((Long) -> Unit)?,
        onUserNovelsClick: ((Long, String) -> Unit)?,
        initialTab: Int,
    ) {
        CouiUserDetailScreen(
            userId = userId,
            pageState = pageState,
            onBack = onBack,
            onIllustClick = onIllustClick,
            onFollowListClick = onFollowListClick,
            onFollowerListClick = onFollowerListClick,
            onRelatedUsersClick = onRelatedUsersClick,
            onUserNovelsClick = onUserNovelsClick,
            initialTab = initialTab,
        )
    }

    @Composable
    override fun novelViewer(
        novelId: Long,
        onBack: () -> Unit,
        onNovelClick: (Long) -> Unit,
        onIllustClick: (Long) -> Unit,
        onUserClick: (Long) -> Unit,
        onCommentsClick: (Long) -> Unit,
    ) {
        CouiNovelViewerScreen(
            novelId = novelId,
            onBack = onBack,
            onNovelClick = onNovelClick,
            onIllustClick = onIllustClick,
            onUserClick = onUserClick,
            onCommentsClick = onCommentsClick,
        )
    }

    @Composable
    override fun comments(
        illustId: Long,
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
        isNovel: Boolean,
    ) {
        CouiCommentsScreen(
            illustId = illustId,
            onBack = onBack,
            onUserClick = onUserClick,
            isNovel = isNovel,
        )
    }

    @Composable
    override fun relatedIllusts(
        illustId: Long,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    ) {
        CouiRelatedIllustsScreen(
            illustId = illustId,
            onBack = onBack,
            onIllustClick = onIllustClick,
        )
    }

    @Composable
    override fun downloadHistory(
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    ) {
        CouiDownloadHistoryScreen(
            onBack = onBack,
            onIllustClick = onIllustClick,
        )
    }

    @Composable
    override fun accountEdit(
        onBack: () -> Unit,
    ) {
        CouiAccountEditScreen(onBack = onBack)
    }

    @Composable
    override fun login(
        onBack: () -> Unit,
        onLoginSuccess: () -> Unit,
        onNetworkSettingClick: (() -> Unit)?,
    ) {
        CouiLoginScreen(
            onBack = onBack,
            onLoginSuccess = onLoginSuccess,
            onNetworkSettingClick = onNetworkSettingClick,
        )
    }

    @Composable
    override fun illustSeries(
        seriesId: Long,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    ) {
        CouiIllustSeriesScreen(
            seriesId = seriesId,
            onBack = onBack,
            onIllustClick = onIllustClick,
        )
    }

    @Composable
    override fun userNovelList(
        userId: Long,
        authorName: String,
        onBack: () -> Unit,
        onNovelClick: (Long) -> Unit,
        onUserClick: ((Long) -> Unit)?,
    ) {
        CouiUserNovelListScreen(
            userId = userId,
            authorName = authorName,
            onBack = onBack,
            onNovelClick = onNovelClick,
            onUserClick = onUserClick,
        )
    }

    @Composable
    override fun userList(
        kind: CouiUserListKind,
        userId: Long,
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
    ) {
        CouiUserListScreen(
            kind = kind,
            userId = userId,
            onBack = onBack,
            onUserClick = onUserClick,
        )
    }

    @Composable
    override fun downloadTask(
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    ) {
        CouiDownloadTaskScreen(
            onBack = onBack,
            onIllustClick = onIllustClick,
        )
    }

    @Composable
    override fun history(
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
        onNovelClick: (Long) -> Unit,
    ) {
        CouiHistoryScreen(
            onBack = onBack,
            onIllustClick = onIllustClick,
            onNovelClick = onNovelClick,
        )
    }

    @Composable
    override fun board(
        onBack: () -> Unit,
    ) {
        CouiBoardScreen(onBack = onBack)
    }

    @Composable
    override fun recomUser(
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
    ) {
        CouiRecomUserScreen(
            onBack = onBack,
            onUserClick = onUserClick,
        )
    }

    @Composable
    override fun shield(
        onBack: () -> Unit,
        onAISettingClick: (showAI: Boolean) -> Unit,
    ) {
        CouiShieldScreen(
            onBack = onBack,
            onAISettingClick = onAISettingClick,
        )
    }

    @Composable
    override fun aiSetting(
        showAI: Boolean,
        onBack: () -> Unit,
    ) {
        CouiAiSettingScreen(
            showAI = showAI,
            onBack = onBack,
        )
    }

    @Composable
    override fun welcomePageSetting(
        onBack: () -> Unit,
    ) {
        CouiWelcomePageScreen(onBack = onBack)
    }

    @Composable
    override fun widgetRecommend(
        onBack: () -> Unit,
    ) {
        CouiWidgetRecommendScreen(onBack = onBack)
    }

    @Composable
    override fun interactionSetting(
        onBack: () -> Unit,
    ) {
        CouiInteractionSettingScreen(onBack = onBack)
    }

    @Composable
    override fun qualitySetting(
        onBack: () -> Unit,
    ) {
        CouiQualitySettingScreen(onBack = onBack)
    }

    @Composable
    override fun layoutSetting(
        onBack: () -> Unit,
    ) {
        CouiLayoutSettingScreen(onBack = onBack)
    }

    @Composable
    override fun updateSetting(
        onBack: () -> Unit,
    ) {
        CouiUpdateSettingScreen(onBack = onBack)
    }

    @Composable
    override fun dataExport(
        onBack: () -> Unit,
    ) {
        CouiDataExportScreen(onBack = onBack)
    }

    @Composable
    override fun spotlightDetail(
        article: SpotlightArticle,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
        onUserClick: (Long) -> Unit,
        onArticleClick: (SpotlightArticle) -> Unit,
    ) {
        CouiSpotlightDetailScreen(
            article = article,
            onBack = onBack,
            onIllustClick = onIllustClick,
            onUserClick = onUserClick,
            onArticleClick = onArticleClick,
        )
    }

    @Composable
    override fun guide(
        onLoginClick: () -> Unit,
        onFinish: () -> Unit,
    ) {
        CouiGuideScreen(
            onLoginClick = onLoginClick,
            onFinish = onFinish,
        )
    }

    @Composable
    override fun search(
        initialQuery: String,
        onIllustClick: (Long) -> Unit,
        onUserClick: (Long) -> Unit,
        onNovelClick: (Long) -> Unit,
    ) {
        CouiSearchScreen(
            initialQuery = initialQuery,
            onIllustClick = onIllustClick,
            onUserClick = onUserClick,
            onNovelClick = onNovelClick,
        )
    }

    @Composable
    override fun languageSetting(
        onBack: () -> Unit,
    ) {
        CouiLanguageSettingScreen(onBack = onBack)
    }

    @Composable
    override fun copyTextSetting(
        onBack: () -> Unit,
    ) {
        CouiCopyTextSettingScreen(onBack = onBack)
    }

    @Composable
    override fun downloadSetting(
        onBack: () -> Unit,
    ) {
        CouiDownloadSettingScreen(onBack = onBack)
    }

    @Composable
    override fun networkSetting(
        onBack: () -> Unit,
    ) {
        CouiNetworkSettingScreen(onBack = onBack)
    }

    @Composable
    override fun about(
        onBack: () -> Unit,
        onThanksClick: () -> Unit,
        onUpdateClick: () -> Unit,
    ) {
        CouiAboutScreen(
            onBack = onBack,
            onThanksClick = onThanksClick,
            onUpdateClick = onUpdateClick,
        )
    }

    @Composable
    override fun thanks(
        onBack: () -> Unit,
    ) {
        CouiThanksScreen(onBack = onBack)
    }

    @Composable
    override fun bookTag(
        onBack: () -> Unit,
        onTagSearch: (String) -> Unit,
    ) {
        CouiBookTagScreen(
            onBack = onBack,
            onTagSearch = onTagSearch,
        )
    }

    @Composable
    override fun themeSetting(
        onBack: () -> Unit,
    ) {
        CouiThemeSettingScreen(onBack = onBack)
    }

    @Composable
    override fun accountManage(
        onBack: () -> Unit,
        onAddAccount: () -> Unit,
    ) {
        CouiAccountManageScreen(
            onBack = onBack,
            onAddAccount = onAddAccount,
        )
    }

    @Composable
    override fun novel(
        onBack: () -> Unit,
        onNovelClick: (Long) -> Unit,
        onUserClick: ((Long) -> Unit)?,
    ) {
        CouiNovelScreen(
            onBack = onBack,
            onNovelClick = onNovelClick,
            onUserClick = onUserClick,
        )
    }

    @Composable
    override fun settings(
        onBack: () -> Unit,
        onAboutClick: () -> Unit,
        onShieldClick: () -> Unit,
        onLoginClick: () -> Unit,
        onDownloadHistoryClick: () -> Unit,
        onThemeSettingClick: () -> Unit,
        onNetworkSettingClick: () -> Unit,
        onDownloadSettingClick: () -> Unit,
        onLayoutSettingClick: () -> Unit,
        onLanguageSettingClick: () -> Unit,
        onWidgetRecommendSettingClick: () -> Unit,
        onInteractionSettingClick: () -> Unit,
        onQualitySettingClick: () -> Unit,
        onCopyTextSettingClick: () -> Unit,
        onWelcomePageSettingClick: () -> Unit,
        onBookTagClick: () -> Unit,
        onUpdateSettingClick: () -> Unit,
        onAccountEditClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onDownloadTaskClick: () -> Unit,
        onDataExportClick: () -> Unit,
        onBoardClick: () -> Unit,
        onGuideClick: () -> Unit,
        onUserClick: (Long) -> Unit,
        onUserBookmarksClick: (Long) -> Unit,
        onAccountManageClick: () -> Unit,
        onNovelBrowseClick: () -> Unit,
    ) {
        CouiSettingsScreen(
            onBack = onBack,
            onAboutClick = onAboutClick,
            onShieldClick = onShieldClick,
            onLoginClick = onLoginClick,
            onDownloadHistoryClick = onDownloadHistoryClick,
            onThemeSettingClick = onThemeSettingClick,
            onNetworkSettingClick = onNetworkSettingClick,
            onDownloadSettingClick = onDownloadSettingClick,
            onLayoutSettingClick = onLayoutSettingClick,
            onLanguageSettingClick = onLanguageSettingClick,
            onWidgetRecommendSettingClick = onWidgetRecommendSettingClick,
            onInteractionSettingClick = onInteractionSettingClick,
            onQualitySettingClick = onQualitySettingClick,
            onCopyTextSettingClick = onCopyTextSettingClick,
            onWelcomePageSettingClick = onWelcomePageSettingClick,
            onBookTagClick = onBookTagClick,
            onUpdateSettingClick = onUpdateSettingClick,
            onAccountEditClick = onAccountEditClick,
            onHistoryClick = onHistoryClick,
            onDownloadTaskClick = onDownloadTaskClick,
            onDataExportClick = onDataExportClick,
            onBoardClick = onBoardClick,
            onGuideClick = onGuideClick,
            onUserClick = onUserClick,
            onUserBookmarksClick = onUserBookmarksClick,
            onAccountManageClick = onAccountManageClick,
            onNovelBrowseClick = onNovelBrowseClick,
        )
    }
}
