package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.NovelRankingMode
import com.perol.pixez.shared.ui.coui.component.CouiSegmentedTabs
import com.perol.pixez.shared.ui.coui.component.CouiTopBarSegmentedTabs
import com.perol.pixez.shared.ui.coui.core.CouiErrorMessage
import com.perol.pixez.shared.ui.coui.core.CouiLoadMoreFooter
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberAccountSession
import kotlinx.coroutines.launch

/** 小说浏览分类：推荐与排行榜。 */
private enum class CouiNovelBrowseTab {
    Recommend,
    Ranking,
}

/**
 * COUI 小说浏览页：二级骨架 + 分类胶囊 + 小说列表。
 *
 * 推荐/排行榜切换、日/周/月榜切换、账号切换作废、触底预加载、
 * 手动刷新与三态占位全部保留；差异只有皮肤层：顶栏图标换成骨架槽位按钮、
 * 分类胶囊改作正文头部、占位与分页页脚改用 core 组件，底色由骨架承担。
 */
@Composable
internal fun CouiNovelScreen(
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
) {
    val novelRepository = LocalNovelRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(CouiNovelBrowseTab.Recommend) }
    var rankingMode by rememberSaveable { mutableStateOf(NovelRankingMode.Day) }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时刷新，供页面作废旧账号数据并重拉首屏。
    val (user, isLoggedIn) = rememberAccountSession()

    // 分页状态机单点实现（含 generation 竞态守卫）；tab/榜单切换以身份键重建。
    val pagedList = remember(currentTab, rankingMode) {
        val tab = currentTab
        val mode = rankingMode
        PagedListState(
            fetch = { nextUrl ->
                val response = when (tab) {
                    CouiNovelBrowseTab.Recommend ->
                        novelRepository.getRecommendedNovels(nextUrl = nextUrl)

                    CouiNovelBrowseTab.Ranking ->
                        novelRepository.getNovelRanking(mode = mode, nextUrl = nextUrl)
                }
                PagedListPage(response.novels, response.nextUrl)
            },
            mergeItems = { old, new -> old.appendDistinct(new) },
        )
    }

    // 账号身份跟踪：登录/登出/切号时作废旧账号列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(currentTab, rankingMode, refreshToken, isLoggedIn, user) {
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        pagedList.loadFirst(force = refreshToken > 0)
    }

    val listState = rememberLazyListState()

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    // 触底自动加载更多：状态机内四条件守卫，翻页失败时自动加载停止。
    LaunchedEffect(listState, pagedList) {
        snapshotFlow {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
            )
        }.collect { shouldLoad ->
            if (shouldLoad) {
                loadMore()
            }
        }
    }

    val mainTabLabels = remember(strings) {
        listOf(strings.novelRecommend, strings.novelRanking)
    }

    CouiSecondaryScaffold(
        title = strings.novelBrowseTitle,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                icon = CouiIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = { refreshToken++ },
            )
        },
        topBarBottom = {
            CouiTopBarSegmentedTabs(
                items = mainTabLabels,
                selectedIndex = if (currentTab == CouiNovelBrowseTab.Ranking) 1 else 0,
                onSelect = { idx ->
                    val next = if (idx == 1) CouiNovelBrowseTab.Ranking else CouiNovelBrowseTab.Recommend
                    if (currentTab != next) currentTab = next
                },
            )
        },
    ) { padding ->
        val header: @Composable () -> Unit = {
            if (currentTab == CouiNovelBrowseTab.Ranking) {
                CouiNovelRankingPeriodHeader(
                    rankingMode = rankingMode,
                    onRankingModeChange = { rankingMode = it },
                )
            }
        }
        val novelItems = pagedList.items
        when {
            novelItems == null && pagedList.initialError == null -> CouiNovelStateBody(
                padding = padding,
                message = strings.loading,
                header = header,
            )

            pagedList.initialError != null -> CouiNovelStateBody(
                padding = padding,
                message = CouiErrorMessage(pagedList.initialError),
                actionText = strings.retry,
                onAction = { refreshToken++ },
                header = header,
            )

            novelItems.isNullOrEmpty() -> CouiNovelStateBody(
                padding = padding,
                message = strings.noData,
                header = header,
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 12.dp,
                    bottom = padding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                // 分类胶囊随列表滚动：切 tab 后列表会重建，胶囊仍在首屏顶部。
                item(key = "coui_novel_browse_header", contentType = "header") {
                    header()
                }

                items(
                    items = novelItems,
                    key = { it.id },
                    contentType = { "novel_item" },
                ) { novel ->
                    CouiNovelCard(
                        novel = novel,
                        onClick = { onNovelClick(novel.id) },
                        onUserClick = onUserClick,
                    )
                }

                val footerState = pagedList.footerState
                if (footerState.phase != PagingFooterPhase.Hidden) {
                    item(key = "coui_novel_footer", contentType = "footer") {
                        CouiLoadMoreFooter(
                            state = footerState,
                            onRetry = ::loadMore,
                        )
                    }
                }
            }
        }
    }
}

/** 排行榜周期分段切换（日 / 周 / 月）。 */
@Composable
private fun CouiNovelRankingPeriodHeader(
    rankingMode: NovelRankingMode,
    onRankingModeChange: (NovelRankingMode) -> Unit,
) {
    val strings = LocalStrings.current
    val modes = remember { listOf(NovelRankingMode.Day, NovelRankingMode.Week, NovelRankingMode.Month) }
    val labels = remember(strings) { listOf(strings.rankingDay, strings.rankingWeek, strings.rankingMonth) }
    CouiSegmentedTabs(
        items = labels,
        selectedIndex = modes.indexOf(rankingMode).coerceAtLeast(0),
        onSelect = { idx -> modes.getOrNull(idx)?.let(onRankingModeChange) },
        small = true,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/** 占位态正文：分类胶囊固定在最上方，占位卡片吃掉剩余高度。 */
@Composable
private fun CouiNovelStateBody(
    padding: PaddingValues,
    message: String,
    header: @Composable () -> Unit,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            .padding(horizontal = 12.dp),
    ) {
        header()
        CouiPlaceholder(
            message = message,
            modifier = Modifier.weight(1f),
            actionText = actionText,
            onAction = onAction,
        )
    }
}
