package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.snapshots.Snapshot
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.data.model.isR18
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 高-4 分页状态机断言：generation 竞态守卫 + 复位 isLoadingMore + appendDistinct 去重，
 * 常规组与竞态组两组差异化数据，语义基准为 RankingScreen 版状态机。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IllustPagedListStateTest {

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
        createDate = "2026-10-11T00:00:00+09:00",
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

    /** 可变过滤开关，模拟 ShieldScreen 的「隐藏 R-18」开关与 filterChangeVersion 信号。 */
    private class FakeFilterSettings {
        var hideR18 = false
        var version = 0

        suspend fun filter(raw: List<Illust>): List<Illust> =
            if (hideR18) raw.filterNot { it.isR18() } else raw
    }

    /**
     * 可编程假数据源：按请求序号（从 1 计）挂起指定调用以编排竞态时序，
     * 每个测试自建实例，不跨用例共享状态。
     */
    private class FakeIllustSource {
        /** nextUrl → 该页响应；null 键为首页，可覆写以模拟刷新后返回新数据。 */
        val pages = mutableMapOf<String?, IllustPagedPage>()
        private val failures = mutableMapOf<String?, Throwable>()
        private val heldCalls = mutableMapOf<Int, CompletableDeferred<Unit>>()

        /** 已发出的请求参数序列（从首页开始），用于断言「未触发请求」。 */
        val calls = mutableListOf<String?>()

        /** 让第 [callIndex] 次 fetch 挂起，返回放行闸门。 */
        fun hold(callIndex: Int): CompletableDeferred<Unit> {
            val gate = CompletableDeferred<Unit>()
            heldCalls[callIndex] = gate
            return gate
        }

        fun release(callIndex: Int) {
            heldCalls.getValue(callIndex).complete(Unit)
        }

        fun failOn(nextUrl: String?, error: Throwable) {
            failures[nextUrl] = error
        }

        fun clearFailure(nextUrl: String?) {
            failures.remove(nextUrl)
        }

        suspend fun fetch(nextUrl: String?): IllustPagedPage {
            calls.add(nextUrl)
            // 读取但不移除：闸门由 release 完成后继续保留，供断言侧定位
            heldCalls[calls.size]?.await()
            failures[nextUrl]?.let { throw it }
            return pages.getValue(nextUrl)
        }
    }

    private fun newState(
        source: FakeIllustSource,
        settings: FakeFilterSettings,
    ): IllustPagedListState = IllustPagedListState(
        fetch = source::fetch,
        filter = { raw -> settings.filter(raw) },
        filterVersionProvider = { settings.version },
    )

    // ---- 常规组 ----

    @Test
    fun loadFirstSuccessReplacesListAndUpdatesNextUrl() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(
            illusts = listOf(illust(1), illust(2, xRestrict = 1), illust(3)),
            nextUrl = "p1",
        )

        state.loadFirst()

        assertEquals(listOf(1L, 2L, 3L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(1L, 2L, 3L), state.rawIllusts?.map { it.id })
        assertEquals("p1", state.nextUrl)
        assertNull(state.initialError)
        assertFalse(state.isRefreshing)
        assertEquals(1, state.generation)

        // 差异化第二组数据：force 刷新必须整体替换而非追加，nextUrl 更新为新页签
        settings.hideR18 = true
        settings.version = 1
        source.pages[null] = IllustPagedPage(
            illusts = listOf(illust(9, xRestrict = 1), illust(8)),
            nextUrl = null,
        )
        state.loadFirst(force = true)

        assertEquals(listOf(8L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(9L, 8L), state.rawIllusts?.map { it.id })
        assertNull(state.nextUrl)
        assertFalse(state.isRefreshing)
        assertEquals(2, state.generation)
    }

    @Test
    fun loadMoreAppendsDistinctAndUpdatesNextUrl() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1), illust(2)), "p1")
        source.pages["p1"] = IllustPagedPage(listOf(illust(2), illust(3)), "p2")
        source.pages["p2"] = IllustPagedPage(listOf(illust(3), illust(4)), null)

        state.loadFirst()
        state.loadMore()

        assertEquals(listOf(1L, 2L, 3L), state.rawIllusts?.map { it.id })
        assertEquals("p2", state.nextUrl)

        state.loadMore()

        assertEquals(listOf(1L, 2L, 3L, 4L), state.rawIllusts?.map { it.id })
        assertNull(state.nextUrl)
        assertNull(state.loadMoreError)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun loadMoreFailureSetsErrorAndResetsLoadingState() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1)), "p1")
        val pageError = RuntimeException("page fetch failed")
        source.failOn("p1", pageError)

        state.loadFirst()
        state.loadMore()

        assertSame(pageError, state.loadMoreError)
        assertFalse(state.isLoadingMore)
        assertEquals(listOf(1L), state.rawIllusts?.map { it.id })
        assertEquals("p1", state.nextUrl)
        assertNull(state.initialError)
    }

    @Test
    fun loadMoreWithoutNextUrlDoesNotFetch() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1)), null)
        state.loadFirst()
        val callsAfterFirst = source.calls.size

        state.loadMore()

        assertEquals(callsAfterFirst, source.calls.size)
        assertFalse(state.isLoadingMore)
        assertNull(state.loadMoreError)
        assertEquals(listOf(1L), state.rawIllusts?.map { it.id })
    }

    @Test
    fun concurrentLoadMoreWhileLoadingIsRejected() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1), illust(2)), "p1")
        source.pages["p1"] = IllustPagedPage(listOf(illust(2), illust(3)), "p2")
        state.loadFirst()

        source.hold(2)
        val first = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)

        // 翻页在途时重复触发必须被拒绝：不发出第二次请求
        state.loadMore()

        source.release(2)
        first.join()

        assertEquals(2, source.calls.size)
        assertEquals(listOf(1L, 2L, 3L), state.rawIllusts?.map { it.id })
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun loadFirstFailureWithoutDataSetsInitialErrorThenRetryRecovers() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        val initialFailure = RuntimeException("network down")
        source.failOn(null, initialFailure)

        state.loadFirst()

        assertSame(initialFailure, state.initialError)
        assertNull(state.filteredIllusts)
        assertFalse(state.isRefreshing)

        // 重试：失败不遗留数据，再次 loadFirst 不得因早退跳过
        source.clearFailure(null)
        source.pages[null] = IllustPagedPage(listOf(illust(5)), "p1")
        state.loadFirst()

        assertNull(state.initialError)
        assertEquals(listOf(5L), state.filteredIllusts?.map { it.id })
        assertEquals("p1", state.nextUrl)
    }

    @Test
    fun loadFirstFailureWithDataKeepsExistingList() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1)), "p1")
        state.loadFirst()
        val refreshFailure = RuntimeException("refresh failed")
        source.failOn(null, refreshFailure)

        state.loadFirst(force = true)

        // 已有数据时刷新失败不得写入 initialError，旧列表原样保留
        assertNull(state.initialError)
        assertEquals(listOf(1L), state.rawIllusts?.map { it.id })
        assertEquals("p1", state.nextUrl)
        assertFalse(state.isRefreshing)
    }

    // ---- 竞态组 ----

    @Test
    fun inFlightLoadMoreIsRejectedAfterRefreshBumpsGeneration() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1)), "p1")
        source.pages["p1"] = IllustPagedPage(listOf(illust(2)), "stale-next")
        state.loadFirst()

        // 旧翻页在途（挂在第 2 次 fetch）
        source.hold(2)
        val staleLoadMore = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)

        // 刷新递增 generation 并复位 isLoadingMore，返回新首页 [9]
        source.pages[null] = IllustPagedPage(listOf(illust(9)), "p2")
        val refreshed = launch { state.loadFirst(force = true) }
        runCurrent()
        assertFalse(state.isLoadingMore)
        assertEquals(listOf(9L), state.filteredIllusts?.map { it.id })
        assertEquals("p2", state.nextUrl)

        // 放行旧翻页：其结果与错误必须被 generation 守卫整体丢弃
        source.release(2)
        staleLoadMore.join()
        refreshed.join()

        assertEquals(listOf(9L), state.rawIllusts?.map { it.id })
        assertEquals("p2", state.nextUrl)
        assertNull(state.loadMoreError)
        assertFalse(state.isLoadingMore)
        assertNull(state.initialError)
    }

    @Test
    fun concurrentLoadFirstsOlderGenerationDoesNotClobber() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1)), "p1")
        state.loadFirst()

        // 两次连续刷新并发：分别挂在第 2、3 次 fetch 上
        source.hold(2)
        val older = launch { state.loadFirst(force = true) }
        runCurrent()
        source.pages[null] = IllustPagedPage(listOf(illust(7)), "pB")
        source.hold(3)
        val newer = launch { state.loadFirst(force = true) }
        runCurrent()
        assertTrue(state.isRefreshing)

        // 旧刷新先完成：不得覆写新列表，也不得复位仍在途的新刷新标志
        source.release(2)
        runCurrent()
        older.join()

        assertEquals(listOf(1L), state.rawIllusts?.map { it.id })
        assertEquals("p1", state.nextUrl)
        assertTrue(state.isRefreshing)

        // 新刷新完成后写入结果并复位刷新标志
        source.release(3)
        runCurrent()
        newer.join()

        assertEquals(listOf(7L), state.rawIllusts?.map { it.id })
        assertEquals("pB", state.nextUrl)
        assertFalse(state.isRefreshing)
    }

    // ---- 组合透传 ----

    @Test
    fun pagingFlagsAreComposeSnapshotStates() {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        val observed = mutableSetOf<Any>()
        val snapshot = Snapshot.takeSnapshot(readObserver = { observed.add(it) })
        snapshot.enter {
            state.nextUrl
            state.initialError
            state.isRefreshing
            state.isLoadingMore
            state.loadMoreError
        }
        snapshot.dispose()

        // 五个分页标志必须是五个不同的快照状态对象：UI 在组合期直读
        // （RankingScreen.kt:286-292,326-328 消费方式），普通 var 写入不会触发重组。
        assertEquals(5, observed.size)
    }

    @Test
    fun refilterIfChangedUsesInjectedVersionProvider() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)
        source.pages[null] = IllustPagedPage(listOf(illust(1), illust(2, xRestrict = 1)), "p1")
        state.loadFirst()
        assertEquals(listOf(1L, 2L), state.filteredIllusts?.map { it.id })

        settings.hideR18 = true
        settings.version = 1

        assertTrue(state.refilterIfChanged())
        assertEquals(listOf(1L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(1L, 2L), state.rawIllusts?.map { it.id })

        // 版本未变时不得重复过滤
        assertFalse(state.refilterIfChanged())
    }

    @Test
    fun restoreSnapshotThenLoadFirstKeepsSnapshot() = runTest {
        val source = FakeIllustSource()
        val settings = FakeFilterSettings()
        val state = newState(source, settings)

        state.restore(listOf(illust(7)), "p9")

        assertEquals(listOf(7L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(7L), state.rawIllusts?.map { it.id })
        assertEquals("p9", state.nextUrl)

        // 恢复态已有数据且无首屏错误，loadFirst 须早退不覆盖快照
        state.loadFirst()

        assertEquals(0, source.calls.size)
        assertEquals(listOf(7L), state.filteredIllusts?.map { it.id })
        assertEquals("p9", state.nextUrl)
    }
}
