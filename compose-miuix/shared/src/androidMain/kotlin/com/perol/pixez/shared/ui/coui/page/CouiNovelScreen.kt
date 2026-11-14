package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.model.isLoggedIn
import com.perol.pixez.shared.data.repository.NovelRankingMode
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiPagingFooter
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.MiuixIcons

/** 小说浏览分类：推荐与排行榜。 */
private enum class CouiNovelBrowseTab {
    Recommend,
    Ranking,
}

/**
 * COUI 小说浏览页：固定顶栏 + 分类胶囊 + 小说列表。
 *
 * 推荐/排行榜切换、日/周/月榜切换、账号切换作废、触底预加载、
 * 手动刷新与三态占位全部保留；差异只有皮肤层：顶栏图标、分类胶囊、
 * 小说卡片与占位改用 COUI 组件，列表收敛到平板可读宽度。
 */
@Composable
internal fun CouiNovelScreen(
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
) {
    val novelRepository = LocalNovelRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(CouiNovelBrowseTab.Recommend) }
    var rankingMode by rememberSaveable { mutableStateOf(NovelRankingMode.Day) }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时作废旧账号列表并重拉首屏。
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user.isLoggedIn
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user.isLoggedIn
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.novelBrowseTitle,
            onBack = onBack,
        ) {
            CouiActionButton(
                icon = MiuixIcons.Refresh,
                contentDescription = strings.refresh,
                onClick = { refreshToken++ },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CouiAssistChip(
                text = strings.novelRecommend,
                selected = currentTab == CouiNovelBrowseTab.Recommend,
                onClick = {
                    if (currentTab != CouiNovelBrowseTab.Recommend) {
                        currentTab = CouiNovelBrowseTab.Recommend
                    }
                },
            )
            CouiAssistChip(
                text = strings.novelRanking,
                selected = currentTab == CouiNovelBrowseTab.Ranking,
                onClick = {
                    if (currentTab != CouiNovelBrowseTab.Ranking) {
                        currentTab = CouiNovelBrowseTab.Ranking
                    }
                },
            )
        }

        if (currentTab == CouiNovelBrowseTab.Ranking) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiAssistChip(
                    text = strings.rankingDay,
                    selected = rankingMode == NovelRankingMode.Day,
                    onClick = { rankingMode = NovelRankingMode.Day },
                )
                CouiAssistChip(
                    text = strings.rankingWeek,
                    selected = rankingMode == NovelRankingMode.Week,
                    onClick = { rankingMode = NovelRankingMode.Week },
                )
                CouiAssistChip(
                    text = strings.rankingMonth,
                    selected = rankingMode == NovelRankingMode.Month,
                    onClick = { rankingMode = NovelRankingMode.Month },
                )
            }
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            val novelItems = pagedList.items
            when {
                novelItems == null && pagedList.initialError == null -> CouiStatePlaceholder(
                    message = strings.loading,
                    modifier = Modifier.fillMaxSize(),
                )

                pagedList.initialError != null -> CouiStatePlaceholder(
                    message = couiErrorMessage(pagedList.initialError),
                    actionText = strings.retry,
                    onAction = { refreshToken++ },
                    modifier = Modifier.fillMaxSize(),
                )

                novelItems.isNullOrEmpty() -> CouiStatePlaceholder(
                    message = strings.noData,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    overscrollEffect = rememberCouiOverscrollEffect(),
                ) {
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
                    if (footerState.phase != com.perol.pixez.shared.ui.state.PagingFooterPhase.Hidden) {
                        item(key = "coui_novel_footer", contentType = "footer") {
                            CouiPagingFooter(
                                state = footerState,
                                onRetry = ::loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}
