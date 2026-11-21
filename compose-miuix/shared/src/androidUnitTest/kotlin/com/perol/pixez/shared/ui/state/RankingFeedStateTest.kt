package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.ui.state.StateTestFixtures.illust
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 排行榜页状态算子 [rememberRankingFeedState] 的行为与接线守护。
 *
 * 测试形态：算子为 @Composable 且依赖 LocalIllustRepository / LocalSettingsRepository 等具体仓储类，
 * shared 模块未引入 compose ui-test 依赖，无法驱动组合。故按算子内实际构造复刻
 * [IllustPagedListState]（以榜单模式代码 + 日期为 remember key，恢复缓存键同源）并直接驱动真状态机，
 * 验证榜单隔离、缓存键唯一性、快照恢复、早退与失败语义；另以源码守护锁定算子接线。
 */
class RankingFeedStateTest {

    private fun listState(
        fetch: suspend (String?) -> IllustPagedPage,
        filter: suspend (List<Illust>) -> List<Illust> = { it },
        filterVersion: () -> Int = { 0 },
    ): IllustPagedListState = IllustPagedListState(
        fetch = fetch,
        filter = filter,
        filterVersionProvider = filterVersion,
    )

    /** 复刻算子内的恢复缓存键拼接：模式代码 + 日期（未选日期即为空串）。 */
    private fun rankingCacheKey(mode: RankingMode, date: String?): String = "${mode.code}_${date.orEmpty()}"

    private fun IllustPagedListState.ids(): List<Long>? = filteredIllusts?.map { it.id }

    @Test
    fun `十二个榜单模式的代码表唯一且与接口取值一致`() {
        val expected = mapOf(
            RankingMode.DAY to "day",
            RankingMode.WEEK to "week",
            RankingMode.MONTH to "month",
            RankingMode.DAY_MALE to "day_male",
            RankingMode.DAY_FEMALE to "day_female",
            RankingMode.WEEK_ORIGINAL to "week_original",
            RankingMode.DAY_ROOKIE to "day_rookie",
            RankingMode.DAY_AI to "day_ai",
            RankingMode.DAY_R18_AI to "day_r18_ai",
            RankingMode.DAY_R18 to "day_r18",
            RankingMode.WEEK_R18 to "week_r18",
            RankingMode.WEEK_R18G to "week_r18g",
        )
        assertEquals(expected.size, RankingMode.entries.size, "榜单模式数量应与接口一致")
        expected.forEach { (mode, code) ->
            assertEquals(code, mode.code, "榜单模式 $mode 的接口代码应为 $code")
        }
        assertEquals(
            RankingMode.entries.size,
            RankingMode.entries.map { it.code }.toSet().size,
            "榜单模式代码必须唯一，否则恢复缓存键会互相覆盖",
        )
    }

    @Test
    fun `恢复缓存键由模式代码与日期拼接，不同榜单与日期互不覆盖`() {
        // 常规：同一模式同一日期稳定映射到同一键（重组后命中同一份恢复快照）。
        assertEquals("day_", rankingCacheKey(RankingMode.DAY, null), "未选日期时键应由模式代码加空日期构成")
        assertEquals("day_", rankingCacheKey(RankingMode.DAY, ""), "空字符串日期与 null 应产生同一键")
        assertEquals(
            rankingCacheKey(RankingMode.DAY, "2026-11-15"),
            rankingCacheKey(RankingMode.DAY, "2026-11-15"),
            "相同模式与日期必须产生相同恢复缓存键",
        )

        // 边界：不同模式或不同日期必须产生不同键，否则切榜后恢复出别的榜单数据。
        val keys = listOf(
            rankingCacheKey(RankingMode.DAY, "2026-11-15"),
            rankingCacheKey(RankingMode.WEEK, "2026-11-15"),
            rankingCacheKey(RankingMode.DAY, "2026-11-16"),
            rankingCacheKey(RankingMode.WEEK_ORIGINAL, "2026-11-15"),
            rankingCacheKey(RankingMode.DAY, null),
        )
        assertEquals(keys.size, keys.toSet().size, "不同榜单/日期组合的恢复缓存键不得重复：$keys")
        assertEquals(
            "week_original_2026-11-15",
            rankingCacheKey(RankingMode.WEEK_ORIGINAL, "2026-11-15"),
            "键格式应为 模式代码_日期",
        )
    }

    @Test
    fun `模式或日期变化时状态机整体重建，新榜单从空态开始`() = runTest {
        // 复刻 remember(selectedMode, selectedDate)：不同键各自一份状态机。
        val dayState = listState({ nextUrl ->
            if (nextUrl == null) IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?day=2")
            else IllustPagedPage(listOf(illust(3)), null)
        })
        dayState.loadFirst()
        dayState.loadMore()
        assertEquals(listOf(1L, 2L, 3L), dayState.ids(), "日榜应累积两页数据")

        // 切换到周榜：remember key 变化 => 新建状态机，不得复用日榜数据。
        val weekState = listState({ IllustPagedPage(listOf(illust(21), illust(22)), null) })
        assertNull(weekState.filteredIllusts, "切换榜单后新状态机应从空态开始")
        assertNull(weekState.nextUrl, "切换榜单后不得继承旧榜游标")
        weekState.loadFirst()
        assertEquals(listOf(21L, 22L), weekState.ids(), "周榜应只展示自身数据")

        // 边界：切回日榜同样重建状态机（remember 块重新执行），因此需要重新拉首屏。
        val dayAgain = listState({ IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?day=2") })
        assertNull(dayAgain.filteredIllusts, "切回原榜单也应是重建后的空状态机")
        assertEquals(listOf(1L, 2L, 3L), dayState.ids(), "旧状态机实例仍持有旧数据（仅作废，不被新实例复用）")
    }

    @Test
    fun `恢复快照恢复首屏与游标，空快照也恢复为空态并早退`() = runTest {
        var fetchCount = 0
        val restored = listState(
            fetch = { fetchCount++; IllustPagedPage(listOf(illust(50)), null) },
            filter = { raw -> raw.filterNot { it.xRestrict == 2 } },
        )
        restored.restore(listOf(illust(1), illust(2, xRestrict = 2)), "https://pixiv.net/next?page=2")
        assertEquals(listOf(1L, 2L), restored.ids(), "恢复后应立即可展示（恢复态先等于原始列表）")
        assertEquals("https://pixiv.net/next?page=2", restored.nextUrl, "恢复应带回下一页游标")
        assertTrue(restored.refilterIfChanged(), "恢复后首轮应强制本地重过滤")
        assertEquals(listOf(1L), restored.ids(), "重过滤后屏蔽项应被剔除")
        assertEquals(0, fetchCount, "恢复与重过滤均不得发起网络请求")

        // 边界：空快照（illustIds 为空）在算子条件 snapshot.illustIds.isEmpty() 下同样 restore，
        // 得到空列表 + 空游标；此时状态机早退不再请求，避免重复拉取。
        val emptySnapshot = listState({ fetchCount++; IllustPagedPage(listOf(illust(50)), null) })
        emptySnapshot.restore(emptyList(), null)
        assertEquals(emptyList<Long>(), emptySnapshot.ids(), "空快照应恢复为空列表")
        assertNull(emptySnapshot.nextUrl, "空快照不应带出游标")
        assertEquals(PagingFooterPhase.Hidden, emptySnapshot.footerState.phase, "空列表 footer 应隐藏")
        assertEquals(PagedLoadOutcome.Skipped, emptySnapshot.loadFirst(), "已有（空）列表且无错误时首屏应早退")
        assertEquals(0, fetchCount, "早退不得发起请求")
    }

    @Test
    fun `已有榜单数据时重复进入早退不重复请求，强制刷新才重拉`() = runTest {
        val requested = mutableListOf<String?>()
        var head = 1L
        val state = listState({ nextUrl ->
            requested += nextUrl
            IllustPagedPage(listOf(illust(head), illust(head + 1)), null)
        })
        assertEquals(PagedLoadOutcome.Written, state.loadFirst(), "首次进入应加载")
        assertEquals(PagedLoadOutcome.Skipped, state.loadFirst(), "已有数据时（重组/重试点击）应早退 Skipped")
        assertEquals(listOf<String?>(null), requested, "早退不得产生额外请求")

        // 边界：强制刷新（手动下拉）必须重拉，并以新首屏替换旧列表。
        head = 10L
        assertEquals(PagedLoadOutcome.Written, state.loadFirst(force = true), "强制刷新应写入")
        assertEquals(listOf(10L, 11L), state.ids(), "强制刷新应以新首屏替换旧列表")
        assertEquals(listOf<String?>(null, null), requested, "强制刷新同样从首页拉取")

        // 边界：翻页失败保留旧列表并暴露重试入口。
        val boom = IllegalStateException("ranking down")
        var failNext = false
        val failing = listState({ nextUrl ->
            if (failNext) throw boom else IllustPagedPage(listOf(illust(1), illust(2)), "https://pixiv.net/next?page=2")
        })
        failing.loadFirst()
        failNext = true
        assertEquals(PagedLoadOutcome.Failed, failing.loadMore(), "翻页失败应回报 Failed")
        assertEquals(listOf(1L, 2L), failing.ids(), "翻页失败必须保留旧列表")
        assertEquals(PagingFooterPhase.Retry, failing.footerState.phase, "失败后 footer 应保留重试入口")
    }

    @Test
    fun `算子接线守护：缓存键、remember key、快照恢复与本地重过滤`() {
        val source = StateSourceReader.readUi("state/RankingFeedState.kt")

        assertTrue("val rankingCacheKey" in source, "恢复缓存键应在算子内单点构造")
        assertTrue("selectedDate.orEmpty()" in source, "缓存键中的日期应把 null 归一为空串")
        assertTrue("remember(selectedMode, selectedDate) {" in source, "状态机应以榜单模式与日期为 remember key 整体重建")
        assertTrue("feedRestore.get(rankingCacheKey)" in source, "进入榜单应按缓存键读取恢复快照")
        assertTrue("state.restore(raw, snapshot.nextUrl)" in source, "应经仓库缓存回查原始列表并恢复游标")
        assertTrue(
            "feedRestore.update(rankingCacheKey, it, listState.nextUrl)" in source,
            "仅写入成功时按缓存键更新恢复快照并携带游标",
        )
        assertTrue("if (!force && listState.refilterIfChanged()) {" in source, "过滤版本变化时应本地重过滤并提前返回")
        assertTrue("mode = selectedMode.code," in source, "请求应使用榜单模式代码")
        assertTrue("date = selectedDate," in source, "请求应使用防抖校验后的日期")
    }
}
