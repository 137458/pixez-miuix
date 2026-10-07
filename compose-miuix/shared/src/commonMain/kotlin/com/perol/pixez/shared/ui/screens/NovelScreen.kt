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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.data.model.Novel
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.data.repository.NovelRankingMode
import com.perol.pixez.shared.data.repository.NovelRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.NovelCard
import com.perol.pixez.shared.ui.components.blurBackdropSource
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
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
    novelRepository: NovelRepository,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val backdrop = rememberBlurBackdrop()

    var currentTab by rememberSaveable { mutableStateOf(NovelBrowseTab.Recommend) }
    var rankingMode by rememberSaveable { mutableStateOf(NovelRankingMode.Day) }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }

    var novels by remember { mutableStateOf<List<Novel>?>(null) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var initialError by remember { mutableStateOf<Throwable?>(null) }

    // 请求代次守卫：切 tab/切榜单/刷新后，在途旧请求的结果不得写回新数据源（写法同 RankingScreen）。
    var requestGeneration by remember { mutableIntStateOf(0) }

    fun loadInitialData() {
        val generation = ++requestGeneration
        val tab = currentTab
        val mode = rankingMode
        coroutineScope.launch {
            isLoading = true
            initialError = null
            // 使在途旧 loadMore 的结果失配后被拒绝；复位加载态避免其卡死阻塞新列表分页。
            isLoadingMore = false
            try {
                suspendRunCatchingNonCancel {
                    when (tab) {
                        NovelBrowseTab.Recommend -> novelRepository.getRecommendedNovels()
                        NovelBrowseTab.Ranking -> novelRepository.getNovelRanking(mode = mode)
                    }
                }.fold(
                    onSuccess = { response ->
                        if (generation == requestGeneration) {
                            novels = response.novels
                            nextUrl = response.nextUrl
                        }
                    },
                    onFailure = { error ->
                        if (generation == requestGeneration) {
                            initialError = error
                        }
                    },
                )
            } finally {
                // 取消或异常时同样复位，避免列表被永久标记为加载中。
                if (generation == requestGeneration) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(currentTab, rankingMode, refreshToken) {
        loadInitialData()
    }

    val listState = rememberLazyListState()

    fun loadMore() {
        val currentNextUrl = nextUrl ?: return
        if (isLoadingMore) return
        val generation = requestGeneration
        val tab = currentTab
        val mode = rankingMode
        coroutineScope.launch {
            isLoadingMore = true
            suspendRunCatchingNonCancel {
                when (tab) {
                    NovelBrowseTab.Recommend -> novelRepository.getRecommendedNovels(nextUrl = currentNextUrl)
                    NovelBrowseTab.Ranking -> novelRepository.getNovelRanking(mode = mode, nextUrl = currentNextUrl)
                }
            }.fold(
                onSuccess = { response ->
                    if (generation == requestGeneration) {
                        novels = (novels.orEmpty()).appendDistinct(response.novels)
                        nextUrl = response.nextUrl
                    }
                },
                onFailure = {
                    // 保留现有列表，网络失败后允许再次触发
                },
            )
            if (generation == requestGeneration) {
                isLoadingMore = false
            }
        }
    }

    // 触底自动加载更多：使用 snapshotFlow 监听滚动可视项位置，消除原 LaunchedEffect key 翻转导致的协程取消与分页死锁
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS
        }.collect { shouldLoad ->
            if (shouldLoad && !isLoadingMore && !nextUrl.isNullOrBlank()) {
                loadMore()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop) {
                TopAppBar(
                    title = strings.novelBrowseTitle,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { refreshToken++ }) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = strings.refresh,
                            )
                        }
                    },
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
                    isLoading -> {
                        LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                    }
                    initialError != null -> {
                        ErrorPlaceholder(
                            error = initialError,
                            onRetry = { refreshToken++ },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    novels.isNullOrEmpty() -> {
                        EmptyPlaceholder(
                            message = strings.noData,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        val novelList = novels.orEmpty()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
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

                            if (isLoadingMore) {
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
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}
