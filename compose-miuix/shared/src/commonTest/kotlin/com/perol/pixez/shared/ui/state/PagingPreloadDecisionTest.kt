package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.ui.AppConstants
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 架构审查 UI-2：触底预加载判定。
 *
 * 覆盖四条件守卫（空列表 / 无下一页 / 在途 / 翻页出错）、边界索引，以及列表 4 /
 * 瀑布流 6 两种阈值的差异化数据；并断言状态机以自身条目数判定（过滤后条目数、
 * 而非原始条目数）。
 */
class PagingPreloadDecisionTest {

    private val listThreshold = AppConstants.Layout.PRELOAD_THRESHOLD_ITEMS
    private val staggeredThreshold = AppConstants.Layout.PRELOAD_THRESHOLD_STAGGERED_ITEMS

    private fun decide(
        itemCount: Int,
        lastVisibleItemIndex: Int,
        hasMore: Boolean = true,
        isLoadingMore: Boolean = false,
        hasError: Boolean = false,
        thresholdItems: Int = listThreshold,
    ): Boolean = shouldPreloadMore(
        itemCount = itemCount,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        hasLoadMoreError = hasError,
        lastVisibleItemIndex = lastVisibleItemIndex,
        thresholdItems = thresholdItems,
    )

    private fun illust(id: Long): Illust = Illust(
        id = id,
        title = "Illust $id",
        type = "illust",
        imageUrls = ImageUrls(
            squareMedium = "https://example.com/sq.jpg",
            medium = "https://example.com/med.jpg",
            large = "https://example.com/large.jpg",
        ),
        caption = "",
        restrict = 0,
        user = IllustUser(
            id = 1L,
            name = "Artist",
            account = "artist",
            profileImageUrls = IllustProfileImageUrls(medium = "https://example.com/avatar.jpg"),
        ),
        tags = listOf(IllustTag(name = "original")),
        tools = emptyList(),
        createDate = "2026-10-11T00:00:00+09:00",
        pageCount = 1,
        width = 1000,
        height = 1000,
        sanityLevel = 2,
        xRestrict = 0,
        metaSinglePage = MetaSinglePage(originalImageUrl = "https://example.com/orig.jpg"),
        metaPages = emptyList(),
        totalView = 0,
        totalBookmarks = 0,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = 0,
    )

    // ---- 四条件守卫 ----

    @Test
    fun emptyListNeverPreloads() {
        // 无条目时无论可见到哪一项都不触发：0 条 / 阈值 4 的差值落在负区间也不例外
        assertFalse(decide(itemCount = 0, lastVisibleItemIndex = -1))
        assertFalse(decide(itemCount = 0, lastVisibleItemIndex = 0))
        assertFalse(decide(itemCount = 0, lastVisibleItemIndex = 9, thresholdItems = staggeredThreshold))
    }

    @Test
    fun missingNextPageNeverPreloads() {
        assertFalse(decide(itemCount = 20, lastVisibleItemIndex = 19, hasMore = false))
        assertFalse(decide(itemCount = 3, lastVisibleItemIndex = 2, hasMore = false))
    }

    @Test
    fun inFlightLoadMoreNeverPreloads() {
        assertFalse(decide(itemCount = 20, lastVisibleItemIndex = 19, isLoadingMore = true))
        assertFalse(decide(itemCount = 200, lastVisibleItemIndex = 199, isLoadingMore = true))
    }

    @Test
    fun loadMoreErrorNeverPreloads() {
        // 出错后等待用户显式重试，不自动续拉
        assertFalse(decide(itemCount = 20, lastVisibleItemIndex = 19, hasError = true))
        assertFalse(decide(itemCount = 20, lastVisibleItemIndex = 18, hasError = true))
    }

    // ---- 边界索引与两种阈值 ----

    @Test
    fun listThresholdTriggersFromBoundaryIndexOnward() {
        // 20 条 + 阈值 4：索引 16 起触发，15 不触发
        assertFalse(decide(itemCount = 20, lastVisibleItemIndex = 15))
        assertTrue(decide(itemCount = 20, lastVisibleItemIndex = 16))
        assertTrue(decide(itemCount = 20, lastVisibleItemIndex = 19))
    }

    @Test
    fun staggeredThresholdTriggersEarlierThanListThreshold() {
        // 同样 20 条：瀑布流阈值 6 从索引 14 起触发
        assertFalse(decide(itemCount = 20, lastVisibleItemIndex = 13, thresholdItems = staggeredThreshold))
        assertTrue(decide(itemCount = 20, lastVisibleItemIndex = 14, thresholdItems = staggeredThreshold))
        // 两条阈值必须真的不同，防止两处常量再次漂移成同值
        assertTrue(staggeredThreshold > listThreshold)
    }

    @Test
    fun shortListIsAlwaysWithinThreshold() {
        // 3 条 + 阈值 4：差值 -1，任何可见索引都落在阈值内（与原各屏幕判定一致）
        assertTrue(decide(itemCount = 3, lastVisibleItemIndex = 0))
        assertTrue(decide(itemCount = 3, lastVisibleItemIndex = 2))
    }

    // ---- 状态机委派 ----

    @Test
    fun pagedListStateDelegatesWithOwnItemCountAndGuards() = runTest {
        val paged = PagedListState(fetch = { _ -> PagedListPage(List(20) { "i$it" }, "p1") })
        paged.loadFirst()

        assertFalse(paged.shouldPreloadMore(lastVisibleItemIndex = 15))
        assertTrue(paged.shouldPreloadMore(lastVisibleItemIndex = 16))

        // 没有下一页：即使末项可见也不预加载
        val single = PagedListState(fetch = { _ -> PagedListPage(List(20) { "i$it" }, null) })
        single.loadFirst()
        assertFalse(single.shouldPreloadMore(lastVisibleItemIndex = 19))

        // 翻页失败：等待重试，不自动续拉
        val failing = PagedListState(
            fetch = { nextUrl ->
                if (nextUrl == null) PagedListPage(List(20) { "i$it" }, "p1") else throw RuntimeException("boom")
            },
        )
        failing.loadFirst()
        assertEquals(PagedLoadOutcome.Failed, failing.loadMore())
        assertFalse(failing.shouldPreloadMore(lastVisibleItemIndex = 19))
    }

    @Test
    fun illustPagedListStateUsesFilteredItemCount() = runTest {
        val state = IllustPagedListState(
            fetch = { _ -> IllustPagedPage(List(20) { illust(it.toLong()) }, "p1") },
            // 过滤掉一半：过滤后 10 条，若误用原始 20 条则边界会落在索引 16
            filter = { raw -> raw.filter { it.id % 2L == 0L } },
            filterVersionProvider = { 0 },
        )
        state.loadFirst()
        assertEquals(10, state.filteredIllusts?.size)

        assertFalse(state.shouldPreloadMore(lastVisibleItemIndex = 5))
        assertTrue(state.shouldPreloadMore(lastVisibleItemIndex = 6))
    }
}
