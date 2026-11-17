package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.rememberPagingPreload
import com.perol.pixez.shared.ui.i18n.LocalStrings

/** 作品瀑布流：网格布局、预加载门槛与加载更多页脚在此，卡片本体见 CouiIllustCard.kt。 */

/** COUI 瀑布流：自适应列宽、触底预加载与三态 footer。 */
@Composable
internal fun CouiIllustGrid(
    illusts: List<Illust>,
    state: LazyStaggeredGridState,
    onIllustClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    header: (@Composable () -> Unit)? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: Throwable? = null,
    onLoadMore: (() -> Unit)? = null,
) {
    // 设置变更版本由列表统一收集一次：卡片只做无状态渲染，避免每张卡片各挂一个收集器。
    val changeVersion = LocalSettingsRepository.current.changeVersion.collectAsState().value

    if (onLoadMore != null) {
        rememberPagingPreload(
            itemCount = illusts.size,
            hasMore = hasMore,
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
            lastVisibleItemIndex = { state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1 },
            thresholdItems = AppConstants.Layout.PRELOAD_THRESHOLD_STAGGERED_ITEMS,
            onLoadMore = onLoadMore,
        )
    }

    // 长按菜单用官方锚点菜单贴着被按卡片弹出：入栈到条目自身，随卡片回收一起销毁。
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        if (header != null) {
            item(
                key = "coui_grid_header",
                span = StaggeredGridItemSpan.FullLine,
                contentType = "coui_grid_header",
            ) {
                header()
            }
        }
        items(items = illusts, key = { it.id }, contentType = { "coui_illust" }) { illust ->
            val menuAnchor = rememberCouiMenuAnchor()
            var menuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                CouiIllustCard(
                    illust = illust,
                    changeVersion = changeVersion,
                    onClick = { onIllustClick(illust.id) },
                    onLongClick = { menuAnchor.open { menuExpanded = true } },
                )
                CouiIllustActionMenu(
                    illust = illust,
                    expanded = menuExpanded,
                    anchor = menuAnchor,
                    onDismiss = { menuExpanded = false },
                )
            }
        }
        if (onLoadMore != null && (hasMore || isLoadingMore || loadMoreError != null)) {
            item(
                key = "coui_load_more_footer",
                span = StaggeredGridItemSpan.FullLine,
                contentType = "coui_load_more_footer",
            ) {
                CouiLoadMoreFooter(
                    isLoading = isLoadingMore,
                    error = loadMoreError,
                    onRetry = onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun CouiLoadMoreFooter(
    isLoading: Boolean,
    error: Throwable?,
    onRetry: () -> Unit,
) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> Text(
                text = strings.loading,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            error != null -> CouiTextActionButton(
                text = strings.retry,
                onClick = onRetry,
            )
        }
    }
}
