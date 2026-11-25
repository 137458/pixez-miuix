package com.perol.pixez.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.repository.NovelRankingMode
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.navigation.animation.transitionTopBarOccluder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.NovelCard
import com.perol.pixez.shared.ui.components.PagingFooterAppearance
import com.perol.pixez.shared.ui.components.PagingLoadMoreFooter
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.components.rememberPagingPreload
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.NovelBrowsePageState
import com.perol.pixez.shared.ui.state.NovelBrowseTab
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.rememberAccountSession
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 小说浏览页：提供推荐与排行榜两类作品流，
 * 遵循 Xiaomi HyperOS / MIUIX 规范，支持多榜单切换、平滑滚动、自适应分页拉取与阅读器跳转；
 * 状态容器 [pageState] 挂在导航栈 InstanceKeeper 上，切 Tab 或从阅读器返回不重拉首屏。
 */
@Composable
fun NovelScreen(
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: ((Long) -> Unit)? = null,
    pageState: NovelBrowsePageState = remember { NovelBrowsePageState() },
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val novelRepository = LocalNovelRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val backdrop = rememberBlurBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    val currentTab = pageState.currentTab
    val rankingMode = pageState.rankingMode
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时刷新，供页面作废旧账号数据并重拉首屏。
    val (user, isLoggedIn) = rememberAccountSession()

    // 分页状态机由 pageState 按 (tab, mode) 持久池化，切 Tab 或阅读器退栈返回直接复用既有数据。
    val pagedList = remember(pageState, currentTab, rankingMode) {
        val tab = currentTab
        val mode = rankingMode
        pageState.getOrCreateList(tab, mode) {
            PagedListState(
                fetch = { nextUrl ->
                    val response = when (tab) {
                        NovelBrowseTab.Recommend -> novelRepository.getRecommendedNovels(nextUrl = nextUrl)
                        NovelBrowseTab.Ranking -> novelRepository.getNovelRanking(mode = mode, nextUrl = nextUrl)
                    }
                    PagedListPage(response.novels, response.nextUrl)
                },
                mergeItems = { old, new -> old.appendDistinct(new) },
            )
        }
    }

    // 账号身份跟踪：仅登录/登出/切号时作废旧账号列表；已有数据直接缓存早退，不重拉首屏。
    LaunchedEffect(pageState, currentTab, rankingMode, refreshTrigger, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val loadedForAccount = pageState.loadedAccount
        val accountSwitched = pageState.hasLoadedAccount && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        if (accountSwitched) {
            pageState.resetAll()
            pageState.putList(currentTab, rankingMode, pagedList)
        }
        pageState.hasLoadedAccount = true
        pageState.loadedAccount = user
        val forceRefresh = pageState.pendingForceRefresh
        pageState.pendingForceRefresh = false
        pagedList.loadFirst(force = forceRefresh)
    }

    val listState = key(pageState, currentTab, rankingMode) {
        val savedScroll = remember(pageState, currentTab, rankingMode) {
            pageState.getScrollPosition(currentTab, rankingMode)
        }
        rememberLazyListState(
            initialFirstVisibleItemIndex = savedScroll.index,
            initialFirstVisibleItemScrollOffset = savedScroll.offset,
        )
    }
    DisposableEffect(pageState, currentTab, rankingMode, listState) {
        onDispose {
            pageState.saveScrollPosition(
                tab = currentTab,
                rankingMode = rankingMode,
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset,
            )
        }
    }

    fun triggerRefresh() {
        pageState.pendingForceRefresh = true
        refreshTrigger++
    }

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    // 触底自动加载更多：四条件守卫与阈值单源在状态机内（UI-2），
    // 统一入口内部以派生状态监听可视项位置，末尾进入预加载区间时回调。
    rememberPagingPreload(pagedList = pagedList, listState = listState, onLoadMore = ::loadMore)

    BlurTopBarScaffold(
        modifier = modifier.fillMaxSize(),
        title = strings.novelBrowseTitle,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            IconButton(onClick = ::triggerRefresh) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = strings.refresh,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .blurBackdropSource(backdrop),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部分类切换：主 Tab 等宽铺满一行；榜单周期单独成行，长文案语言下不再挤压溢出。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .transitionTopBarOccluder(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NovelTabChip(
                        title = strings.novelRecommend,
                        selected = currentTab == NovelBrowseTab.Recommend,
                        onClick = {
                            if (currentTab != NovelBrowseTab.Recommend) {
                                pageState.currentTab = NovelBrowseTab.Recommend
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )

                    NovelTabChip(
                        title = strings.novelRanking,
                        selected = currentTab == NovelBrowseTab.Ranking,
                        onClick = {
                            if (currentTab != NovelBrowseTab.Ranking) {
                                pageState.currentTab = NovelBrowseTab.Ranking
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // 榜单周期：仅排行 Tab 出现，日 / 周两段等宽，与主 Tab 分行互不挤压。
                AnimatedVisibility(visible = currentTab == NovelBrowseTab.Ranking) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .transitionTopBarOccluder(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NovelSubModeChip(
                            title = strings.rankingDay,
                            selected = rankingMode == NovelRankingMode.Day,
                            onClick = { pageState.rankingMode = NovelRankingMode.Day },
                            modifier = Modifier.weight(1f),
                        )
                        NovelSubModeChip(
                            title = strings.rankingWeek,
                            selected = rankingMode == NovelRankingMode.Week,
                            onClick = { pageState.rankingMode = NovelRankingMode.Week },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 内容区：加载 / 失败 / 空 / 列表展示
                when {
                    pagedList.items == null && pagedList.initialError == null -> {
                        LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    }
                    pagedList.initialError != null -> {
                        ErrorPlaceholder(
                            error = pagedList.initialError,
                            onRetry = ::triggerRefresh,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    pagedList.items.isNullOrEmpty() -> {
                        EmptyPlaceholder(
                            message = strings.noData,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        val novelList = pagedList.items.orEmpty()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = LocalBottomBarContentPadding.current,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(
                                items = novelList,
                                key = { _, item -> item.id },
                            ) { _, novel ->
                                NovelCard(
                                    novel = novel,
                                    onClick = { onNovelClick(novel.id) },
                                    onUserClick = onUserClick,
                                )
                            }

                            // 三态 footer 收敛到统一组件（UI-3）：加载中 / 翻页失败重试 / 没有更多。
                            // 翻页失败后自动预加载会停（hasLoadMoreError 守卫），必须提供重试入口。
                            val footerState = pagedList.footerState
                            if (footerState.phase != PagingFooterPhase.Hidden) {
                                item(key = "novel_footer") {
                                    PagingLoadMoreFooter(
                                        state = footerState,
                                        onRetry = ::loadMore,
                                        appearance = PagingFooterAppearance.Compact,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelTabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.secondaryContainer
    val textColor = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = textColor,
        )
    }
}

@Composable
private fun NovelSubModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
    val textColor = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote1.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = textColor,
        )
    }
}
