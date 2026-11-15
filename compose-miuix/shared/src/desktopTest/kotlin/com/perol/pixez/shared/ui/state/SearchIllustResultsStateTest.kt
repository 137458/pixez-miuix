package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.screens.applySearchClientFilters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * 作品搜索结果状态（rememberSearchIllustResultsState，SearchResultsState.kt）的行为测试。
 *
 * 为什么不能直接驱动算子本身：算子是 @Composable，依赖 LocalAccountRepository 与具体仓储
 * （SearchRepository / BanRepository / SettingsRepository），shared 未引入 compose ui-test，
 * 纯 JVM 测试无法启动组合，也无法打桩这些具体类。
 *
 * 分层锁定：
 * 1. 真实生产函数直测：applySearchClientFilters（展示阶段的动图/比例本地过滤）。
 * 2. 分层状态机直测：算子内部真正的分页状态机 IllustPagedListState 以 suspend lambda 注入，
 *    可在 JVM 上以「记录请求 + 固定返回」的假请求源驱动，覆盖首屏、追加去重、失败保留、
 *    reset 清空、refilterIfChanged 零网络重过滤。
 * 3. 内联逻辑复刻 + 源码接线守护：searchWord 构造、切号重置顺序、refilter 提前返回等
 *    内联在算子体内的逻辑按源码同语义复刻测试，并由守护测试断言源码原文仍在。
 */
class SearchIllustResultsStateTest {

    // ---------------------------------------------------------------- 复刻逻辑与假请求源

    /**
     * 复刻 SearchResultsState.kt:67-70 的搜索词构造：先清除 `\s*\d+users入り` 后缀，
     * 再按收藏阈值追加 `" Nusers入り"`，避免重复筛选词叠加。
     */
    private fun buildSearchWord(query: String, bookmarkThreshold: Int): String {
        val cleanQuery = query.replace(Regex("""\s*\d+users入り"""), "").trim()
        return if (bookmarkThreshold > 0) "$cleanQuery ${bookmarkThreshold}users入り" else cleanQuery
    }

    /** 一次搜索请求的可观测参数：搜索词、排序、游标。 */
    private data class SearchRequest(val word: String, val sort: String, val nextUrl: String?)

    /** 记录算子发出的每次请求（含游标），并以固定页面回应；callCount 用于断言「零网络」。 */
    private class RecordingSearchFetch(
        private val handler: (SearchRequest) -> IllustPagedPage,
    ) {
        val requests = mutableListOf<SearchRequest>()
        var callCount = 0
            private set

        suspend fun fetch(nextUrl: String?, word: String, sort: String): IllustPagedPage {
            callCount++
            val request = SearchRequest(word, sort, nextUrl)
            requests += request
            return handler(request)
        }
    }

    /**
     * 按算子构造方式组装分页状态机：fetch 透传搜索词与游标，filter 复刻
     * `banRepository.filterIllustsBySettings` 的双列表本地过滤（此处以 xRestrict 屏蔽替代设置读取）。
     */
    private fun listStateFor(
        fetch: RecordingSearchFetch,
        word: String,
        sort: String = "date_desc",
        filterVersionProvider: () -> Int = { 0 },
        hideR18: () -> Boolean = { true },
    ): IllustPagedListState = IllustPagedListState(
        fetch = { nextUrl -> fetch.fetch(nextUrl, word, sort) },
        filter = { raw -> if (hideR18()) raw.filterNot { it.xRestrict > 0 } else raw },
        filterVersionProvider = filterVersionProvider,
    )

    private fun IllustPagedListState.ids(): List<Long>? = filteredIllusts?.map { it.id }

    // ---------------------------------------------------------------- 搜索词构造

    @Test
    fun `按收藏阈值构建搜索词并替换既有 users入り 后缀`() {
        val cases = listOf(
            Triple("初音ミク", 0, "初音ミク"),
            Triple("初音ミク", 1000, "初音ミク 1000users入り"),
            Triple("初音ミク 500users入り", 1000, "初音ミク 1000users入り"),
            Triple("初音ミク 500users入り", 0, "初音ミク"),
            Triple("  初音ミク 10users入り  ", 100, "初音ミク 100users入り"),
        )
        for ((query, threshold, expected) in cases) {
            assertEquals(
                expected,
                buildSearchWord(query, threshold),
                "query=$query, 阈值=$threshold 时应构造出 $expected",
            )
        }
    }

    @Test
    fun `空查询词与无空格后缀等边界仍按同一规则处理`() {
        assertEquals("", buildSearchWord("", 0), "空查询词且无阈值时搜索词保持为空")
        assertEquals("初音ミク", buildSearchWord("初音ミク10000users入り", 0), "无空格后缀同样应被清除")
        // 阈值恰好为 1（最小正阈值）是「是否追加后缀」的分界点。
        assertEquals("初音ミク 1users入り", buildSearchWord("初音ミク", 1), "阈值 1 应追加后缀")
        assertEquals("初音ミク", buildSearchWord("初音ミク", 0), "阈值 0 应清除后缀")
    }

    // ---------------------------------------------------------------- 展示阶段客户端过滤

    @Test
    fun `ugoira 筛选按保留或排除动图两档生效`() {
        val page = listOf(
            StateTestFixtures.illust(1, type = "illust"),
            StateTestFixtures.illust(2, type = "ugoira"),
            StateTestFixtures.illust(3, type = "ugoira"),
            StateTestFixtures.illust(4, type = "manga"),
        )
        assertEquals(
            listOf(2L, 3L),
            applySearchClientFilters(page, ugoira = 1, ratio = 0).map { it.id },
            "ugoira=1 应只保留动图作品",
        )
        assertEquals(
            listOf(1L, 4L),
            applySearchClientFilters(page, ugoira = 2, ratio = 0).map { it.id },
            "ugoira=2 应排除动图作品",
        )
        assertEquals(
            listOf(1L, 2L, 3L, 4L),
            applySearchClientFilters(page, ugoira = 0, ratio = 0).map { it.id },
            "ugoira=0 时不过滤动图",
        )
        // 边界：页面里没有动图时按保留过滤应得到空列表。
        val noUgoira = listOf(StateTestFixtures.illust(9))
        assertTrue(
            applySearchClientFilters(noUgoira, ugoira = 1, ratio = 0).isEmpty(),
            "无动图页面在 ugoira=1 时应被过滤为空",
        )
    }

    @Test
    fun `长宽比筛选按横图竖图方图三档生效`() {
        val page = listOf(
            StateTestFixtures.illust(1, width = 2000, height = 1000),
            StateTestFixtures.illust(2, width = 1000, height = 2000),
            StateTestFixtures.illust(3, width = 1000, height = 1000),
            StateTestFixtures.illust(4, width = 1500, height = 500),
        )
        assertEquals(
            listOf(1L, 4L),
            applySearchClientFilters(page, ugoira = 0, ratio = 1).map { it.id },
            "ratio=1 应只保留横图（宽 > 高）",
        )
        assertEquals(
            listOf(2L),
            applySearchClientFilters(page, ugoira = 0, ratio = 2).map { it.id },
            "ratio=2 应只保留竖图（高 > 宽）",
        )
        assertEquals(
            listOf(3L),
            applySearchClientFilters(page, ugoira = 0, ratio = 3).map { it.id },
            "ratio=3 应只保留方图（宽 == 高）",
        )
        assertEquals(4, applySearchClientFilters(page, ugoira = 0, ratio = 0).size, "ratio=0 时不过滤")
    }

    @Test
    fun `ugoira 与长宽比组合过滤先动图后比例且可为空`() {
        val page = listOf(
            StateTestFixtures.illust(1, type = "illust", width = 2000, height = 1000),
            StateTestFixtures.illust(2, type = "ugoira", width = 1000, height = 2000),
            StateTestFixtures.illust(3, type = "ugoira", width = 1000, height = 1000),
            StateTestFixtures.illust(4, type = "illust", width = 1000, height = 1000),
        )
        assertEquals(
            emptyList(),
            applySearchClientFilters(page, ugoira = 1, ratio = 1).map { it.id },
            "动图均为竖图/方图时，组合条件应得到空列表（边界）",
        )
        assertEquals(
            listOf(4L),
            applySearchClientFilters(page, ugoira = 2, ratio = 3).map { it.id },
            "排除动图后再取方图应只剩 id=4",
        )
    }

    @Test
    fun `展示阶段客户端过滤作用在状态机过滤结果之上`() {
        val page = listOf(
            StateTestFixtures.illust(1, type = "illust", width = 2000, height = 1000),
            StateTestFixtures.illust(2, type = "ugoira", width = 1000, height = 2000),
            StateTestFixtures.illust(3, xRestrict = 1, type = "illust", width = 2000, height = 1000),
            StateTestFixtures.illust(4, type = "illust", width = 1500, height = 500),
        )
        val fetch = RecordingSearchFetch { IllustPagedPage(page, null) }
        val state = listStateFor(fetch, word = "初音ミク")
        runTest {
            state.loadFirst()
            // 状态机过滤（复刻屏蔽设置）：R-18 已在 filteredIllusts 中剔除。
            assertEquals(listOf(1L, 2L, 4L), state.ids(), "屏蔽过滤应先剔除 xRestrict>0 的作品")
            // 展示阶段再叠加客户端过滤：仅保留横图。
            assertEquals(
                listOf(1L, 4L),
                applySearchClientFilters(state.filteredIllusts!!, ugoira = 0, ratio = 1).map { it.id },
                "展示阶段的横图过滤应作用在状态机过滤结果之上",
            )
            assertEquals(
                listOf(2L),
                applySearchClientFilters(state.filteredIllusts!!, ugoira = 1, ratio = 2).map { it.id },
                "展示阶段的动图+竖图组合应作用在状态机过滤结果之上",
            )
        }
    }

    // ---------------------------------------------------------------- 分页状态机

    @Test
    fun `首屏替换翻页追加去重且游标推进`() {
        runTest {
            // 常规：两页数据，第二页首条与第一页末条重复。
            val first = listOf(
                StateTestFixtures.illust(1),
                StateTestFixtures.illust(2),
                StateTestFixtures.illust(3),
            )
            val second = listOf(StateTestFixtures.illust(3), StateTestFixtures.illust(4))
            val fetch = RecordingSearchFetch { request ->
                when (request.nextUrl) {
                    null -> IllustPagedPage(first, "https://pixiv.net/search?page=2")
                    else -> IllustPagedPage(second, null)
                }
            }
            val state = listStateFor(fetch, word = "初音ミク", sort = "popular_desc")
            assertEquals(PagedLoadOutcome.Written, state.loadFirst(), "首屏加载应写入")
            assertEquals(listOf(1L, 2L, 3L), state.ids())
            assertEquals("https://pixiv.net/search?page=2", state.nextUrl, "首屏响应应透传下一页游标")
            assertEquals(PagedLoadOutcome.Written, state.loadMore(), "翻页加载应写入")
            assertEquals(listOf(1L, 2L, 3L, 4L), state.ids(), "翻页应追加并按下标去重")
            assertNull(state.nextUrl, "末页响应 nextUrl 为空时应停止继续翻页")
            assertEquals(
                listOf("初音ミク" to "popular_desc", "初音ミク" to "popular_desc"),
                fetch.requests.map { it.word to it.sort },
                "两页请求都应携带当前搜索词与排序",
            )
        }
    }

    @Test
    fun `空页响应推进游标且失败保留旧列表`() {
        runTest {
            // 边界：第二页为空列表但给出游标，随后失败页应保留已加载数据。
            val fetch = RecordingSearchFetch { request ->
                when (request.nextUrl) {
                    null -> IllustPagedPage(listOf(StateTestFixtures.illust(7)), "cursor-empty")
                    "cursor-empty" -> IllustPagedPage(emptyList(), "cursor-fail")
                    else -> error("模拟翻页失败")
                }
            }
            val state = listStateFor(fetch, word = "原神")
            state.loadFirst()
            assertEquals(PagedLoadOutcome.Written, state.loadMore(), "空页也应视为写入并推进游标")
            assertEquals(listOf(7L), state.ids(), "空页不应影响已加载列表")
            assertEquals("cursor-fail", state.nextUrl)
            assertEquals(PagedLoadOutcome.Failed, state.loadMore(), "请求抛错应返回 Failed")
            assertEquals(listOf(7L), state.ids(), "翻页失败应保留旧列表")
            assertTrue(state.loadMoreError != null, "翻页失败应暴露 loadMoreError")
            assertEquals(PagingFooterPhase.Retry, state.footerState.phase, "失败后页脚应显示重试")
        }
    }

    @Test
    fun `账号切换重置后清空旧账号结果并以空游标重拉`() {
        runTest {
            val accountAFetch = RecordingSearchFetch { IllustPagedPage(listOf(StateTestFixtures.illust(11)), "p2") }
            val state = listStateFor(accountAFetch, word = "ブルーアーカイブ")
            state.loadFirst()
            assertEquals(listOf(11L), state.ids())
            assertEquals("p2", state.nextUrl)

            // 复刻算子 :134-136：检测到切号后先 reset()，再由同一 LaunchedEffect 调 loadFirst()。
            state.reset()
            assertNull(state.rawIllusts, "reset 应清空原始列表")
            assertNull(state.filteredIllusts, "reset 应清空过滤列表")
            assertNull(state.nextUrl, "reset 应清空游标")
            assertNull(state.initialError, "reset 应清空首屏错误")
            assertNull(state.loadMoreError, "reset 应清空翻页错误")
            assertTrue(state.generation > 0, "reset 应推进 generation 以作废在途请求")
            assertEquals(PagingFooterPhase.Hidden, state.footerState.phase, "reset 后页脚应隐藏")

            val accountBFetch = RecordingSearchFetch { IllustPagedPage(listOf(StateTestFixtures.illust(22)), null) }
            val afterSwitch = listStateFor(accountBFetch, word = "ブルーアーカイブ")
            assertEquals(PagedLoadOutcome.Written, afterSwitch.loadFirst(), "切号后应重新加载首屏")
            assertEquals(listOf(22L), afterSwitch.ids(), "切号后应展示新账号结果")
            assertEquals(listOf<String?>(null), accountBFetch.requests.map { it.nextUrl }, "重拉必须从空游标开始")
            assertEquals("ブルーアーカイブ", accountBFetch.requests.single().word, "重拉应沿用同一搜索词")
            // 旧状态机保持清空（不会被新账号数据回填）。
            assertNull(state.filteredIllusts, "旧账号状态机应保持清空")
        }
    }

    @Test
    fun `筛选版本变化时本地重过滤且不发起新请求`() {
        runTest {
            var version = 0
            var hideR18 = false
            val page = listOf(
                StateTestFixtures.illust(1, xRestrict = 1),
                StateTestFixtures.illust(2),
                StateTestFixtures.illust(3),
            )
            val fetch = RecordingSearchFetch { IllustPagedPage(page, "p2") }
            val state = listStateFor(
                fetch = fetch,
                word = "初音ミク",
                filterVersionProvider = { version },
                hideR18 = { hideR18 },
            )
            state.loadFirst()
            assertEquals(listOf(1L, 2L, 3L), state.ids(), "未开启屏蔽时原始与过滤列表一致")
            assertEquals(1, fetch.callCount)

            // 复刻算子 :139：版本变化时 refilterIfChanged 返回 true 并提前 return，不再 loadFirst。
            hideR18 = true
            version = 1
            assertTrue(state.refilterIfChanged(), "筛选版本变化应触发本地重过滤")
            assertEquals(listOf(2L, 3L), state.ids(), "重过滤应剔除被屏蔽作品")
            assertEquals(1, fetch.callCount, "本地重过滤不应发起新请求")
            assertFalse(state.refilterIfChanged(), "版本未再变化时不应重复重过滤")
            assertEquals(1, fetch.callCount)
        }
    }

    // ---------------------------------------------------------------- 源码接线守护

    @Test
    fun `算子源码保留复刻所依赖的内联接线`() {
        val source = StateSourceReader.readUi("screens/SearchResultsState.kt")
        val requiredFragments = listOf(
            "val cleanQuery = query.replace(Regex(" to "清理检索词后缀的起点",
            """\s*\d+users入り""" to "清除既有 users入り 后缀的正则",
            "if (bookmarkThreshold > 0)" to "按收藏阈值决定是否追加后缀",
            "users入り\" else cleanQuery" to "阈值为 0 时回落到清理后的检索词",
            "val searchWord = remember(query, bookmarkThreshold) {" to "搜索词随查询词与阈值变化",
            "val accountSwitched = loadedForAccount !== accountUnloaded && loadedForAccount != user" to
                "账号身份跟踪（未加载哨兵参与比较）",
            "if (accountSwitched) listState.reset()" to "切号时先重置分页状态机",
            "if (listState.refilterIfChanged()) return@LaunchedEffect" to "筛选版本变化时跳过网络加载",
            "if (isLoggedIn == null) return@LaunchedEffect" to "登录态未判定完成时不发起加载",
            "IllustPagedPage(response.illusts, response.nextUrl)" to "分页响应透传游标",
            "banRepository.filterIllustsBySettings(raw, settingsRepository)" to "状态机过滤链复用屏蔽设置",
            "internal fun applySearchClientFilters(list: List<Illust>, ugoira: Int, ratio: Int): List<Illust> {" to
                "客户端二次过滤保持 internal 顶层函数（本测试直接调用）",
        )
        for ((fragment, reason) in requiredFragments) {
            assertTrue(fragment in source, "SearchResultsState.kt 应保留$reason：$fragment")
        }

        // 展示阶段的客户端过滤必须由页面在收集到的列表上调用（否则筛选按钮无效果）。
        val section = StateSourceReader.readUi("screens/SearchResultSection.kt")
        assertTrue(
            "applySearchClientFilters(currentIllusts, filterState.ugoiraFilter, filterState.ratioFilter)" in section,
            "SearchResultSection 应在展示列表上调用 applySearchClientFilters",
        )
    }
}
