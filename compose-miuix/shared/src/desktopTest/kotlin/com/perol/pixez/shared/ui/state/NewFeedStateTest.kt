package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MangaSeries
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.data.model.appendDistinct as appendDistinctSeries
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 动态/追更页状态算子 [rememberNewFeedState] 的行为与接线守护。
 *
 * 测试形态：该算子为 @Composable 且依赖 LocalIllustRepository / LocalWatchlistRepository 等具体仓储类，
 * shared 模块未引入 compose ui-test 依赖，无法驱动组合。故按算子内实际构造方式复刻两份状态机
 * （动态 [IllustPagedListState] + 追更 PagedListState<MangaSeries> 以 appendDistinctSeries 去重合并），
 * 直接驱动真状态机验证游标推进/去重/失败保留/重置行为，另以源码守护锁定算子的接线不变
 * （动态与追更两份状态机在 accountSwitched 时都要 reset，任一遗漏即残留上一账号列表）。
 */
class NewFeedStateTest {

    private fun illust(id: Long, xRestrict: Int = 0): Illust = Illust(
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
        createDate = "2026-11-15T00:00:00+09:00",
        pageCount = 1,
        width = 1000,
        height = 1000,
        sanityLevel = 2,
        xRestrict = xRestrict,
        metaSinglePage = MetaSinglePage(originalImageUrl = "https://example.com/orig.jpg"),
        metaPages = emptyList(),
        totalView = 0,
        totalBookmarks = 0,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = 0,
    )

    private fun series(id: Long): MangaSeries = MangaSeries(
        latestContentId = id * 10,
        id = id,
        title = "Series $id",
        publishedContentCount = 1,
    )

    /** 记录请求游标的假数据源，便于断言「游标推进」与「是否重复请求」。 */
    private class RecordingIllustFetch(
        private val handler: suspend (String?) -> IllustPagedPage,
    ) {
        val requestedNextUrls = mutableListOf<String?>()

        suspend fun fetch(nextUrl: String?): IllustPagedPage {
            requestedNextUrls += nextUrl
            return handler(nextUrl)
        }
    }

    /** 按算子内实际构造方式复刻动态状态机：fetch 注入分页数据源，filter 注入屏蔽过滤链。 */
    private fun illustListState(
        fetch: suspend (String?) -> IllustPagedPage,
        filter: suspend (List<Illust>) -> List<Illust> = { it },
    ): IllustPagedListState = IllustPagedListState(
        fetch = fetch,
        filter = filter,
        filterVersionProvider = { 0 },
    )

    /** 按算子内实际构造方式复刻追更状态机：合并策略为按系列 ID 去重。 */
    private fun watchlistState(fetch: suspend (String?) -> PagedListPage<MangaSeries>): PagedListState<MangaSeries> =
        PagedListState(
            fetch = fetch,
            mergeItems = { old, new -> old.appendDistinctSeries(new) },
        )

    @Test
    fun `初始态为未加载：双列表为空、无游标、footer 隐藏且不触发预加载`() {
        val state = illustListState({ IllustPagedPage(emptyList(), null) })

        assertNull(state.rawIllusts, "初始未加载时原始列表应为 null")
        assertNull(state.filteredIllusts, "初始未加载时过滤列表应为 null")
        assertNull(state.nextUrl, "初始未加载时不应有下一页游标")
        assertNull(state.initialError, "初始未加载时不应有首屏错误")
        assertNull(state.loadMoreError, "初始未加载时不应有翻页错误")
        assertEquals(0, state.generation, "初始代数应为 0")
        assertEquals(PagingFooterPhase.Hidden, state.footerState.phase, "无数据时 footer 应为隐藏")
        assertTrue(!state.shouldPreloadMore(lastVisibleItemIndex = 0), "无数据时不应触发触底预加载")

        val watchlist = watchlistState({ PagedListPage(emptyList(), null) })
        assertNull(watchlist.items, "追更状态机初始 items 应为 null")
        assertNull(watchlist.nextUrl, "追更状态机初始不应有下一页游标")
    }

    @Test
    fun `动态首屏加载成功后写入过滤列表并推进游标`() = runTest {
        val datasets = listOf(
            Triple(listOf(1L, 2L, 3L), "https://pixiv.net/next?page=2", PagingFooterPhase.Hidden),
            Triple(listOf(9L), null, PagingFooterPhase.NoMore),
        )
        for ((ids, nextUrl, expectedPhase) in datasets) {
            val fetch = RecordingIllustFetch { IllustPagedPage(ids.map { illust(it) }, nextUrl) }
            val state = illustListState(fetch::fetch)

            val outcome = state.loadFirst()

            assertEquals(PagedLoadOutcome.Written, outcome, "首屏成功应回报 Written（游标=$nextUrl）")
            assertEquals(ids, state.rawIllusts?.map { it.id }, "原始列表应为本页内容")
            assertEquals(ids, state.filteredIllusts?.map { it.id }, "过滤列表应与原始列表一致（过滤链为空）")
            assertEquals(nextUrl, state.nextUrl, "应记录本页返回的下一页游标")
            assertEquals(expectedPhase, state.footerState.phase, "footer 相位应由 hasMore 决定")
            assertEquals(1, state.generation, "首屏加载应推进代数一次")
            assertNull(state.initialError, "首屏成功后不应残留错误")
            assertEquals(listOf<String?>(null), fetch.requestedNextUrls, "首屏必须以 null 游标请求")
        }
    }

    @Test
    fun `动态翻页按作品 ID 去重追加并保留过滤链结果`() = runTest {
        // 常规数据：第二页与第一页存在重复作品。
        val plainFetch = RecordingIllustFetch { nextUrl ->
            when (nextUrl) {
                null -> IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?page=2")
                else -> IllustPagedPage(listOf(illust(2), illust(3), illust(3)), null)
            }
        }
        val plain = illustListState(plainFetch::fetch)
        plain.loadFirst()
        assertEquals(PagedLoadOutcome.Written, plain.loadMore(), "翻页成功应回报 Written")
        assertEquals(listOf(1L, 2L, 3L), plain.filteredIllusts?.map { it.id }, "重复作品 ID 应被去重合并")
        assertNull(plain.nextUrl, "末页应清空下一页游标")
        assertEquals(PagingFooterPhase.NoMore, plain.footerState.phase, "末页 footer 应为 NoMore")

        // 边界数据：第二页包含被屏蔽（R-18）作品，去重后仍应被过滤链剔除。
        val filteredFetch = RecordingIllustFetch { nextUrl ->
            when (nextUrl) {
                null -> IllustPagedPage(listOf(illust(1), illust(2, xRestrict = 2)), "https://pixiv.net/next?page=2")
                else -> IllustPagedPage(listOf(illust(2, xRestrict = 2), illust(3), illust(4)), null)
            }
        }
        val filtered = illustListState(filteredFetch::fetch) { raw -> raw.filterNot { it.xRestrict == 2 } }
        filtered.loadFirst()
        filtered.loadMore()
        assertEquals(
            listOf(1L, 2L, 3L, 4L),
            filtered.rawIllusts?.map { it.id },
            "原始列表应保留屏蔽项（过滤链只影响展示层）",
        )
        assertEquals(listOf(1L, 3L, 4L), filtered.filteredIllusts?.map { it.id }, "R-18 作品应被过滤链剔除")
    }

    @Test
    fun `动态翻页失败保留旧列表并暴露重试入口，首屏失败写入错误态`() = runTest {
        val boom = IllegalStateException("network down")
        val fetch = RecordingIllustFetch { nextUrl ->
            if (nextUrl == null) IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?page=2")
            else throw boom
        }
        val state = illustListState(fetch::fetch)
        state.loadFirst()

        assertEquals(PagedLoadOutcome.Failed, state.loadMore(), "翻页失败应回报 Failed")
        assertEquals(listOf(1L, 2L), state.filteredIllusts?.map { it.id }, "翻页失败必须保留旧列表")
        assertEquals("https://pixiv.net/next?page=2", state.nextUrl, "翻页失败不应丢弃游标")
        assertNotNull(state.loadMoreError, "翻页失败应写入 loadMoreError")
        assertTrue(!state.isLoadingMore, "翻页结束后应复位加载态")
        assertEquals(PagingFooterPhase.Retry, state.footerState.phase, "失败后 footer 应给出重试入口")

        // 边界：无任何旧数据时首屏失败写入 initialError，列表保持 null。
        val emptyState = illustListState({ throw boom })
        assertEquals(PagedLoadOutcome.Failed, emptyState.loadFirst(), "首屏失败应回报 Failed")
        assertNotNull(emptyState.initialError, "无旧数据时失败应写入 initialError")
        assertNull(emptyState.filteredIllusts, "首屏失败不应伪造列表")
        assertTrue(!emptyState.isRefreshing, "首屏失败后应复位刷新态")
        assertEquals(PagingFooterPhase.Hidden, emptyState.footerState.phase, "无数据的错误态 footer 应隐藏")
    }

    @Test
    fun `追更列表按系列 ID 去重追加，失败保留旧列表，重置后清空`() = runTest {
        val requested = mutableListOf<String?>()
        val source = PagedListState(
            fetch = { nextUrl ->
                requested += nextUrl
                when (nextUrl) {
                    null -> PagedListPage(listOf(series(10), series(11)), "https://pixiv.net/next?w=2")
                    else -> PagedListPage(listOf(series(11), series(12)), null)
                }
            },
            mergeItems = { old, new -> old.appendDistinctSeries(new) },
        )
        source.loadFirst()
        assertEquals(PagedLoadOutcome.Written, source.loadMore(), "追更翻页成功应回报 Written")
        assertEquals(listOf(10L, 11L, 12L), source.items?.map { it.id }, "追更列表应保留旧列表并去重追加")
        assertEquals(listOf(null, "https://pixiv.net/next?w=2"), requested, "翻页应携带上一页返回的游标")

        // 边界：重置后再次首屏加载等价于重新开始，游标从 null 起算。
        source.reset()
        assertNull(source.items, "重置后追更列表应清空")
        assertNull(source.nextUrl, "重置后游标应清空")
        source.loadFirst()
        assertEquals(listOf(10L, 11L), source.items?.map { it.id }, "重置后首屏应重新拉取第一页")
        assertEquals(null, requested.last(), "重置后首屏应以 null 游标请求")
    }

    @Test
    fun `账号切换时动态与追更两份状态机均被重置`() = runTest {
        // 常规：两份状态机都有数据，切号后两条列表都必须清空。
        val listState = illustListState({ nextUrl ->
            if (nextUrl == null) IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?page=2")
            else IllustPagedPage(emptyList(), null)
        })
        val watchlist = watchlistState({ PagedListPage(listOf(series(10)), "https://pixiv.net/next?w=2") })
        listState.loadFirst()
        watchlist.loadFirst()

        // 复刻算子接线：accountSwitched 时两份状态机都 reset()。
        val accountSwitched = true
        if (accountSwitched) listState.reset()
        if (accountSwitched) watchlist.reset()

        assertNull(listState.filteredIllusts, "切号后动态列表应清空")
        assertNull(listState.nextUrl, "切号后动态游标应清空")
        assertEquals(PagingFooterPhase.Hidden, listState.footerState.phase, "切号后 footer 应回到隐藏态")
        assertNull(watchlist.items, "切号后追更列表应清空")
        assertNull(watchlist.nextUrl, "切号后追更游标应清空")

        // 边界：只有追更侧有数据时同样被清空，且 reset 递增代数使后续首屏重拉。
        val watchlistOnly = watchlistState({ PagedListPage(listOf(series(20)), null) })
        watchlistOnly.loadFirst()
        val generationBefore = watchlistOnly.generation
        watchlistOnly.reset()
        assertNull(watchlistOnly.items, "仅有追更数据时切号也应清空")
        assertEquals(generationBefore + 1, watchlistOnly.generation, "reset 应递增代数以作废在途请求")
    }

    @Test
    fun `算子接线守护：动态页用两份状态机并在切号时全部重置`() {
        val source = StateSourceReader.readUi("state/NewFeedState.kt")

        assertTrue(
            "mergeItems = { old, new -> old.appendDistinctSeries(new) }" in source,
            "追更状态机的去重合并策略应注入 appendDistinctSeries（按系列 ID 去重）",
        )
        assertTrue(
            "IllustPagedListState(" in source,
            "动态列表应使用下沉的 IllustPagedListState 状态机",
        )
        assertTrue(
            "if (accountSwitched) listState.reset()" in source,
            "切号时必须重置动态分页状态机",
        )
        assertTrue(
            "if (accountSwitched) watchlistState.reset()" in source,
            "切号时必须重置追更分页状态机（只重置一份会残留上一账号列表）",
        )
        assertTrue(
            "loginEventFlow.collect" in source,
            "应收集 loginEventFlow 作为登录/登出/切号信号源",
        )
        assertTrue(
            "remember(selectedRestrict)" in source,
            "可见性筛选切换应整体重建动态状态机（等价清空重拉）",
        )
    }
}
