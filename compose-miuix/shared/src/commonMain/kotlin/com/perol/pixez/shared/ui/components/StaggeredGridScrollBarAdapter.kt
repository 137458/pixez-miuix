package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.basic.ScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

@OptIn(ExperimentalScrollBarApi::class)
class LazyStaggeredGridScrollBarAdapter(
    private val scrollState: LazyStaggeredGridState,
) : ScrollBarAdapter {

    private val dynamicLaneCount: Int
        get() = scrollState.layoutInfo.visibleItemsInfo
            .maxOfOrNull { it.lane }
            ?.plus(1)
            ?.coerceAtLeast(1) ?: 2

    private val averageItemHeight: Double
        get() = scrollState.layoutInfo.visibleItemsInfo
            .map { it.size.height }
            .average()
            .takeIf { !it.isNaN() && it > 0 } ?: 240.0

    override val scrollOffset: Double
        get() {
            val laneCount = dynamicLaneCount
            val firstItem = scrollState.firstVisibleItemIndex
            val row = firstItem / laneCount
            val offsetRatio = scrollState.firstVisibleItemScrollOffset.toDouble() / averageItemHeight.coerceAtLeast(1.0)
            return (row + offsetRatio) * averageItemHeight
        }

    override val viewportSize: Double
        get() = scrollState.layoutInfo.viewportSize.height.toDouble().coerceAtLeast(1.0)

    override val contentSize: Double
        get() {
            val totalItems = scrollState.layoutInfo.totalItemsCount
            val laneCount = dynamicLaneCount
            val rowCount = (totalItems + laneCount - 1) / laneCount
            return (rowCount.toDouble() * averageItemHeight).coerceAtLeast(viewportSize)
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val laneCount = dynamicLaneCount
        val avgHeight = averageItemHeight.coerceAtLeast(1.0)
        val targetRow = (scrollOffset / avgHeight).toInt()
        val targetIndex = (targetRow * laneCount).coerceIn(0, (scrollState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
        scrollState.scrollToItem(targetIndex)
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun rememberStaggeredGridScrollBarAdapter(
    scrollState: LazyStaggeredGridState,
): ScrollBarAdapter = remember(scrollState) {
    LazyStaggeredGridScrollBarAdapter(scrollState)
}
