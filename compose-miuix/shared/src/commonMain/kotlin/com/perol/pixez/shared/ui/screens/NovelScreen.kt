package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.repository.NovelRankingMode
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.LocalNovelRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.BlurTopBarScaffold
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.NovelCard
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagedListPage
import com.perol.pixez.shared.ui.state.PagedListState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class NovelBrowseTab {
    Recommend,
    Ranking,
}

/**
 * 小说浏览页：提供推荐与排行榜两类作品流，
 * 遵循 Xiaomi HyperOS / MIUIX 规范，支持多榜单切换、平滑滚动、自适应分页拉取与阅读器跳转。
 */
@Composable
fun NovelScreen(
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 中-9：数据仓储统一经 CompositionLocal 注入（RootContent 根部 provide）。
    val novelRepository = LocalNovelRepository.current
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val backdrop = rememberBlurBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    var currentTab by rememberSaveable { mutableStateOf(NovelBrowseTab.Recommend) }
    var rankingMode by rememberSaveable { mutableStateOf(NovelRankingMode.Day) }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }

    // 登录状态与账号身份：登录/登出/切号（loginEventFlow）到达时作废旧账号列表并重拉首屏。
    var user by remember { mutableStateOf<AccountPersist?>(null) }
    var isLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        // 挂起上下文读取当前账号：suspendRunCatchingNonCancel 捕获异常并保留取消语义（同 NewScreen）。
        user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
        isLoggedIn = user != null
        accountRepository.loginEventFlow.collect {
            user = suspendRunCatchingNonCancel { accountRepository.currentAccount() }.getOrNull()
            isLoggedIn = user != null
        }
    }

    // 分页状态机单点实现（含 generation 竞态守卫）；tab/榜单切换以身份键重建，等效旧代次自增。
    val pagedList = remember(currentTab, rankingMode) {
        val tab = currentTab
        val mode = rankingMode
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

    // 账号身份跟踪：登录/登出/切号（loginEventFlow 更新 user/isLoggedIn）时作废旧账号列表并重拉首屏。
    val accountUnloaded = remember { Any() }
    var loadedForAccount by remember { mutableStateOf<Any?>(accountUnloaded) }
    LaunchedEffect(currentTab, rankingMode, refreshToken, isLoggedIn, user) {
        // 登录态未检测完成（null）时不发起加载，避免账号解析前后误判为账号切换。
        if (isLoggedIn == null) return@LaunchedEffect
        val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user
        if (accountSwitched) pagedList.reset()
        loadedForAccount = user
        // refreshToken 变化（手动刷新/重试）时强制重新拉取首屏。
        pagedList.loadFirst(force = refreshToken > 0)
    }

    val listState = rememberLazyListState()

    fun loadMore() {
        coroutineScope.launch { pagedList.loadMore() }
    }

    // 触底自动加载更多：使用 snapshotFlow 监听滚动可视项位置，消除原 LaunchedEffect key 翻转导致的协程取消与分页死锁
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS
        }.collect { shouldLoad ->
            if (shouldLoad && !pagedList.isLoadingMore && pagedList.nextUrl != null && pagedList.loadMoreError == null) {
                loadMore()
            }
        }
    }

    BlurTopBarScaffold(
        modifier = modifier.fillMaxSize(),
        title = strings.novelBrowseTitle,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
        actions = {
            IconButton(onClick = { refreshToken++ }) {
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
                // 顶部分类切换药丸导航栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NovelTabChip(
                        title = strings.novelRecommend,
                        selected = currentTab == NovelBrowseTab.Recommend,
                        onClick = {
                            if (currentTab != NovelBrowseTab.Recommend) {
                                currentTab = NovelBrowseTab.Recommend
                            }
                        },
                    )

                    NovelTabChip(
                        title = strings.novelRanking,
                        selected = currentTab == NovelBrowseTab.Ranking,
                        onClick = {
                            if (currentTab != NovelBrowseTab.Ranking) {
                                currentTab = NovelBrowseTab.Ranking
                            }
                        },
                    )

                    if (currentTab == NovelBrowseTab.Ranking) {
                        Spacer(modifier = Modifier.weight(1f))
                        // 排行榜模式快速切换
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NovelSubModeChip(
                                title = strings.rankingDay,
                                selected = rankingMode == NovelRankingMode.Day,
                                onClick = { rankingMode = NovelRankingMode.Day },
                            )
                            NovelSubModeChip(
                                title = strings.rankingWeek,
                                selected = rankingMode == NovelRankingMode.Week,
                                onClick = { rankingMode = NovelRankingMode.Week },
                            )
                            NovelSubModeChip(
                                title = strings.rankingMonth,
                                selected = rankingMode == NovelRankingMode.Month,
                                onClick = { rankingMode = NovelRankingMode.Month },
                            )
                        }
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
                            onRetry = { refreshToken++ },
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
                                )
                            }

                            if (pagedList.isLoadingMore) {
                                item(key = "footer_loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        InfiniteProgressIndicator(
                                            color = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
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
}

@Composable
private fun NovelTabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.secondaryContainer
    val textColor = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
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
) {
    val bg = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
    val textColor = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Box(
        modifier = Modifier
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
