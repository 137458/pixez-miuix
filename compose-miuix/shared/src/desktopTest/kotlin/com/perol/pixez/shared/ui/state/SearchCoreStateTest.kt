package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.ui.screens.SEARCH_HISTORY_LIMIT
import com.perol.pixez.shared.ui.screens.SearchFilterState
import com.perol.pixez.shared.ui.screens.SearchQueryTarget
import com.perol.pixez.shared.ui.screens.mergeSearchHistory
import com.perol.pixez.shared.ui.screens.normalizeSearchWord
import com.perol.pixez.shared.ui.screens.parseSearchQueryTarget
import com.perol.pixez.shared.ui.screens.shouldPersistFilter
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
 *    （筛选是否生效的判定，决定顶栏高亮与筛选按钮文案）、normalizeSearchWord 与
 *    mergeSearchHistory（提交词归一化、历史置顶去重取 20 条）、shouldPersistFilter
 *    （筛选回写守卫）——测试直接调用算子使用的真实实现。
 * 2. 交互顺序复刻：算子体内仍是内联实现的「先写历史再分流」顺序、数字 ID 跳转去向与
 *    排序/AI 开关循环，在测试内以同名同语义的小函数复刻，用差异化数据驱动，
 *    并由第 3 层守护保证复刻与源码一致；复刻函数逐条标注源码行号。
 * 3. 源码接线守护：断言 SearchCoreState.kt 仍调用上述生产纯函数、且每个筛选项的回写都经过
 *    shouldPersistFilter，防止算子绕过已抽取的实现。
 */
class SearchCoreStateTest {

    // ---------------------------------------------------------------- 复刻逻辑

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
     * 复刻 SearchCoreState.kt:155-168 的 onSearch 分流：
     * 先用 normalizeSearchWord 归一化（空白词忽略），再用 mergeSearchHistory 写历史，
     * 最后判断查询词是否为作品/画师/数字 ID 直达目标；非直达才置 isSearching。
     * 数字 ID 的导航去向取决于 searchTypeIndex（1 画师、2 小说、其余作品）。
     * 归一化与历史合并均直接调用生产纯函数，复刻的只是「写入与分流的先后顺序」。
     */
    private fun submitSearch(
        query: String,
        history: List<String>,
        searchTypeIndex: Int = 0,
    ): SearchSubmission {
        val word = normalizeSearchWord(query)
            ?: return SearchSubmission(isSearching = false, history = history, navigatedTo = null)
        val newHistory = mergeSearchHistory(history, word)
        val target = parseSearchQueryTarget(word)
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

    /** 复刻 SearchCoreState.kt:227-233 的排序点击循环：date_desc → popular_desc → date_asc → date_desc。 */
    private fun nextSort(current: String): String = when (current) {
        "date_desc" -> "popular_desc"
        "popular_desc" -> "date_asc"
        else -> "date_desc"
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
            mergeSearchHistory(existing, "Fate"),
            "新搜索词应置顶且保留既有顺序",
        )
        assertEquals(
            listOf("ブルーアーカイブ", "初音ミク", "原神"),
            mergeSearchHistory(existing, "ブルーアーカイブ"),
            "已在历史中的词应移动到首位而不是产生第二条副本",
        )
        assertEquals(
            listOf("初音ミク"),
            mergeSearchHistory(listOf("初音ミク"), "初音ミク"),
            "重复提交同一词时历史应保持不变",
        )
    }

    @Test
    fun `搜索历史最多保留二十条且最旧一条被挤出`() {
        val full = (1..20).map { "q$it" }
        val afterNewEntry = mergeSearchHistory(full, "q21")
        assertEquals(20, afterNewEntry.size, "历史条数上限应为 20")
        assertEquals("q21", afterNewEntry.first(), "新搜索词应位于首位")
        assertEquals("q1", afterNewEntry[1], "既有历史应整体后移一位")
        assertEquals("q19", afterNewEntry.last(), "超限时应挤出最旧一条（q20）")
        assertFalse("q20" in afterNewEntry, "第 20 条旧历史应被挤出")

        // 边界：历史已满时重复提交中段的词，仍保持 20 条并把该词提到首位。
        val afterDuplicate = mergeSearchHistory(full, "q5")
        assertEquals(20, afterDuplicate.size, "去重后条数不应减少")
        assertEquals("q5", afterDuplicate.first())
        assertFalse(afterDuplicate.drop(1).any { it == "q5" }, "中段重复词不应残留副本")
        // 去重只替换位置、总数仍为 20，未超过上限，因此最旧一条不应被挤出。
        assertTrue("q20" in afterDuplicate, "满额去重不应挤出最旧一条")
    }

    @Test
    fun `搜索历史恰好达到上限时不截断且显式上限参数生效`() {
        // 边界：合并后条数恰好等于上限 20，不应丢任何条目。
        val nineteen = (1..19).map { "q$it" }
        assertEquals(20, SEARCH_HISTORY_LIMIT, "历史条数上限常量应为 20")
        assertEquals(
            listOf("q0") + nineteen,
            mergeSearchHistory(nineteen, "q0"),
            "合并后恰好 20 条时不应截断",
        )
        // 常规：显式 limit 参数覆盖默认上限，按传入值截断。
        assertEquals(
            listOf("e", "a", "b"),
            mergeSearchHistory(listOf("a", "b", "c", "d"), "e", limit = 3),
            "显式 limit 应覆盖默认上限并保留置顶后的前 3 条",
        )
        // 边界：limit 大于可用条数时不补齐空位。
        assertEquals(listOf("e", "a"), mergeSearchHistory(listOf("a"), "e", limit = 10))
        // 边界：limit = 1 只保留新词；空历史时新词成为唯一条目。
        assertEquals(listOf("e"), mergeSearchHistory(listOf("a", "b"), "e", limit = 1))
        assertEquals(listOf("初音ミク"), mergeSearchHistory(emptyList(), "初音ミク"))
    }

    @Test
    fun `搜索词归一化去除首尾空白并忽略纯空白词`() {
        // 常规：仅去掉首尾空白，词内空格保留。
        assertEquals("初音ミク", normalizeSearchWord("初音ミク"))
        assertEquals("初音ミク", normalizeSearchWord("  初音ミク  "))
        assertEquals("初音 ミク", normalizeSearchWord("\t初音 ミク\n"))
        // 边界：空串与各类纯空白词返回 null，表示本次提交被算子忽略。
        for (blank in listOf("", " ", "   ", "\n\t")) {
            assertNull(normalizeSearchWord(blank), "纯空白词应返回 null：长度 ${blank.length}")
        }
        // 边界：与合并函数串联时空白词不会进入历史（对应算子 onSearch 的分支顺序）。
        val history = listOf("原神")
        val word = normalizeSearchWord("   ")
        assertEquals(
            history,
            word?.let { mergeSearchHistory(history, it) } ?: history,
            "空白词不应改动历史",
        )
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
    fun `筛选回写守卫仅在值与已持久化值不同时成立`() {
        // 常规：整型筛选项（收藏阈值、UGira 筛选、AI 类型）。
        assertFalse(shouldPersistFilter(0, 0), "收藏阈值未变不应回写")
        assertTrue(shouldPersistFilter(0, 500), "收藏阈值 0 → 500 应回写")
        assertTrue(shouldPersistFilter(500, 0), "收藏阈值 500 → 0 应回写（清空阈值）")
        assertFalse(shouldPersistFilter(2, 2), "Ugoira 筛选未变不应回写")
        assertTrue(shouldPersistFilter(2, 0), "Ugoira 筛选 2 → 0 应回写")
        // 边界：字符串筛选项（排序、搜索目标、起止日期）。
        assertFalse(shouldPersistFilter("date_desc", "date_desc"), "排序未变不应回写")
        assertTrue(shouldPersistFilter("date_desc", "popular_desc"), "排序切换应回写")
        assertFalse(shouldPersistFilter("", ""), "日期为空且未变不应回写")
        assertTrue(shouldPersistFilter("", "2026-01-01"), "设置起始日期应回写")
        assertTrue(shouldPersistFilter("2026-01-01", ""), "清空起始日期应回写")
        // 边界：布尔筛选项（屏蔽 H）走同一泛型守卫。
        assertFalse(shouldPersistFilter(false, false), "屏蔽 H 未变不应回写")
        assertTrue(shouldPersistFilter(false, true), "开启屏蔽 H 应回写")
        assertTrue(shouldPersistFilter(true, false), "关闭屏蔽 H 应回写")
    }

    @Test
    fun `筛选设置仅在值变化时回写一次`() {
        // 设置存储的当前值，与算子首帧读取的初值同源；回写守卫来自生产纯函数 shouldPersistFilter。
        val stored = mutableMapOf<String, Any>(
            "sort" to "date_desc",
            "target" to "partial_match_for_tags",
            "aiType" to 0,
            "bookmarkThreshold" to 0,
            "ugoira" to 0,
            "startDate" to "",
            "endDate" to "",
        )
        val writes = mutableListOf<String>()

        fun persist(key: String, value: Any) {
            if (!shouldPersistFilter(stored[key], value)) return
            stored[key] = value
            writes += "$key=$value"
        }

        // 首帧：算子初值取自设置本身，回写守卫应完全静默。
        for ((key, value) in stored.toMap()) persist(key, value)
        assertEquals(emptyList(), writes, "首帧值等于设置值时不应用任何持久化")

        // 常规：用户改动两个筛选项 → 各写一次。
        persist("bookmarkThreshold", 500)
        persist("sort", "popular_desc")
        assertEquals(listOf("bookmarkThreshold=500", "sort=popular_desc"), writes)
        assertEquals(500, stored["bookmarkThreshold"], "改动应落到设置存储")
        assertEquals("popular_desc", stored["sort"])

        // 边界：同一值重复提交（重组/重新进入页面）不应重复写入。
        persist("bookmarkThreshold", 500)
        persist("sort", "popular_desc")
        assertEquals(2, writes.size, "重复提交相同值不应新增写入")
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
    fun `算子源码仍接入生产纯函数而非内联实现`() {
        val source = StateSourceReader.readUi("screens/SearchCoreState.kt")
        val requiredFragments = listOf(
            "val word = normalizeSearchWord(query)" to "提交前用生产纯函数归一化查询词",
            "updateHistory(mergeSearchHistory(searchHistory, word))" to
                "历史置顶去重取 20 条改用生产纯函数 mergeSearchHistory",
            "val target = parseSearchQueryTarget(word)" to "提交时解析直达目标",
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

        // 七个筛选项各有一个经生产纯函数守卫的回写，计数必须为 7，新增筛选项时需同步本测试。
        val guardCount = Regex(Regex.escape("if (shouldPersistFilter(settingsRepository.")).findAll(source).count()
        assertEquals(7, guardCount, "每个筛选项都应经过 shouldPersistFilter 的值变化守卫")
        // 旧的内联 != 守卫写法不应再出现，否则说明有筛选项绕过了生产纯函数。
        val inlineGuardCount = Regex(Regex.escape("if (settingsRepository.")).findAll(source).count()
        assertEquals(0, inlineGuardCount, "筛选项回写守卫应统一走 shouldPersistFilter")

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
