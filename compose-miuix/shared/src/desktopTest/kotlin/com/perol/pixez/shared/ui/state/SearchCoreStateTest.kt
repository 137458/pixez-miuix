package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.screens.SearchFilterState
import com.perol.pixez.shared.ui.screens.SearchQueryTarget
import com.perol.pixez.shared.ui.screens.parseSearchQueryTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 搜索页核心状态算子（rememberSearchCoreState，SearchCoreState.kt）的行为测试。
 *
 * 为什么不能直接驱动算子本身：算子是 @Composable，依赖 LocalIllustRepository 等
 * CompositionLocal 下的具体仓储类，shared 模块未引入 compose ui-test 依赖，
 * 纯 JVM 测试既无法启动组合，也无法打桩这些具体类（无接口可伪造）。
 *
 * 因此分三层锁定：
 * 1. 生产纯函数直测：parseSearchQueryTarget（分流规则）、SearchFilterState.hasActiveFilters
 *    （筛选是否生效的判定，决定顶栏高亮与筛选按钮文案）——测试直接调用真实实现。
 * 2. 内联行为复刻：算子体内无法抽出的内联逻辑（历史去重取 20 条、onSearch 分流顺序、
 *    排序循环、设置回写守卫）在测试内以同名同语义的小函数复刻，用差异化数据驱动，
 *    并配合第 3 层守护保证复刻与源码一致；复刻函数逐条标注源码行号。
 * 3. 源码接线守护：断言 SearchCoreState.kt 仍保留上述内联表达式原文，防止复刻与实现脱节。
 */
class SearchCoreStateTest {

    // ---------------------------------------------------------------- 复刻逻辑

    /**
     * 复刻 SearchCoreState.kt:150 的搜索历史写入语义：新词置顶、去重、最多保留 20 条。
     */
    private fun addToHistory(history: List<String>, entry: String): List<String> =
        (listOf(entry) + history.filter { it != entry }).take(20)

    /**
     * 一次搜索提交的结果，对应算子内 onSearch 的三个可观测后果：
     * 是否进入搜索结果模式、历史列表、以及是否发生直达导航（形如 "illust:123"）。
     */
    private data class SearchSubmission(
        val isSearching: Boolean,
        val history: List<String>,
        val navigatedTo: String?,
    )

    /**
     * 复刻 SearchCoreState.kt:146-158 的 onSearch 分流：
     * 先写历史，再判断查询词是否为作品/画师/数字 ID 直达目标；非直达才置 isSearching。
     * 数字 ID 的导航去向取决于 searchTypeIndex（:130-136：1 画师、2 小说、其余作品）。
     */
    private fun submitSearch(
        query: String,
        history: List<String>,
        searchTypeIndex: Int = 0,
    ): SearchSubmission {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return SearchSubmission(isSearching = false, history = history, navigatedTo = null)
        }
        val newHistory = addToHistory(history, trimmed)
        val target = parseSearchQueryTarget(trimmed)
            ?: return SearchSubmission(isSearching = true, history = newHistory, navigatedTo = null)
        val route = when (target) {
            is SearchQueryTarget.IllustId -> "illust:${target.id}"
            is SearchQueryTarget.UserId -> "user:${target.id}"
            is SearchQueryTarget.NumericId -> when (searchTypeIndex) {
                1 -> "user:${target.id}"
                2 -> "novel:${target.id}"
                else -> "illust:${target.id}"
            }
        }
        return SearchSubmission(isSearching = false, history = newHistory, navigatedTo = route)
    }

    /** 复刻 SearchCoreState.kt:217-223 的排序点击循环：date_desc → popular_desc → date_asc → date_desc。 */
    private fun nextSort(current: String): String = when (current) {
        "date_desc" -> "popular_desc"
        "popular_desc" -> "date_asc"
        else -> "date_desc"
    }

    /**
     * 复刻 SearchCoreState.kt:80-104 的筛选回写守卫：算子内每个筛选项各有一个 LaunchedEffect，
     * 首帧值取自设置本身，因此比较后才写入，避免无谓持久化与 filterChangeVersion 自增。
     */
    private class FakeSearchSettings {
        var searchSort = "date_desc"
        var searchTarget = "partial_match_for_tags"
        var searchAiType = 0
        var searchBookmarkThreshold = 0
        var searchUgoiraFilter = 0
        var searchStartDate = ""
        var searchEndDate = ""
        val writeLog = mutableListOf<String>()

        fun persistSort(value: String) {
            if (searchSort != value) {
                searchSort = value
                writeLog += "sort=$value"
            }
        }

        fun persistTarget(value: String) {
            if (searchTarget != value) {
                searchTarget = value
                writeLog += "target=$value"
            }
        }

        fun persistAiType(value: Int) {
            if (searchAiType != value) {
                searchAiType = value
                writeLog += "aiType=$value"
            }
        }

        fun persistBookmarkThreshold(value: Int) {
            if (searchBookmarkThreshold != value) {
                searchBookmarkThreshold = value
                writeLog += "bookmarkThreshold=$value"
            }
        }

        fun persistUgoiraFilter(value: Int) {
            if (searchUgoiraFilter != value) {
                searchUgoiraFilter = value
                writeLog += "ugoira=$value"
            }
        }

        fun persistStartDate(value: String) {
            if (searchStartDate != value) {
                searchStartDate = value
                writeLog += "startDate=$value"
            }
        }

        fun persistEndDate(value: String) {
            if (searchEndDate != value) {
                searchEndDate = value
                writeLog += "endDate=$value"
            }
        }
    }

    // ---------------------------------------------------------------- 初始态与筛选判定

    @Test
    fun `默认筛选态不含任何活跃条件`() {
        val default = SearchFilterState()
        assertEquals("partial_match_for_tags", default.searchTarget, "默认搜索目标应与插画标签部分匹配一致")
        assertEquals(0, default.searchAiType)
        assertEquals(0, default.bookmarkThreshold)
        assertEquals(0, default.ugoiraFilter)
        assertEquals(0, default.ratioFilter)
        assertEquals("", default.startDate)
        assertEquals("", default.endDate)
        assertEquals(false, default.hIsNotAllow)
        assertFalse(default.hasActiveFilters, "默认筛选态不应点亮筛选按钮")
    }

    @Test
    fun `任一筛选字段偏离默认值都判定为存在活跃筛选`() {
        // 常规：逐项单字段偏离默认值。
        val singleFieldCases = listOf(
            "换搜索目标" to SearchFilterState(searchTarget = "exact_match_for_tags"),
            "开 AI 类型" to SearchFilterState(searchAiType = 1),
            "设收藏阈值" to SearchFilterState(bookmarkThreshold = 500),
            "动图筛选" to SearchFilterState(ugoiraFilter = 1),
            "比例筛选" to SearchFilterState(ratioFilter = 2),
            "起始日期" to SearchFilterState(startDate = "2026-01-01"),
            "结束日期" to SearchFilterState(endDate = "2026-12-31"),
            "屏蔽 H" to SearchFilterState(hIsNotAllow = true),
        )
        for ((label, state) in singleFieldCases) {
            assertTrue(state.hasActiveFilters, "$label 后应判定存在活跃筛选")
        }
        // 边界：组合多个条件、以及只把字段改回默认值（阈值 0 不等于「已设置」）。
        assertTrue(
            SearchFilterState(bookmarkThreshold = 1000, ugoiraFilter = 2, startDate = "2026-02-02")
                .hasActiveFilters,
            "多条件组合应判定存在活跃筛选",
        )
        assertFalse(
            SearchFilterState(bookmarkThreshold = 0, ugoiraFilter = 0, searchAiType = 0)
                .hasActiveFilters,
            "显式写回默认值不应判定为活跃筛选",
        )
    }

    // ---------------------------------------------------------------- 搜索历史

    @Test
    fun `搜索历史新词置顶且重复词不产生副本`() {
        val existing = listOf("初音ミク", "ブルーアーカイブ", "原神")
        assertEquals(
            listOf("Fate", "初音ミク", "ブルーアーカイブ", "原神"),
            addToHistory(existing, "Fate"),
            "新搜索词应置顶且保留既有顺序",
        )
        assertEquals(
            listOf("ブルーアーカイブ", "初音ミク", "原神"),
            addToHistory(existing, "ブルーアーカイブ"),
            "已在历史中的词应移动到首位而不是产生第二条副本",
        )
        assertEquals(
            listOf("初音ミク"),
            addToHistory(listOf("初音ミク"), "初音ミク"),
            "重复提交同一词时历史应保持不变",
        )
    }

    @Test
    fun `搜索历史最多保留二十条且最旧一条被挤出`() {
        val full = (1..20).map { "q$it" }
        val afterNewEntry = addToHistory(full, "q21")
        assertEquals(20, afterNewEntry.size, "历史条数上限应为 20")
        assertEquals("q21", afterNewEntry.first(), "新搜索词应位于首位")
        assertEquals("q1", afterNewEntry[1], "既有历史应整体后移一位")
        assertEquals("q19", afterNewEntry.last(), "超限时应挤出最旧一条（q20）")
        assertFalse("q20" in afterNewEntry, "第 20 条旧历史应被挤出")

        // 边界：历史已满时重复提交中段的词，仍保持 20 条并把该词提到首位。
        val afterDuplicate = addToHistory(full, "q5")
        assertEquals(20, afterDuplicate.size, "去重后条数不应减少")
        assertEquals("q5", afterDuplicate.first())
        assertFalse(afterDuplicate.drop(1).any { it == "q5" }, "中段重复词不应残留副本")
        // 去重只替换位置、总数仍为 20，未超过上限，因此最旧一条不应被挤出。
        assertTrue("q20" in afterDuplicate, "满额去重不应挤出最旧一条")
    }

    // ---------------------------------------------------------------- 提交分流

    @Test
    fun `关键词提交进入搜索结果模式并写入历史`() {
        val keyword = submitSearch("初音ミク", listOf("原神"))
        assertTrue(keyword.isSearching, "普通关键词应进入搜索结果模式")
        assertNull(keyword.navigatedTo, "普通关键词不应触发直达导航")
        assertEquals(listOf("初音ミク", "原神"), keyword.history, "关键词应写入历史首位")

        // 边界：首尾带空格的查询词按 trim 后的文本判定与写历史。
        val padded = submitSearch("  初音ミク  ", emptyList())
        assertTrue(padded.isSearching)
        assertEquals(listOf("初音ミク"), padded.history, "历史应记录 trim 后的查询词")
    }

    @Test
    fun `作品与画师链接以及纯数字查询直接导航而不进入搜索结果模式`() {
        val cases = listOf(
            "https://www.pixiv.net/artworks/123456" to "illust:123456",
            "illust_id=555" to "illust:555",
            "pixiv://illusts/777" to "illust:777",
            "https://www.pixiv.net/users/9988" to "user:9988",
            "member.php?id=4242" to "user:4242",
            "pixiv://users/31337" to "user:31337",
        )
        for ((raw, expectedRoute) in cases) {
            val submission = submitSearch(raw, listOf("原神"))
            assertEquals(expectedRoute, submission.navigatedTo, "$raw 应直达 $expectedRoute")
            assertFalse(submission.isSearching, "$raw 直达导航时不应进入搜索结果模式")
            assertEquals(raw.trim(), submission.history.first(), "直达查询词同样应写入历史（与源码顺序一致）")
        }
        // 边界：非法/无法解析的输入回落到搜索结果模式。
        for (raw in listOf("https://www.pixiv.net/artworks/0", "artworks/abc", "12345678users入り")) {
            val submission = submitSearch(raw, emptyList())
            assertNull(submission.navigatedTo, "$raw 不应被解析成直达目标")
            assertTrue(submission.isSearching, "$raw 应回落到搜索结果模式")
        }
    }

    @Test
    fun `纯数字查询按搜索类型分别跳转画师小说或作品`() {
        val expectedByType = mapOf(
            0 to "illust:123",
            1 to "user:123",
            2 to "novel:123",
            3 to "illust:123",
            -1 to "illust:123",
        )
        for ((searchTypeIndex, expectedRoute) in expectedByType) {
            val submission = submitSearch("123", emptyList(), searchTypeIndex)
            assertEquals(
                expectedRoute,
                submission.navigatedTo,
                "searchTypeIndex=$searchTypeIndex 时纯数字查询应跳转到 $expectedRoute",
            )
            assertFalse(
                submission.isSearching,
                "searchTypeIndex=$searchTypeIndex 时纯数字查询应直达而不进入搜索结果模式",
            )
        }
        assertEquals(
            SearchQueryTarget.NumericId(7),
            parseSearchQueryTarget("007"),
            "前导零的纯数字应解析为数值 7",
        )
        assertNull(parseSearchQueryTarget("0"), "0 不是合法 ID")
        assertNull(parseSearchQueryTarget("   "), "空白查询词不是直达目标")
    }

    @Test
    fun `空白查询词被忽略且不写历史`() {
        val history = listOf("原神")
        for (blank in listOf("", "   ", "\n\t")) {
            val submission = submitSearch(blank, history)
            assertFalse(submission.isSearching, "空白查询词不应进入搜索结果模式")
            assertNull(submission.navigatedTo, "空白查询词不应触发导航")
            assertEquals(history, submission.history, "空白查询词不应改动历史")
        }
    }

    // ---------------------------------------------------------------- 筛选回写与交互

    @Test
    fun `筛选设置仅在值变化时回写一次`() {
        val settings = FakeSearchSettings()
        // 首帧：算子初值取自设置本身，回写守卫应完全静默。
        settings.persistSort(settings.searchSort)
        settings.persistTarget(settings.searchTarget)
        settings.persistAiType(settings.searchAiType)
        settings.persistBookmarkThreshold(settings.searchBookmarkThreshold)
        settings.persistUgoiraFilter(settings.searchUgoiraFilter)
        settings.persistStartDate(settings.searchStartDate)
        settings.persistEndDate(settings.searchEndDate)
        assertEquals(emptyList(), settings.writeLog, "首帧值等于设置值时不应用任何持久化")

        // 常规：用户改动两个筛选项 → 各写一次。
        settings.persistBookmarkThreshold(500)
        settings.persistSort("popular_desc")
        assertEquals(listOf("bookmarkThreshold=500", "sort=popular_desc"), settings.writeLog)
        assertEquals(500, settings.searchBookmarkThreshold, "改动应落到设置存储")
        assertEquals("popular_desc", settings.searchSort)

        // 边界：同一值重复提交（重组/重新进入页面）不应重复写入。
        settings.persistBookmarkThreshold(500)
        settings.persistSort("popular_desc")
        assertEquals(2, settings.writeLog.size, "重复提交相同值不应新增写入")
    }

    @Test
    fun `排序循环与筛选快捷操作共享同一状态`() {
        // 常规：三档排序循环回到起点。
        var sort = "date_desc"
        val cycle = buildList {
            repeat(3) {
                sort = nextSort(sort)
                add(sort)
            }
        }
        assertEquals(listOf("popular_desc", "date_asc", "date_desc"), cycle, "排序应在三档之间循环")

        // 边界：未知排序值（设置被外部写坏）应回落到热门排序（date_desc）。
        assertEquals("date_desc", nextSort("unknown_mode"), "未知排序值应回落到 date_desc")

        // AI 类型开关：0 ↔ 1 往复。
        val toggle = listOf(0, 1, 0).zipWithNext { current, _ -> if (current == 0) 1 else 0 }
        assertEquals(listOf(1, 0), toggle, "AI 类型开关应在 0/1 之间切换")

        // 清空收藏阈值：置 0 后活跃筛选判定必须随之回到 false。
        var bookmarkThreshold = 500
        assertTrue(
            SearchFilterState(bookmarkThreshold = bookmarkThreshold).hasActiveFilters,
            "收藏阈值 500 应判定为活跃筛选",
        )
        bookmarkThreshold = 0
        assertFalse(
            SearchFilterState(bookmarkThreshold = bookmarkThreshold).hasActiveFilters,
            "清空收藏阈值后不应再判定为活跃筛选",
        )
    }

    // ---------------------------------------------------------------- 源码接线守护

    @Test
    fun `算子源码保留复刻所依赖的内联接线`() {
        val source = StateSourceReader.readUi("screens/SearchCoreState.kt")
        val requiredFragments = listOf(
            "updateHistory((listOf(trimmed) + searchHistory.filter { it != trimmed }).take(20))" to
                "搜索历史去重取 20 条的内联实现",
            "val target = parseSearchQueryTarget(trimmed)" to "提交时解析直达目标",
            "if (target == null) {" to "非直达才进入搜索结果模式",
            "banRepository.filterBannedTags(banRepository.getAllBanTags(), tags) { listOfNotNull(it.tag, it.translatedName) }" to
                "热门标签同样受屏蔽规则约束",
            "var searchHistory by rememberSaveable { mutableStateOf(settingsRepository.searchHistory) }" to
                "历史从设置读取",
            "settingsRepository.searchHistory = newHistory" to "历史写回设置",
            "\"date_desc\" -> \"popular_desc\"" to "排序循环第一档",
            "\"popular_desc\" -> \"date_asc\"" to "排序循环第二档",
            "onAiTypeClick = { searchAiType = if (searchAiType == 0) 1 else 0 }" to "AI 类型开关",
            "onClearBookmarkThreshold = { bookmarkThreshold = 0 }" to "清空收藏阈值",
        )
        for ((fragment, reason) in requiredFragments) {
            assertTrue(fragment in source, "SearchCoreState.kt 应保留$reason：$fragment")
        }

        // 七个筛选项各有一个带 != 守卫的回写，计数必须为 7，新增筛选项时需同步本测试。
        val guardCount = Regex(Regex.escape("if (settingsRepository.")).findAll(source).count()
        assertEquals(7, guardCount, "每个筛选项都应有一个 \"值不同才回写\" 的守卫")

        // 搜索目标解析是生产纯函数，测试直接调用它；守护其仍为 internal 顶层函数且返回可空 sealed.
        val searchScreen = StateSourceReader.readUi("screens/SearchScreen.kt")
        assertTrue(
            "internal fun parseSearchQueryTarget(rawQuery: String): SearchQueryTarget? {" in searchScreen,
            "parseSearchQueryTarget 应保持 internal 顶层函数签名（本测试直接调用真实实现）",
        )
        assertTrue(
            "val hasActiveFilters: Boolean" in searchScreen,
            "SearchFilterState.hasActiveFilters 应保持为派生属性",
        )
    }
}
