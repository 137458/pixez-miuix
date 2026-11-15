package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.appendDistinct
import com.perol.pixez.shared.ui.screens.SpotlightCategory
import com.perol.pixez.shared.ui.state.StateTestFixtures.spotlightArticle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 特辑页状态算子 [rememberSpotlightFeedState] 的行为与接线守护。
 *
 * 测试形态：算子为 @Composable 且依赖 LocalPixivisionRepository 与 LazyStaggeredGridState，
 * shared 模块未引入 compose ui-test 依赖，无法驱动组合。故按算子内实际构造复刻
 * 「每分类一份 [PagedListState]"（mergeItems 注入 appendDistinct 去重）与
 * 「缓存快照命中即 restore」的分支，直接驱动真状态机验证分类隔离、去重追加、缓存恢复与触底判定，
 * 另以源码守护锁定接线。
 */
class SpotlightFeedStateTest {

    /** 按算子内实际构造复刻：合并策略为按文章 ID 去重。 */
    private fun newCategoryState(
        fetch: suspend (String?) -> PagedListPage<SpotlightArticle>,
    ): PagedListState<SpotlightArticle> = PagedListState(
        fetch = fetch,
        mergeItems = { old, new -> old.appendDistinct(new) },
    )

    private fun PagedListState<SpotlightArticle>.ids(): List<Long>? = items?.map { it.id }

    @Test
    fun `初始态每个分类都未加载且不触发触底预加载`() {
        val states = SpotlightCategory.entries.associateWith {
            newCategoryState({ PagedListPage(emptyList(), null) })
        }

        assertEquals(4, states.size, "特辑分类应各有一份状态机")
        states.forEach { (category, state) ->
            assertNull(state.items, "$category 初始不应有数据")
            assertNull(state.nextUrl, "$category 初始不应有游标")
            assertEquals(0, state.generation, "$category 初始代数应为 0")
            assertEquals(PagingFooterPhase.Hidden, state.footerState.phase, "$category 初始 footer 应隐藏")
            assertTrue(!state.shouldPreloadMore(lastVisibleItemIndex = 0), "$category 初始不应触发预加载")
        }
    }

    @Test
    fun `首屏加载成功后写入列表并按游标决定 footer`() = runTest {
        val datasets = listOf(
            Triple(listOf(1L, 2L), "https://pixivision.net/next?page=2", PagingFooterPhase.Hidden),
            Triple(listOf(7L), null, PagingFooterPhase.NoMore),
        )
        for ((ids, nextUrl, phase) in datasets) {
            val requested = mutableListOf<String?>()
            val state = newCategoryState({ url ->
                requested += url
                PagedListPage(ids.map { spotlightArticle(it) }, nextUrl)
            })

            assertEquals(PagedLoadOutcome.Written, state.loadFirst(), "首屏应写入（游标=$nextUrl）")
            assertEquals(ids, state.ids(), "列表应为返回文章")
            assertEquals(nextUrl, state.nextUrl, "应记录下一页游标")
            assertEquals(phase, state.footerState.phase, "footer 相位应由 hasMore 决定")
            assertEquals(listOf<String?>(null), requested, "首屏必须以 null 游标请求")
        }
    }

    @Test
    fun `翻页按文章 ID 去重追加，末页后不再关注游标`() = runTest {
        // 常规：第二页含重复文章。
        val plain = newCategoryState({ url ->
            when (url) {
                null -> PagedListPage(listOf(spotlightArticle(1), spotlightArticle(2)), "https://pixivision.net/next?page=2")
                else -> PagedListPage(listOf(spotlightArticle(2), spotlightArticle(3), spotlightArticle(3)), null)
            }
        })
        plain.loadFirst()
        assertEquals(PagedLoadOutcome.Written, plain.loadMore(), "翻页成功应写入")
        assertEquals(listOf(1L, 2L, 3L), plain.ids(), "重复文章 ID 应被去重合并")
        assertNull(plain.nextUrl, "末页应清空游标")
        assertEquals(PagingFooterPhase.NoMore, plain.footerState.phase, "末页 footer 应为 NoMore")
        assertEquals(PagedLoadOutcome.Skipped, plain.loadMore(), "末页后翻页应早退")

        // 边界：第二页为空列表（服务端返回空页但有游标），仍应保留旧数据并推进游标。
        val emptyPage = newCategoryState({ url ->
            when (url) {
                null -> PagedListPage(listOf(spotlightArticle(5)), "https://pixivision.net/next?page=2")
                else -> PagedListPage(emptyList(), "https://pixivision.net/next?page=3")
            }
        })
        emptyPage.loadFirst()
        assertEquals(PagedLoadOutcome.Written, emptyPage.loadMore(), "空页也属于成功写入")
        assertEquals(listOf(5L), emptyPage.ids(), "空页不得清空既有列表")
        assertEquals("https://pixivision.net/next?page=3", emptyPage.nextUrl, "空页也应推进游标")
    }

    @Test
    fun `分类状态互相隔离，切回分类复用同一状态机且缓存命中早退`() = runTest {
        val fetchCount = mutableMapOf<SpotlightCategory, Int>()
        val states = mutableMapOf<SpotlightCategory, PagedListState<SpotlightArticle>>()

        // 复刻算子接线：newCategoryState(category) + categoryStates.getOrPut(selectedCategory)。
        fun stateOf(category: SpotlightCategory): PagedListState<SpotlightArticle> = states.getOrPut(category) {
            newCategoryState({ _ ->
                fetchCount[category] = (fetchCount[category] ?: 0) + 1
                PagedListPage(listOf(spotlightArticle(category.ordinal * 10L + 1)), "https://pixivision.net/next?${category.code}")
            })
        }

        val all = stateOf(SpotlightCategory.ALL)
        all.loadFirst()
        assertEquals(listOf(1L), all.ids(), "ALL 分类应展示自身数据")

        val illust = stateOf(SpotlightCategory.ILLUST)
        assertNull(illust.items, "切换分类后新分类应从空态开始")
        illust.loadFirst()
        assertEquals(listOf(11L), illust.ids(), "ILLUST 分类应展示自身数据")
        assertEquals(listOf(1L), all.ids(), "加载另一分类不得改动既有分类列表")

        // 切回 ALL：getOrPut 复用同一实例，既有数据仍在且无需重新请求。
        val allAgain = stateOf(SpotlightCategory.ALL)
        assertSame(all, allAgain, "切回分类应复用同一状态机实例（保持浏览位置与缓存）")
        assertEquals(PagedLoadOutcome.Skipped, allAgain.loadFirst(), "已有数据时（分类切换重入）首屏应早退")
        assertEquals(1, fetchCount[SpotlightCategory.ALL], "缓存命中不得重复请求")
        assertEquals(listOf(1L), allAgain.ids(), "复用实例应保留既有列表")
    }

    @Test
    fun `缓存快照命中即恢复，空缓存不恢复必须请求`() = runTest {
        // 常规：仓库有首屏缓存 => restore 后直接可用，且不再请求。
        var fetchCount = 0
        val cached = newCategoryState({ fetchCount++; PagedListPage(listOf(spotlightArticle(99)), null) })
        cached.restore(listOf(spotlightArticle(1), spotlightArticle(2)), "https://pixivision.net/next?page=2")
        assertEquals(listOf(1L, 2L), cached.ids(), "缓存命中应立即可展示")
        assertEquals("https://pixivision.net/next?page=2", cached.nextUrl, "缓存命中应恢复游标")
        assertEquals(PagedLoadOutcome.Skipped, cached.loadFirst(), "缓存命中后首屏应早退")
        assertEquals(0, fetchCount, "缓存命中不得发起请求")

        // 边界：缓存存在但文章为空列表 => 算子条件不 restore，首次进入必须请求。
        val emptyCache = newCategoryState({ fetchCount++; PagedListPage(listOf(spotlightArticle(3)), null) })
        val cachedArticles = emptyList<SpotlightArticle>()
        if (cachedArticles.isNotEmpty()) {
            emptyCache.restore(cachedArticles, null)
        }
        assertNull(emptyCache.items, "空缓存不得 restore 出空列表（否则会误判为已加载而永不请求）")
        assertEquals(PagedLoadOutcome.Written, emptyCache.loadFirst(), "空缓存时首屏应真正请求")
        assertEquals(listOf(3L), emptyCache.ids(), "请求后应展示返回文章")
        assertEquals(1, fetchCount, "空缓存应发起一次请求")
    }

    @Test
    fun `失败保留旧列表，触底判定只在有游标且无错误时放行`() = runTest {
        val boom = IllegalStateException("pixivision down")
        var failNext = false
        val state = newCategoryState({ _ ->
            if (failNext) throw boom else PagedListPage(listOf(spotlightArticle(1), spotlightArticle(2)), "https://pixivision.net/next?page=2")
        })
        state.loadFirst()
        assertTrue(state.shouldPreloadMore(lastVisibleItemIndex = 1), "有游标且未在加载时应允许触底预加载")

        failNext = true
        val outcome = state.loadMore()
        assertEquals(PagedLoadOutcome.Failed, outcome, "翻页失败应回报 Failed")
        assertEquals(listOf(1L, 2L), state.ids(), "翻页失败必须保留旧列表")
        assertEquals(PagingFooterPhase.Retry, state.footerState.phase, "失败后 footer 应给出重试入口")
        assertTrue(!state.shouldPreloadMore(lastVisibleItemIndex = 1), "存在翻页错误时不得自动重试（需用户点击）")

        // 边界：无游标（末页）时不得再预加载。
        val noMore = newCategoryState({ PagedListPage(listOf(spotlightArticle(5)), null) })
        noMore.loadFirst()
        assertTrue(!noMore.shouldPreloadMore(lastVisibleItemIndex = 0), "无下一页时不得预加载")
    }

    @Test
    fun `算子接线守护：每分类一份状态机、去重合并、缓存恢复与触底监听`() {
        val source = StateSourceReader.readUi("state/SpotlightFeedState.kt")

        assertTrue(
            "fun newCategoryState(category: SpotlightCategory): PagedListState<SpotlightArticle>" in source,
            "每分类状态机应在单点构造",
        )
        assertTrue(
            "mergeItems = { old, new -> old.appendDistinct(new) }" in source,
            "累积与去重应收敛到状态机注入点（仓库只返回当页）",
        )
        assertTrue(
            "mutableStateMapOf<SpotlightCategory, PagedListState<SpotlightArticle>>()" in source,
            "应为每个分类分别维护状态，保留各自浏览位置与缓存",
        )
        assertTrue(
            "categoryStates.getOrPut(selectedCategory) { newCategoryState(selectedCategory) }" in source,
            "当前分类状态应经 getOrPut 复用，避免切回时重建丢数据",
        )
        assertTrue(
            "state.restore(cached.spotlightArticles, cached.nextUrl)" in source,
            "首屏缓存命中应直接恢复列表与游标",
        )
        assertTrue(
            "if (cached.spotlightArticles.isNotEmpty()) {" in source,
            "空缓存不得恢复（否则会被误判为已加载而永不请求）",
        )
        assertTrue("currentCategoryState.loadFirst()" in source, "首次进入或分类切换应发起首屏加载")
        assertTrue("currentCategoryState.shouldPreloadMore(" in source, "触底判定应走状态机单点实现")
        assertTrue(".distinctUntilChanged()" in source, "触底监听应去重，避免重复触发翻页")
    }
}
