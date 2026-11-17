package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.perol.pixez.shared.ui.state.PagedListState
import com.perol.pixez.shared.ui.state.shouldPreloadMore

/**
 * 触底预加载统一入口：把「末项索引 → 状态机四条件守卫 → 触发 loadMore」这段样板收敛到一处。
 *
 * 各列表页此前逐字重复同一段 derivedStateOf + LaunchedEffect，且末项索引取法不统一
 * （maxOfOrNull 与 lastOrNull() 混用）。这里统一取可视项的最大 index：布局顺序对外不可依赖，
 * 最大值才是真正的末项；无可视项时回落到 -1，交由状态机自身守卫。
 *
 * @param pagedList 分页状态机，提供 [PagedListState.shouldPreloadMore] 的四条件守卫与阈值单源。
 * @param listState 列表滚动状态，用于读取当前可视项区间。
 * @param onLoadMore 进入预加载区间时的回调；宿主 lambda 每次重组都会换身份，故内部经
 *   [rememberUpdatedState] 取最新值，避免 derivedStateOf 闭包固化旧引用后调到过期闭包。
 */
@Composable
internal fun rememberPagingPreload(
    pagedList: PagedListState<*>,
    listState: LazyListState,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    // 键含 pagedList 与 listState：两者任一更换都要重建派生状态，否则派生值一直挂在旧实例上。
    val shouldLoadMore by remember(pagedList, listState) {
        derivedStateOf {
            pagedList.shouldPreloadMore(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
            )
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            currentOnLoadMore()
        }
    }
}

/**
 * 交错网格等不持有 [PagedListState]、只持有条目数与分页标志的列表用这个重载。
 *
 * 守卫条件与上面同源（同一个 [shouldPreloadMore]），差别只是输入由调用方直接给出：
 * 网格的分页标志散在形参上，阈值也按交错布局的密度另取。
 *
 * @param lastVisibleItemIndex 每次求值都重新读取可视末项索引，故经 [rememberUpdatedState] 取最新闭包。
 */
@Composable
internal fun rememberPagingPreload(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    lastVisibleItemIndex: () -> Int,
    thresholdItems: Int,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentLastVisibleItemIndex by rememberUpdatedState(lastVisibleItemIndex)

    // 键含分页标志与条目数：标志翻转或长度变化都要重算，否则派生值会停在旧判定上。
    val shouldLoadMore by remember(itemCount, hasMore, isLoadingMore, loadMoreError, thresholdItems) {
        derivedStateOf {
            shouldPreloadMore(
                itemCount = itemCount,
                hasMore = hasMore,
                isLoadingMore = isLoadingMore,
                hasLoadMoreError = loadMoreError != null,
                lastVisibleItemIndex = currentLastVisibleItemIndex(),
                thresholdItems = thresholdItems,
            )
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            currentOnLoadMore()
        }
    }
}
