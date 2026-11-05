package com.perol.pixez.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.perol.pixez.shared.ui.i18n.AppStrings
import com.perol.pixez.shared.ui.screens.HelloScreen
import com.perol.pixez.shared.ui.screens.NewScreen
import com.perol.pixez.shared.ui.screens.RankingScreen
import com.perol.pixez.shared.ui.screens.SearchScreen
import com.perol.pixez.shared.ui.screens.SpotlightScreen
import com.perol.pixez.shared.ui.state.MainFeedRestore
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 单个一级标签的目录条目（nav13）：文案、图标、徽标条件与 Pager 页内容同表声明。
 *
 * 底栏 [MainBottomBar]、侧栏 [MainNavigationRail] 与 Pager（[MainContent]）共读本表，
 * 新增标签只需在 [MAIN_TAB_DESCRIPTORS] 加一行，不再逐处补表或补 when 分支。
 */
internal data class MainTabDescriptor(
    val tab: RootComponent.MainTab,
    /** 标签文案选择器：文案随语言变化，故按当前语言表求值而非存字符串字面量。 */
    val label: (AppStrings) -> String,
    val icon: ImageVector,
    /** 该标签是否承载「未读动态」徽标（本项目仅动态页承载）。 */
    val carriesUnreadFeedBadge: Boolean = false,
    /** Pager 页内容：参数与 [MainContent] 手头持有的跨页恢复容器一致。 */
    val content: @Composable (RootComponent, MainFeedRestore) -> Unit,
)

/**
 * 一级标签目录：顺序即 [RootComponent.MAIN_TAB_ORDER]，底栏/侧栏/Pager 共用同一份声明。
 */
internal val MAIN_TAB_DESCRIPTORS: List<MainTabDescriptor> = listOf(
    MainTabDescriptor(
        tab = RootComponent.MainTab.Hello,
        label = { it.tabRecommend },
        icon = MiuixIcons.All,
        content = { component, feedRestore ->
            HelloScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
                onSettingsClick = component::onSettingsClicked,
                onLoginClick = component::onLoginClicked,
                onRecomUserClick = component::onRecomUserListClicked,
                feedRestore = feedRestore.hello,
                reselectFlow = remember(component) {
                    component.reselectFlowOf(RootComponent.MainTab.Hello)
                },
            )
        },
    ),
    MainTabDescriptor(
        tab = RootComponent.MainTab.Search,
        label = { it.tabSearch },
        icon = MiuixIcons.Search,
        content = { component, _ ->
            SearchScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
                onNovelClick = component::onNovelClicked,
            )
        },
    ),
    MainTabDescriptor(
        tab = RootComponent.MainTab.Ranking,
        label = { it.tabRanking },
        icon = MiuixIcons.TopDownloads,
        content = { component, feedRestore ->
            RankingScreen(
                onIllustClick = component::onIllustClicked,
                feedRestore = feedRestore.ranking,
            )
        },
    ),
    MainTabDescriptor(
        tab = RootComponent.MainTab.New,
        label = { it.tabNew },
        icon = MiuixIcons.Recent,
        carriesUnreadFeedBadge = true,
        content = { component, _ ->
            NewScreen(
                onIllustClick = component::onIllustClicked,
                onUserClick = component::onUserClicked,
                onLoginClick = component::onLoginClicked,
                onSeriesClick = component::onIllustSeriesClicked,
                reselectFlow = remember(component) {
                    component.reselectFlowOf(RootComponent.MainTab.New)
                },
            )
        },
    ),
    MainTabDescriptor(
        tab = RootComponent.MainTab.Spotlight,
        label = { it.tabSpotlight },
        icon = MiuixIcons.Promotions,
        content = { component, _ ->
            SpotlightScreen(
                onArticleClick = component::onSpotlightArticleClicked,
            )
        },
    ),
)

/**
 * 未读动态徽标判定（底栏与侧栏唯一来源，nav13）：只有承载徽标的标签在「非当前页且存在未读」时显示。
 *
 * @param activeTab 当前选中的一级标签；停留在承载徽标的标签上时不显示徽标。
 * @param hasUnreadFeedBadge 设置仓储上报的未读动态标记。
 */
internal fun shouldShowUnreadFeedBadge(
    descriptor: MainTabDescriptor,
    activeTab: RootComponent.MainTab,
    hasUnreadFeedBadge: Boolean,
): Boolean = descriptor.carriesUnreadFeedBadge && hasUnreadFeedBadge && activeTab != descriptor.tab
