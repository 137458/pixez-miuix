package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState
import com.perol.pixez.shared.ui.state.shouldPreloadMore
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/**
 * 插画瀑布流网格。
 *
 * 支持大屏响应式多列自适应、触底自动流式加载下一页、加载中指示器与重试。
 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun IllustStaggeredGrid(
    illusts: List<Illust>,
    onIllustClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    columns: StaggeredGridCells? = null,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: Throwable? = null,
    onLoadMore: (() -> Unit)? = null,
    // 默认取同一次调用传入的 onLoadMore：Compose 对默认表达式按实参记忆化，onLoadMore 变化时默认值同步更新，
    // 不会固化上一次的引用；仅当重试动作与加载动作不同（如需要先清错误态）才需要显式传入。
    onRetryLoadMore: (() -> Unit)? = onLoadMore,
) {
    val settings = LocalSettingsRepository.current
    val changeVersion = settings.changeVersion.collectAsState().value
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val effectiveContentPadding = contentPadding ?: PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = LocalBottomBarContentPadding.current,
        )
        val isLandscape = maxWidth > maxHeight
        // 列数换算收敛到 UI-9 的纯函数：横竖屏各取一套布局设置，策略参数见 IllustGridColumnPolicy。
        val effectiveColumns = remember(
            columns,
            isLandscape,
            settings.crossAdapt,
            settings.crossAdapterWidth,
            settings.crossCount,
            settings.hCrossAdapt,
            settings.hCrossAdapterWidth,
            settings.hCrossCount,
            changeVersion,
        ) {
            columns ?: resolveGridColumns(
                adapt = if (isLandscape) settings.hCrossAdapt else settings.crossAdapt,
                adapterWidthDp = if (isLandscape) settings.hCrossAdapterWidth else settings.crossAdapterWidth,
                configuredCount = if (isLandscape) settings.hCrossCount else settings.crossCount,
                policy = IllustGridColumnPolicy,
            ).toStaggeredGridCells()
        }

        // 触底自动流式加载监听：当滑动到接近底部（倒数 6 个作品内，UI-2 阈值单源）时自动触发下一页请求。
        if (onLoadMore != null) {
            // derivedStateOf 一旦创建就把参数引用固化在闭包里，故闭包读到的每个参数都要进键：
            // 用 illusts 本身而不是 illusts.size，避免刷新后条数相同、内容整体替换时仍按旧列表判定。
            val shouldLoadMore by remember(
                hasMore,
                isLoadingMore,
                loadMoreError,
                illusts,
                state,
            ) {
                derivedStateOf {
                    shouldPreloadMore(
                        itemCount = illusts.size,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        hasLoadMoreError = loadMoreError != null,
                        lastVisibleItemIndex = state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
                        thresholdItems = AppConstants.Layout.PRELOAD_THRESHOLD_STAGGERED_ITEMS,
                    )
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) {
                    onLoadMore()
                }
            }
        }

        LazyVerticalStaggeredGrid(
            columns = effectiveColumns,
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = effectiveContentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            if (header != null) {
                item(
                    key = "grid_custom_header",
                    span = StaggeredGridItemSpan.FullLine,
                    contentType = "grid_custom_header",
                ) {
                    header()
                }
            }
            items(
                items = illusts,
                key = { it.id },
                contentType = { "illust_card" },
            ) { illust ->
                val onCardClick = remember(illust.id) { { onIllustClick(illust.id) } }
                IllustCard(
                    illust = illust,
                    onClick = onCardClick,
                )
            }

        // 底部加载更多状态区（跨整行展示）：三态判定与呈现收敛到统一 footer（UI-3）。
        val footerState = PagingFooterState(
            hasMore = hasMore,
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
            hasItems = illusts.isNotEmpty(),
        )
        if (onLoadMore != null && footerState.phase != PagingFooterPhase.Hidden) {
            item(
                key = "load_more_footer",
                span = StaggeredGridItemSpan.FullLine,
                contentType = "load_more_footer",
            ) {
                PagingLoadMoreFooter(
                    state = footerState,
                    onRetry = { (onRetryLoadMore ?: onLoadMore)?.invoke() },
                )
            }
        }
    }

        VerticalScrollBar(
            adapter = rememberStaggeredGridScrollBarAdapter(state),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            trackPadding = effectiveContentPadding,
        )
    }
}


