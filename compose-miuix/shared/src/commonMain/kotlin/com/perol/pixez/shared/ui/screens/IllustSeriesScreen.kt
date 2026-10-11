package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.filterIllustsBySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.state.IllustPagedListState
import com.perol.pixez.shared.ui.state.IllustPagedPage
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.IllustStaggeredGrid
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.icon.extended.Refresh

/**
 * 插画系列页：展示指定系列内的作品列表，支持流式分页与下拉刷新。
 */
@Composable
fun IllustSeriesScreen(
    seriesId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    repository: IllustRepository,
    banRepository: BanRepository,
    settingsRepository: SettingsRepository,
) {
    var retryCount by rememberSaveable(seriesId) { mutableIntStateOf(0) }
    var isManualRefreshing by rememberSaveable(seriesId) { mutableStateOf(false) }

    val strings = LocalStrings.current

    // 系列 ID 变更时重建状态机并重置标题；翻页响应不含标题更新。
    var seriesTitle by remember(seriesId) { mutableStateOf("") }
    // 高-4c：分页/竞态/加载态收敛到 IllustPagedListState（generation 守卫语义），
    // 过滤链为高-3「原始/过滤」双列表本地重过滤。系列标题在 fetch 闭包内仅于首页请求（nextUrl == null）
    // 时更新：切系列时 remember(seriesId) 重建闭包，旧闭包写入的是旧 State 对象，不污染新页面。
    val listState = remember(seriesId) {
        IllustPagedListState(
            fetch = { nextUrl ->
                val response = repository.getIllustSeriesResponse(seriesId, nextUrl = nextUrl)
                if (nextUrl == null) {
                    seriesTitle = response.illustSeriesDetail?.title ?: ""
                }
                IllustPagedPage(response.illusts.orEmpty(), response.nextUrl)
            },
            filter = { rawIllusts -> banRepository.filterIllustsBySettings(rawIllusts, settingsRepository) },
            filterVersionProvider = { settingsRepository.filterChangeVersion },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // 系列 ID 变更、重试或手动刷新时重新加载作品；过滤条件变化仅本地重过滤（高-3）。
    LaunchedEffect(seriesId, retryCount, settingsRepository.filterChangeVersion) {
        val force = isManualRefreshing
        if (!force && listState.refilterIfChanged()) return@LaunchedEffect
        if (!force && listState.filteredIllusts != null && listState.initialError == null) {
            return@LaunchedEffect
        }
        val previous = listState.filteredIllusts
        listState.loadFirst(force = force)
        isManualRefreshing = false
        // 引用变化 = 本轮成功替换了列表（失败与竞态失配时状态机不写入）；force 刷新成功后滚回顶部。
        val current = listState.filteredIllusts
        if (current != null && current !== previous && force && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    fun loadMore() {
        // 无下一页、在途与竞态守卫均由状态机内部处理。
        coroutineScope.launch {
            listState.loadMore()
        }
    }

    val triggerManualRefresh: () -> Unit = {
        isManualRefreshing = true
        retryCount++
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = seriesTitle,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = strings.back,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = triggerManualRefresh) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = strings.refresh,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
        ) {
            val currentIllusts = listState.filteredIllusts
            when {
                currentIllusts == null && listState.initialError == null -> LoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts == null && listState.initialError != null -> ErrorPlaceholder(
                    error = listState.initialError,
                    onRetry = { triggerManualRefresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
                currentIllusts != null -> {
                    if (currentIllusts.isEmpty()) {
                        EmptyPlaceholder(
                            message = strings.seriesEmpty,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    } else {
                        PullToRefresh(
                            isRefreshing = isManualRefreshing,
                            onRefresh = triggerManualRefresh,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                            topAppBarScrollBehavior = scrollBehavior,
                        ) {
                            IllustStaggeredGrid(
                                illusts = currentIllusts,
                                onIllustClick = onIllustClick,
                                state = gridState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    top = paddingValues.calculateTopPadding() + 8.dp,
                                    end = 8.dp,
                                    bottom = LocalBottomBarContentPadding.current,
                                ),
                                hasMore = listState.nextUrl != null,
                                isLoadingMore = listState.isLoadingMore,
                                loadMoreError = listState.loadMoreError,
                                onLoadMore = ::loadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}

