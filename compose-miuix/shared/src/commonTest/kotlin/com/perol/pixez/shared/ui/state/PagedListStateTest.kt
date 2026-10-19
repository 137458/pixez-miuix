package com.perol.pixez.shared.ui.state

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 通用分页状态机断言：generation 竞态守卫 + 复位 isLoadingMore + mergeItems 合并去重 +
 * 作用域取消传播，基准语义与 IllustPagedListStateTest 一致；条目用 String 以隔离数据模型。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PagedListStateTest {

    /**
     * 可编程假数据源：按请求序号（从 1 计）挂起指定调用以编排竞态时序，
     * 每个测试自建实例，不跨用例共享状态。
     */
    private class FakePagedSource {
        /** nextUrl → 该页响应；null 键为首页，可覆写以模拟刷新后返回新数据。 */
        val pages = mutableMapOf<String?, PagedListPage<String>>()
        private val failures = mutableMapOf<String?, Throwable>()
        private val heldCalls = mutableMapOf<Int, CompletableDeferred<Unit>>()

        /** 已发出的请求参数序列（从首页开始），用于断言「未触发请求」。 */
        val calls = mutableListOf<String?>()

        /** 通过闸门后继续执行的 fetch 次数：作用域取消后必须停在 0。 */
        var resumedAfterGate = 0

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

        suspend fun fetch(nextUrl: String?): PagedListPage<String> {
            calls.add(nextUrl)
            heldCalls[calls.size]?.await()
            resumedAfterGate++
            failures[nextUrl]?.let { throw it }
            return pages.getValue(nextUrl)
        }
    }

    private fun newState(
        source: FakePagedSource,
        mergeItems: (List<String>, List<String>) -> List<String> = { old, new -> old + new },
    ): PagedListState<String> = PagedListState(
        fetch = source::fetch,
        mergeItems = mergeItems,
    )

    // ---- 常规组 ----

    @Test
    fun loadFirstSuccessReplacesListAndUpdatesNextUrl() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a1", "a2"), "p1")

        state.loadFirst()

        assertEquals(listOf("a1", "a2"), state.items)
        assertEquals("p1", state.nextUrl)
        assertNull(state.initialError)
        assertFalse(state.isRefreshing)
        assertEquals(1, state.generation)

        // 差异化第二组数据：force 刷新必须整体替换而非追加，翻页不递增 generation
        source.pages[null] = PagedListPage(listOf("z1"), null)
        state.loadFirst(force = true)

        assertEquals(listOf("z1"), state.items)
        assertNull(state.nextUrl)
        assertFalse(state.isRefreshing)
        assertEquals(2, state.generation)
    }

    @Test
    fun loadMoreAppendsViaMergeItemsAndUpdatesNextUrl() = runTest {
        val source = FakePagedSource()
        // 模拟各数据模型的 appendDistinct：按值去重合并
        val state = newState(source) { old, new -> old + new.filter { it !in old } }
        source.pages[null] = PagedListPage(listOf("a", "b"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b", "c"), "p2")
        source.pages["p2"] = PagedListPage(listOf("c", "d"), null)

        state.loadFirst()
        state.loadMore()

        assertEquals(listOf("a", "b", "c"), state.items)
        assertEquals("p2", state.nextUrl)

        state.loadMore()

        assertEquals(listOf("a", "b", "c", "d"), state.items)
        assertNull(state.nextUrl)
        assertNull(state.loadMoreError)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun loadMoreFailureSetsErrorAndResetsLoadingState() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        val pageError = RuntimeException("page fetch failed")
        source.failOn("p1", pageError)

        state.loadFirst()
        state.loadMore()

        assertEquals(pageError.message, state.loadMoreError?.message)
        assertFalse(state.isLoadingMore)
        assertEquals(listOf("a"), state.items)
        assertEquals("p1", state.nextUrl)
        assertNull(state.initialError)
    }

    @Test
    fun loadMoreWithoutNextUrlDoesNotFetch() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), null)
        state.loadFirst()
        val callsAfterFirst = source.calls.size

        state.loadMore()

        assertEquals(callsAfterFirst, source.calls.size)
        assertFalse(state.isLoadingMore)
        assertNull(state.loadMoreError)
        assertEquals(listOf("a"), state.items)
    }

    @Test
    fun concurrentLoadMoreWhileLoadingIsRejected() = runTest {
        val source = FakePagedSource()
        // 与生产 Screen 一致注入 appendDistinct：页间重叠条目按值去重，本用例只验证在途拒绝
        val state = newState(source) { old, new -> old + new.filter { it !in old } }
        source.pages[null] = PagedListPage(listOf("a", "b"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b", "c"), "p2")
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
        assertEquals(listOf("a", "b", "c"), state.items)
        assertFalse(state.isLoadingMore)
    }

    // ---- reset 组（登录/账号切换作废旧账号数据） ----

    @Test
    fun resetClearsStateAndInvalidatesInFlightLoadMore() = runTest {
        val source = FakePagedSource()
        val state = newState(source) { old, new -> old + new.filter { it !in old } }
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b"), null)
        state.loadFirst()
        assertEquals(listOf("a"), state.items)
        val generationBeforeReset = state.generation

        source.hold(2)
        val loadMoreJob = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)

        state.reset()

        assertNull(state.items)
        assertNull(state.nextUrl)
        assertFalse(state.isLoadingMore)
        assertFalse(state.isRefreshing)
        assertNull(state.initialError)
        assertNull(state.loadMoreError)
        assertEquals(generationBeforeReset + 1, state.generation)

        source.release(2)
        loadMoreJob.join()

        // 过期翻页结果不得写入；reset 自身不触发请求（calls 停留在首页 + 翻页两次）
        assertNull(state.items)
        assertEquals(2, source.calls.size)
    }

    @Test
    fun resetInvalidatesInFlightRefreshResult() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        state.loadFirst()

        source.pages[null] = PagedListPage(listOf("new"), null)
        source.hold(2)
        val refreshJob = launch { state.loadFirst(force = true) }
        runCurrent()
        assertTrue(state.isRefreshing)
        // 在途刷新自身的 ++generation 此时已生效，基线必须锚定在它之后
        val generationWhileRefreshInFlight = state.generation

        state.reset()

        source.release(2)
        refreshJob.join()

        // 在途刷新结果整体作废：不写入列表，也不得遗留刷新标志
        assertNull(state.items)
        assertNull(state.nextUrl)
        assertFalse(state.isRefreshing)
        assertEquals(generationWhileRefreshInFlight + 1, state.generation)
        assertEquals(2, source.calls.size)
    }

    @Test
    fun loadFirstAfterResetRefetchesInsteadOfEarlyReturn() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), null)
        state.loadFirst()
        assertEquals(listOf("a"), state.items)

        // 模拟换账号后数据源变化：reset 后 loadFirst 必须重新拉取而非早退命中既有列表
        source.pages[null] = PagedListPage(listOf("z"), null)
        state.reset()
        state.loadFirst()

        assertEquals(listOf("z"), state.items)
        assertEquals(2, source.calls.size)
    }

    @Test
    fun loadFirstFailureWithoutDataSetsInitialErrorThenRetryRecovers() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        val initialFailure = RuntimeException("network down")
        source.failOn(null, initialFailure)

        state.loadFirst()

        assertEquals("network down", state.initialError?.message)
        assertNull(state.items)
        assertFalse(state.isRefreshing)

        // 重试：失败不遗留数据，再次 loadFirst 不得因早退跳过
        source.clearFailure(null)
        source.pages[null] = PagedListPage(listOf("r1"), "p1")
        state.loadFirst()

        assertNull(state.initialError)
        assertEquals(listOf("r1"), state.items)
        assertEquals("p1", state.nextUrl)
    }

    @Test
    fun loadFirstFailureWithDataKeepsExistingList() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        state.loadFirst()
        source.failOn(null, RuntimeException("refresh failed"))

        state.loadFirst(force = true)

        // 已有数据时刷新失败不得写入 initialError，旧列表原样保留
        assertNull(state.initialError)
        assertEquals(listOf("a"), state.items)
        assertEquals("p1", state.nextUrl)
        assertFalse(state.isRefreshing)
    }

    // ---- 竞态组 ----

    @Test
    fun inFlightLoadMoreIsRejectedAfterRefreshBumpsGeneration() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("s1"), "stale-next")
        state.loadFirst()

        // 旧翻页在途（挂在第 2 次 fetch）
        source.hold(2)
        val staleLoadMore = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)

        // 刷新递增 generation 并复位 isLoadingMore，返回新首页
        source.pages[null] = PagedListPage(listOf("z"), "p2")
        val refreshed = launch { state.loadFirst(force = true) }
        runCurrent()
        assertFalse(state.isLoadingMore)
        assertEquals(listOf("z"), state.items)
        assertEquals("p2", state.nextUrl)

        // 放行旧翻页：其结果与错误必须被 generation 守卫整体丢弃
        source.release(2)
        staleLoadMore.join()
        refreshed.join()

        assertEquals(listOf("z"), state.items)
        assertEquals("p2", state.nextUrl)
        assertNull(state.loadMoreError)
        assertFalse(state.isLoadingMore)
        assertNull(state.initialError)
    }

    @Test
    fun staleLoadMoreMustNotResetFlagWhileNewLoadMoreInFlight() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        state.loadFirst()

        // 旧一代（gen 1）翻页在途
        source.pages["p1"] = PagedListPage(listOf("s1"), "stale-next")
        source.hold(2)
        val stale = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)

        // 刷新（gen 2）完成：复位 isLoadingMore，换新首页与新游标
        source.pages[null] = PagedListPage(listOf("z"), "p2")
        val refreshed = launch { state.loadFirst(force = true) }
        runCurrent()
        refreshed.join()
        assertEquals(listOf("z"), state.items)
        assertEquals("p2", state.nextUrl)
        assertFalse(state.isLoadingMore)

        // 新一代（gen 2）翻页在途
        source.pages["p2"] = PagedListPage(listOf("n1"), "p3")
        source.hold(4)
        val fresh = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)

        // 放行旧一代翻页：结果丢弃，且不得清除新翻页的在途标志
        source.release(2)
        stale.join()

        assertEquals(listOf("z"), state.items)
        assertEquals("p2", state.nextUrl)
        assertTrue(state.isLoadingMore)
        assertNull(state.loadMoreError)

        // 新翻页完成后写入并复位
        source.release(4)
        fresh.join()
        assertEquals(listOf("z", "n1"), state.items)
        assertEquals("p3", state.nextUrl)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun scopeCancellationStopsInFlightFetchAndDiscardsResult() = runTest {
        val source = FakePagedSource()
        val state = newState(source)
        source.pages[null] = PagedListPage(listOf("a"), "p1")
        source.pages["p1"] = PagedListPage(listOf("b"), "p2")
        state.loadFirst()

        source.hold(2)
        val job = launch { state.loadMore() }
        runCurrent()
        assertTrue(state.isLoadingMore)
        // 基线：首页 fetch 无闸门直接通过，计数为 1；翻页挂在闸门上未通过
        assertEquals(1, source.resumedAfterGate)

        // 模拟离开页面销毁 rememberCoroutineScope：在途请求被取消后不再恢复、不写入
        job.cancel()
        source.release(2)
        runCurrent()
        job.join()

        assertEquals(1, source.resumedAfterGate)
        assertEquals(listOf("a"), state.items)
        assertEquals("p1", state.nextUrl)
    }

    // ---- 组合透传 ----

    @Test
    fun pagingFlagsAreComposeSnapshotStates() {
        val source = FakePagedSource()
        val state = newState(source)
        val observed = mutableSetOf<Any>()
        val snapshot = Snapshot.takeSnapshot(readObserver = { observed.add(it) })
        snapshot.enter {
            state.items
            state.nextUrl
            state.initialError
            state.isRefreshing
            state.isLoadingMore
            state.loadMoreError
        }
        snapshot.dispose()

        // 六个分页标志必须是六个不同的快照状态对象：UI 在组合期直读，普通 var 写入不会触发重组。
        assertEquals(6, observed.size)
    }
}
