package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 插画瀑布流网格。
 *
 * 支持大屏响应式多列自适应、触底自动流式加载下一页、加载中指示器与重试。
 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun IllustStaggeredGrid(
    illusts: List<Illust>,
    onIllustClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    columns: StaggeredGridCells? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = 8.dp,
        top = 8.dp,
        end = 8.dp,
        bottom = 100.dp,
    ),
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: Throwable? = null,
    onLoadMore: (() -> Unit)? = null,
    onRetryLoadMore: (() -> Unit)? = onLoadMore,
) {
    val settings = LocalSettingsRepository.current
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val isLandscape = maxWidth > maxHeight
        val effectiveColumns = remember(
            columns,
            isLandscape,
            settings?.crossAdapt,
            settings?.crossAdapterWidth,
            settings?.crossCount,
            settings?.hCrossAdapt,
            settings?.hCrossAdapterWidth,
            settings?.hCrossCount,
            settings?.changeVersion,
        ) {
            if (columns != null) {
                columns
            } else if (isLandscape) {
                if (settings?.hCrossAdapt == true) {
                    val minWidth = settings.hCrossAdapterWidth.coerceIn(100, 1000)
                    StaggeredGridCells.Adaptive(minWidth.dp)
                } else {
                    val configuredCols = settings?.hCrossCount ?: 2
                    StaggeredGridCells.Fixed(configuredCols.coerceIn(1, 8))
                }
            } else {
                if (settings?.crossAdapt == true) {
                    val minWidth = settings.crossAdapterWidth.coerceIn(100, 1000)
                    StaggeredGridCells.Adaptive(minWidth.dp)
                } else {
                    val configuredCols = settings?.crossCount ?: 2
                    StaggeredGridCells.Fixed(configuredCols.coerceIn(1, 8))
                }
            }
        }

        // 触底自动流式加载监听：当滑动到接近底部（倒数 6 个作品内）时自动触发下一页请求。
        if (onLoadMore != null) {
            val shouldLoadMore by remember(hasMore, isLoadingMore, loadMoreError, illusts.size) {
                derivedStateOf {
                    if (!hasMore || isLoadingMore || loadMoreError != null || illusts.isEmpty()) {
                        false
                    } else {
                        val layoutInfo = state.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleIndex = layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
                        lastVisibleIndex >= totalItems - 6
                    }
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
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            items(
                items = illusts,
                key = { it.id },
                contentType = { "illust_card" },
            ) { illust ->
                IllustCard(
                    illust = illust,
                    onClick = { onIllustClick(illust.id) },
                )
            }

        // 底部加载更多状态区（跨整行展示）
        if (isLoadingMore || loadMoreError != null || (!hasMore && illusts.isNotEmpty() && onLoadMore != null)) {
            item(
                key = "load_more_footer",
                span = StaggeredGridItemSpan.FullLine,
                contentType = "load_more_footer",
            ) {
                val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isLoadingMore -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                InfiniteProgressIndicator(modifier = Modifier.size(20.dp))
                                Text(
                                    text = strings.loadingMore,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                        loadMoreError != null -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = strings.loadMoreFailedRetry,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.error,
                                )
                                TextButton(
                                    text = strings.retry,
                                    onClick = { (onRetryLoadMore ?: onLoadMore)?.invoke() },
                                )
                            }
                        }
                        !hasMore && illusts.isNotEmpty() -> {
                            Text(
                                text = strings.noMoreData,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }
    }

        VerticalScrollBar(
            adapter = rememberStaggeredGridScrollBarAdapter(state),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            trackPadding = contentPadding,
        )
    }
}


