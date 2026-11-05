package com.perol.pixez.shared.ui.state

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 架构审查 UI-3：分页 footer 的三态判定。
 *
 * 目标是把八处各自的 when 分支收敛成一处可断言的渲染态：
 * 仍在翻页时不渲染、加载中、失败重试、没有更多。
 */
class PagingFooterStateTest {

    @Test
    fun hiddenWhileMorePagesRemain() {
        assertEquals(
            PagingFooterPhase.Hidden,
            PagingFooterState(hasMore = true, isLoadingMore = false, loadMoreError = null).phase,
        )
        // 空列表且没有下一页时同样不渲染
        assertEquals(
            PagingFooterPhase.Hidden,
            PagingFooterState(hasMore = false, isLoadingMore = false, loadMoreError = null, hasItems = false).phase,
        )
    }

    @Test
    fun loadingPhaseTakesPrecedence() {
        assertEquals(
            PagingFooterPhase.Loading,
            PagingFooterState(hasMore = true, isLoadingMore = true, loadMoreError = null).phase,
        )
        // 加载中优先于上一次的失败与「没有更多」
        assertEquals(
            PagingFooterPhase.Loading,
            PagingFooterState(hasMore = false, isLoadingMore = true, loadMoreError = RuntimeException("x")).phase,
        )
    }

    @Test
    fun retryPhaseTakesPrecedenceOverNoMore() {
        assertEquals(
            PagingFooterPhase.Retry,
            PagingFooterState(hasMore = true, isLoadingMore = false, loadMoreError = RuntimeException("x")).phase,
        )
        assertEquals(
            PagingFooterPhase.Retry,
            PagingFooterState(hasMore = false, isLoadingMore = false, loadMoreError = RuntimeException("x")).phase,
        )
    }

    @Test
    fun noMoreOnlyWhenListHasItems() {
        assertEquals(
            PagingFooterPhase.NoMore,
            PagingFooterState(hasMore = false, isLoadingMore = false, loadMoreError = null).phase,
        )
        assertEquals(
            PagingFooterPhase.NoMore,
            PagingFooterState(hasMore = false, isLoadingMore = false, loadMoreError = null, hasItems = true).phase,
        )
    }

    @Test
    fun stateMachineExposesMatchingSnapshot() = runTest {
        val paged = PagedListState(fetch = { nextUrl ->
            if (nextUrl == null) PagedListPage(listOf("a"), "p1") else throw RuntimeException("boom")
        })
        paged.loadFirst()
        assertEquals(PagingFooterPhase.Hidden, paged.footerState.phase, "仍有下一页且未在加载/出错不渲染 footer")

        paged.loadMore()
        assertEquals(PagingFooterPhase.Retry, paged.footerState.phase, "翻页失败必须给出重试入口")

        // 没有下一页：三态落到「没有更多」
        val single = PagedListState(fetch = { _ -> PagedListPage(listOf("a"), null) })
        single.loadFirst()
        assertEquals(PagingFooterPhase.NoMore, single.footerState.phase)
    }
}
