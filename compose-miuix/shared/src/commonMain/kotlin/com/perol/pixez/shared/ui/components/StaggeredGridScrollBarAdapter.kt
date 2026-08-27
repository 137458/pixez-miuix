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
    override val scrollOffset: Double
        get() {
            val firstItem = scrollState.firstVisibleItemIndex
            val offset = scrollState.firstVisibleItemScrollOffset
            return firstItem.toDouble() * 100.0 + offset.toDouble()
        }

    override val viewportSize: Double
        get() = scrollState.layoutInfo.viewportSize.height.toDouble().coerceAtLeast(1.0)

    override val contentSize: Double
        get() {
            val totalItems = scrollState.layoutInfo.totalItemsCount
            val rowCount = (totalItems + 1) / 2
            return (rowCount.toDouble() * 100.0).coerceAtLeast(viewportSize)
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val targetRow = (scrollOffset / 100.0).toInt()
        val targetIndex = (targetRow * 2).coerceIn(0, (scrollState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
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
