package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 架构审查 UI-8：loadFirst / loadMore 回报可判定结果（写入 / 丢弃 / 失败 / 未请求）。
 *
 * 屏幕据返回值决定写跨页恢复快照与回顶，因此三态必须与状态机真实写入严格一致：
 * 失败与代际失配都不得报成「写入成功」。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PagedLoadOutcomeTest {

    /** 可编程假数据源：按请求序号挂起指定调用以编排竞态时序。 */
    private class HoldSource {
        val pages = mutableMapOf<String?, PagedListPage<String>>()
        private val failures = mutableMapOf<String?, Throwable>()
        private val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()
        val calls = mutableListOf<String?>()

        fun hold(callIndex: Int): CompletableDeferred<Unit> =
            CompletableDeferred<Unit>().also { gates[callIndex] = it }

        fun release(callIndex: Int) {
            gates.getValue(callIndex).complete(Unit)
        }

        fun failOn(nextUrl: String?, error: Throwable) {
            failures[nextUrl] = error
        }

        suspend fun fetch(nextUrl: String?): PagedListPage<String> {
            calls.add(nextUrl)
            gates[calls.size]?.await()
            failures[nextUrl]?.let { throw it }
            return pages.getValue(nextUrl)
        }
    }

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

    // ---- PagedListState ----

    @Test
    fun loadFirstReportsWrittenThenSkippedThenWritten() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("a", "b"), "p1")
        val state = PagedListState(fetch = source::fetch)

        assertEquals(PagedLoadOutcome.Written, state.loadFirst(), "首屏写入必须报 Written")
        assertEquals(PagedLoadOutcome.Skipped, state.loadFirst(), "缓存命中早退必须报 Skipped")
        assertEquals(PagedLoadOutcome.Written, state.loadFirst(force = true), "强制刷新写入必须报 Written")
        // 早退那次不得发出请求：三次调用只有首屏与强制刷新两次 fetch
        assertEquals(2, source.calls.size)
    }

    @Test
    fun loadFirstReportsFailedWhenFetchThrows() = runTest {
        val source = HoldSource()
        val error = RuntimeException("network down")
        source.failOn(null, error)
        val state = PagedListState(fetch = source::fetch)

        assertEquals(PagedLoadOutcome.Failed, state.loadFirst())
        assertEquals("network down", state.initialError?.message)
    }

    @Test
    fun loadMoreReportsSkippedWithoutNextPage() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("a"), null)
        val state = PagedListState(fetch = source::fetch)
        state.loadFirst()

        assertEquals(PagedLoadOutcome.Skipped, state.loadMore())
        assertEquals(1, source.calls.size, "没有下一页时不得发出请求")
    }

    @Test
    fun loadMoreReportsSkippedWhileInFlight() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b"), "p2")
        val state = PagedListState(fetch = source::fetch)
        state.loadFirst()

        source.hold(2)
        val inFlight = async { state.loadMore() }
        runCurrent()

        assertEquals(PagedLoadOutcome.Skipped, state.loadMore(), "在途重复触发必须报 Skipped")

        source.release(2)
        assertEquals(PagedLoadOutcome.Written, inFlight.await())
    }

    @Test
    fun loadMoreReportsWrittenAndFailed() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b"), "p2")
        val state = PagedListState(fetch = source::fetch)
        state.loadFirst()

        assertEquals(PagedLoadOutcome.Written, state.loadMore())
        assertEquals(listOf("a", "b"), state.items)

        val pageError = RuntimeException("page failed")
        source.failOn("p2", pageError)
        assertEquals(PagedLoadOutcome.Failed, state.loadMore(), "翻页失败必须报 Failed 而非写入")
        assertEquals("page failed", state.loadMoreError?.message)
        assertEquals(listOf("a", "b"), state.items)
    }

    @Test
    fun staleLoadMoreReportsDiscarded() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("stale"), "stale-next")
        val state = PagedListState(fetch = source::fetch)
        state.loadFirst()

        source.hold(2)
        val stale = async { state.loadMore() }
        runCurrent()

        // 刷新递增 generation 并作废在途翻页
        source.pages[null] = PagedListPage(listOf("z"), "p2")
        assertEquals(PagedLoadOutcome.Written, state.loadFirst(force = true))

        source.release(2)
        assertEquals(PagedLoadOutcome.Discarded, stale.await(), "代际失配必须报 Discarded")
        assertEquals(listOf("z"), state.items)
        assertNull(state.loadMoreError)
    }

    @Test
    fun resetDuringLoadMoreReportsDiscarded() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b"), "p2")
        val state = PagedListState(fetch = source::fetch)
        state.loadFirst()

        source.hold(2)
        val stale = async { state.loadMore() }
        runCurrent()
        state.reset()

        source.release(2)
        assertEquals(PagedLoadOutcome.Discarded, stale.await())
        assertNull(state.items)
    }

    // ---- IllustPagedListState ----

    @Test
    fun illustLoadOutcomesDistinguishWrittenStaleAndFailed() = runTest {
        val pages = mutableMapOf<String?, IllustPagedPage>(
            null to IllustPagedPage(listOf(illust(1)), "p1"),
            "p1" to IllustPagedPage(listOf(illust(2)), "p2"),
        )
        var holdPage: CompletableDeferred<Unit>? = null
        var callIndex = 0
        val state = IllustPagedListState(
            fetch = { nextUrl ->
                callIndex++
                if (callIndex == 2) holdPage?.await()
                pages.getValue(nextUrl)
            },
            filter = { raw -> raw },
            filterVersionProvider = { 0 },
        )

        assertEquals(PagedLoadOutcome.Written, state.loadFirst())
        assertEquals(PagedLoadOutcome.Skipped, state.loadFirst(), "缓存命中早退必须报 Skipped")

        // 在途翻页被刷新作废：报 Discarded 且不写入
        holdPage = CompletableDeferred()
        val stale = async { state.loadMore() }
        runCurrent()
        assertEquals(PagedLoadOutcome.Written, state.loadFirst(force = true))
        holdPage!!.complete(Unit)
        assertEquals(PagedLoadOutcome.Discarded, stale.await())

        // 翻页失败：报 Failed
        val failing = IllustPagedListState(
            fetch = { nextUrl ->
                if (nextUrl == null) IllustPagedPage(listOf(illust(1)), "p1") else throw RuntimeException("boom")
            },
            filter = { raw -> raw },
            filterVersionProvider = { 0 },
        )
        failing.loadFirst()
        assertEquals(PagedLoadOutcome.Failed, failing.loadMore())
    }

    @Test
    fun restoreClearsErrorsAndDiscardsInFlightRequests() = runTest {
        val source = HoldSource()
        source.pages[null] = PagedListPage(listOf("stale"), "p1")
        source.failOn("bad", RuntimeException("boom"))
        val state = PagedListState(fetch = source::fetch)

        // 先触发首屏失败，使 initialError 非空
        source.failOn(null, RuntimeException("initial boom"))
        assertEquals(PagedLoadOutcome.Failed, state.loadFirst())

        // 发起新一次在途 loadFirst 后立即 restore：在途旧响应不得覆盖快照，且错误被清空
        source.pages[null] = PagedListPage(listOf("stale"), "p1")
        source.failOn(null, RuntimeException("never"))
        // Rebuild clean state for deterministic hold
        val source2 = HoldSource()
        source2.pages[null] = PagedListPage(listOf("stale-first"), "p-stale")
        val state2 = PagedListState(fetch = source2::fetch)
        source2.hold(1)
        val inFlight = async { state2.loadFirst(force = true) }
        runCurrent()

        state2.restore(listOf("restored"), "p-restored")
        source2.release(1)

        assertEquals(PagedLoadOutcome.Discarded, inFlight.await(), "restore 应递增 generation 作废在途 loadFirst")
        assertEquals(listOf("restored"), state2.items)
        assertNull(state2.initialError)
        assertNull(state2.loadMoreError)
    }
}
