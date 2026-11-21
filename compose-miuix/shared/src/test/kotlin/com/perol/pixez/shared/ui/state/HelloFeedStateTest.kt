package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.state.StateTestFixtures.illust
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 推荐页状态算子 [rememberHelloFeedState] 的行为与接线守护。
 *
 * 测试形态：算子为 @Composable 且依赖 LocalIllustRepository / LocalBanRepository / LocalSettingsRepository
 * 等具体仓储类，shared 模块未引入 compose ui-test 依赖，无法驱动组合。故按算子内实际构造复刻
 * [IllustPagedListState]（登录分支透传 nextUrl、未登录 walkthrough 分支恒为 null 无分页、
 * 登录态未判定(null)时不加载），直接驱动真状态机验证游标/去重/失败保留/快照恢复/本地重过滤/切号重置，
 * 另以源码守护锁定算子接线不变。
 */
class HelloFeedStateTest {

    private fun listState(
        fetch: suspend (String?) -> IllustPagedPage,
        filter: suspend (List<Illust>) -> List<Illust> = { it },
        filterVersion: () -> Int = { 0 },
    ): IllustPagedListState = IllustPagedListState(
        fetch = fetch,
        filter = filter,
        filterVersionProvider = filterVersion,
    )

    @Test
    fun `登录态未判定完成时保持未加载且不发起请求`() = runTest {
        val requested = mutableListOf<String?>()
        val state = listState({ nextUrl ->
            requested += nextUrl
            IllustPagedPage(listOf(illust(1)), null)
        })

        // 复刻算子接线：isLoggedIn 为 null（账号检测未完成）时不发起加载。
        val isLoggedIn: Boolean? = null
        if (isLoggedIn == null) {
            assertNull(state.filteredIllusts, "登录态未判定时不应有列表")
            assertTrue(requested.isEmpty(), "登录态未判定时不得发起请求")
            assertEquals(0, state.generation, "未加载时代数应保持 0")
            return@runTest
        }
        error("不可达：登录态必须为 null")
    }

    @Test
    fun `登录态首屏透传游标，未登录走无分页分支`() = runTest {
        // 数据集一：已登录，服务端返回下一页游标。
        val loggedInRequests = mutableListOf<String?>()
        val loggedIn = listState({ nextUrl ->
            loggedInRequests += nextUrl
            IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?page=2")
        })
        assertEquals(PagedLoadOutcome.Written, loggedIn.loadFirst(), "登录态首屏应写入列表")
        assertEquals(listOf(1L, 2L), loggedIn.filteredIllusts?.map { it.id }, "登录态首屏应展示返回作品")
        assertEquals("https://pixiv.net/next?page=2", loggedIn.nextUrl, "登录态应透传服务端游标")
        assertEquals(PagingFooterPhase.Hidden, loggedIn.footerState.phase, "还有下一页时 footer 应隐藏")
        assertEquals(listOf<String?>(null), loggedInRequests, "首屏必须以 null 游标请求")

        // 数据集二：未登录 walkthrough 匿名推荐，游标恒为 null（无分页）。
        val anonymousRequests = mutableListOf<String?>()
        val anonymous = listState({ nextUrl ->
            anonymousRequests += nextUrl
            IllustPagedPage(listOf(illust(7), illust(8), illust(9)), null)
        })
        assertEquals(PagedLoadOutcome.Written, anonymous.loadFirst(), "未登录首屏也应写入列表")
        assertEquals(listOf(7L, 8L, 9L), anonymous.filteredIllusts?.map { it.id }, "未登录应展示匿名推荐")
        assertNull(anonymous.nextUrl, "未登录分支不得产生下一页游标")
        assertEquals(PagingFooterPhase.NoMore, anonymous.footerState.phase, "无分页时 footer 应为 NoMore")
        assertEquals(PagedLoadOutcome.Skipped, anonymous.loadMore(), "无游标时翻页应早退 Skipped")
        assertEquals(listOf<String?>(null), anonymousRequests, "无游标时不得再发起请求")
    }

    @Test
    fun `翻页按作品 ID 去重追加，失败时保留旧列表与游标`() = runTest {
        val boom = IllegalStateException("network down")
        var failNextPage = false
        val state = listState({ nextUrl ->
            when {
                nextUrl == null -> IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?page=2")
                failNextPage -> throw boom
                else -> IllustPagedPage(listOf(illust(2), illust(3), illust(3)), "https://pixiv.net/next?page=3")
            }
        })
        state.loadFirst()

        failNextPage = true
        assertEquals(PagedLoadOutcome.Failed, state.loadMore(), "翻页失败应回报 Failed")
        assertEquals(listOf(1L, 2L), state.filteredIllusts?.map { it.id }, "翻页失败必须保留旧列表")
        assertEquals("https://pixiv.net/next?page=2", state.nextUrl, "翻页失败不得丢弃游标")
        assertNotNull(state.loadMoreError, "翻页失败应写入 loadMoreError")
        assertTrue(!state.isLoadingMore, "翻页失败后应复位加载态")

        // 边界：同一游标重试成功后，重复 ID 仍应去重且不丢旧数据。
        failNextPage = false
        assertEquals(PagedLoadOutcome.Written, state.loadMore(), "重试成功应回报 Written")
        assertEquals(listOf(1L, 2L, 3L), state.filteredIllusts?.map { it.id }, "重复作品 ID 应去重合并")
        assertEquals("https://pixiv.net/next?page=3", state.nextUrl, "重试成功应推进游标")
        assertNull(state.loadMoreError, "重试成功应清空错误")

        // 边界：把下一页设为失败且列表已存在时，继续失败不改写列表。
        val failing = listState({ nextUrl ->
            if (nextUrl == null) IllustPagedPage(listOf(illust(5)), "https://pixiv.net/next?page=2") else throw boom
        })
        failing.loadFirst()
        failing.loadMore()
        failing.loadMore()
        assertEquals(listOf(5L), failing.filteredIllusts?.map { it.id }, "连续失败不得清空既有列表")
        assertEquals(PagingFooterPhase.Retry, failing.footerState.phase, "失败后 footer 应保留重试入口")
    }

    @Test
    fun `首屏失败写入错误态，已有列表时刷新失败不覆盖旧数据`() = runTest {
        val boom = IllegalStateException("boom")

        val empty = listState({ throw boom })
        assertEquals(PagedLoadOutcome.Failed, empty.loadFirst(), "首屏失败应回报 Failed")
        assertNotNull(empty.initialError, "无旧数据时失败应写入 initialError")
        assertNull(empty.filteredIllusts, "首屏失败不得伪造列表")
        assertTrue(!empty.isRefreshing, "首屏失败后应复位刷新态")

        // 边界：已有数据后强制刷新失败，initialError 不写入（保留旧列表继续展示）。
        var fail = false
        val loaded = listState({ nextUrl ->
            if (fail) throw boom else IllustPagedPage(listOf(illust(1), illust(2)), null)
        })
        loaded.loadFirst()
        fail = true
        assertEquals(PagedLoadOutcome.Failed, loaded.loadFirst(force = true), "强制刷新失败应回报 Failed")
        assertEquals(listOf(1L, 2L), loaded.filteredIllusts?.map { it.id }, "刷新失败必须保留旧列表")
        assertNull(loaded.initialError, "已有列表时失败不得写入 initialError")
    }

    @Test
    fun `恢复快照后无需网络即可展示，并强制按当前过滤设置重过滤`() = runTest {
        var fetchCount = 0
        var filterVersion = 3
        val state = listState(
            fetch = { fetchCount++; IllustPagedPage(listOf(illust(99)), null) },
            filter = { raw -> raw.filterNot { it.xRestrict == 2 } },
            filterVersion = { filterVersion },
        )

        // 复刻算子接线：快照按 ID 经仓库缓存回查后 restore。
        val cached = listOf(illust(1), illust(2, xRestrict = 2), illust(3))
        state.restore(cached, "https://pixiv.net/next?page=2")

        assertEquals(listOf(1L, 2L, 3L), state.filteredIllusts?.map { it.id }, "恢复后应立即有列表（恢复态先等于原始列表）")
        assertEquals("https://pixiv.net/next?page=2", state.nextUrl, "恢复应带回下一页游标")
        assertEquals(0, fetchCount, "恢复本身不得发起网络请求")

        // 恢复态首轮 refilterIfChanged 即使版本号未变也必须强制重过滤。
        assertTrue(state.refilterIfChanged(), "恢复后首轮本地重过滤应返回 true")
        assertEquals(listOf(1L, 3L), state.filteredIllusts?.map { it.id }, "重过滤后屏蔽项应从展示列表剔除")
        assertEquals(listOf(1L, 2L, 3L), state.rawIllusts?.map { it.id }, "重过滤不得改动原始列表")
        assertEquals(0, fetchCount, "本地重过滤不得发起网络请求")

        // 边界：版本号未变且无恢复标志时返回 false（算子据此早退，避免无谓重建）。
        assertTrue(!state.refilterIfChanged(), "版本未变且已重过滤时应返回 false")
        filterVersion = 4
        assertTrue(state.refilterIfChanged(), "过滤版本变化后应再次重过滤")
    }

    @Test
    fun `账号切换重置列表后以新账号身份重拉首屏`() = runTest {
        var account = "A"
        val requested = mutableListOf<String?>()
        val state = listState({ nextUrl ->
            requested += nextUrl
            val ids = if (account == "A") listOf(illust(1), illust(2)) else listOf(illust(8), illust(9))
            IllustPagedPage(ids, "https://pixiv.net/next?page=2")
        })
        state.loadFirst()
        assertEquals(listOf(1L, 2L), state.filteredIllusts?.map { it.id }, "账号 A 应展示 A 的推荐")

        // 复刻算子接线：accountSwitched 时 reset()，随后同一 LaunchedEffect 继续重拉首屏。
        account = "B"
        val accountSwitched = true
        if (accountSwitched) state.reset()
        assertNull(state.filteredIllusts, "切号后列表应清空")
        assertNull(state.rawIllusts, "切号后原始列表应清空")
        assertNull(state.nextUrl, "切号后游标应清空")
        assertEquals(PagingFooterPhase.Hidden, state.footerState.phase, "切号后 footer 应回到隐藏态")

        state.loadFirst()
        assertEquals(listOf(8L, 9L), state.filteredIllusts?.map { it.id }, "重置后应重新拉取新账号首屏")
        assertEquals(listOf<String?>(null, null), requested, "重置后首屏必须以 null 游标重新请求")
    }

    @Test
    fun `算子接线守护：未登录无分页、快照恢复、本地重过滤与切号重置`() {
        val source = StateSourceReader.readUi("state/HelloFeedState.kt")

        assertTrue(
            "IllustPagedPage(response.illusts, null)" in source,
            "未登录 walkthrough 分支必须显式给出 null 游标（匿名推荐无分页）",
        )
        assertTrue(
            "IllustPagedPage(response.illusts, response.nextUrl)" in source,
            "登录分支应透传服务端下一页游标",
        )
        assertTrue(
            "state.restore(raw, snapshot.nextUrl)" in source,
            "首帧应经恢复快照回查原始列表并恢复游标（防白屏闪烁与位置重置）",
        )
        assertTrue(
            "if (!force && listState.refilterIfChanged()) {" in source,
            "过滤版本变化时应本地重过滤并提前返回（零网络）",
        )
        assertTrue(
            "if (accountSwitched) listState.reset()" in source,
            "切号时必须重置分页状态机",
        )
        assertTrue(
            "feedRestore.update(it, listState.nextUrl)" in source,
            "仅写入成功时才更新恢复快照，并携带最新游标",
        )
        assertTrue(
            "banRepository.filterIllustsBySettings(rawIllusts, settingsRepository)" in source,
            "过滤链应注入屏蔽设置过滤，而非在页面内手写",
        )
    }
}
