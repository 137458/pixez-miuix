package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import com.perol.pixez.shared.data.model.isR18
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 高-3 过滤链状态断言：filterChangeVersion 变化 → 对原始列表本地重过滤生效（零网络），原始列表保留。
 */
class FilterableIllustListStateTest {

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

    /** 可变测试开关，模拟 ShieldScreen 的「隐藏 R-18」开关与 filterChangeVersion 信号。 */
    private class FilterSettings {
        var hideR18 = false

        fun filter(raw: List<Illust>): List<Illust> =
            if (hideR18) raw.filterNot { it.isR18() } else raw
    }

    @Test
    fun filterVersionChangeRefiltersRawListLocally() = runTest {
        val settings = FilterSettings()
        val state = FilterableIllustListState(
            filter = { raw -> settings.filter(raw) },
            initialFilterVersion = 0,
        )
        val safe = illust(id = 101L)
        val r18 = illust(id = 202L, xRestrict = 1)
        state.replace(raw = listOf(safe, r18), filterVersion = 0)
        assertEquals(listOf(101L, 202L), state.filteredIllusts?.map { it.id })

        // 模拟过滤条件变化：filterChangeVersion 0 → 1，须对原始列表本地重过滤
        settings.hideR18 = true
        val refiltered = state.refilterIfChanged(filterVersion = 1)

        assertTrue(refiltered)
        assertEquals(listOf(101L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(101L, 202L), state.rawIllusts?.map { it.id })
    }

    @Test
    fun refilterSkippedWhenVersionUnchanged() = runTest {
        val settings = FilterSettings()
        val state = FilterableIllustListState(
            filter = { raw -> settings.filter(raw) },
            initialFilterVersion = 5,
        )
        state.replace(raw = listOf(illust(id = 1L), illust(id = 2L, xRestrict = 1)), filterVersion = 5)

        settings.hideR18 = true
        val refiltered = state.refilterIfChanged(filterVersion = 5)

        assertFalse(refiltered)
        assertEquals(listOf(1L, 2L), state.filteredIllusts?.map { it.id })
    }

    @Test
    fun appendMergesRawDistinctAndAppliesCurrentFilterVersion() = runTest {
        val settings = FilterSettings()
        val state = FilterableIllustListState(
            filter = { raw -> settings.filter(raw) },
            initialFilterVersion = 0,
        )
        state.replace(raw = listOf(illust(id = 1L)), filterVersion = 0)

        // 触底翻页：原始列表去重合并，过滤按当前版本执行
        settings.hideR18 = false
        state.append(rawPage = listOf(illust(id = 1L), illust(id = 9L, xRestrict = 1)), filterVersion = 0)
        assertEquals(listOf(1L, 9L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(1L, 9L), state.rawIllusts?.map { it.id })

        // 过滤开关变化后的下一页：合并去重且整体按当前版本重过滤
        settings.hideR18 = true
        state.append(rawPage = listOf(illust(id = 2L)), filterVersion = 1)
        assertEquals(listOf(1L, 2L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(1L, 9L, 2L), state.rawIllusts?.map { it.id })
    }

    @Test
    fun refilterWithoutRawDataReturnsFalse() = runTest {
        val settings = FilterSettings()
        val state = FilterableIllustListState(
            filter = { raw -> settings.filter(raw) },
            initialFilterVersion = 0,
        )

        val refiltered = state.refilterIfChanged(filterVersion = 1)

        assertFalse(refiltered)
        assertNull(state.filteredIllusts)
        assertNull(state.rawIllusts)
    }

    @Test
    fun restoreRawForcesRefilterEvenWhenVersionUnchanged() = runTest {
        val settings = FilterSettings()
        val state = FilterableIllustListState(
            filter = { raw -> settings.filter(raw) },
            initialFilterVersion = 0,
        )
        // 跨页期间屏蔽设置已变化（长按屏蔽/ShieldScreen），回查得到的原始列表包含被屏项；
        // 过滤版本号在恢复侧未变（仍是 0），恢复后下一轮检查也必须按当前设置重过滤。
        settings.hideR18 = true
        state.restoreRaw(listOf(illust(id = 7L), illust(id = 8L, xRestrict = 1)))

        val refiltered = state.refilterIfChanged(filterVersion = 0)

        assertTrue(refiltered, "恢复后即使版本号未变也必须重过滤一次（过滤版本可能在跨页期间已变化）")
        assertEquals(listOf(7L), state.filteredIllusts?.map { it.id }, "跨页后返回列表，跨页期间屏蔽的项应消失")
        assertEquals(listOf(7L, 8L), state.rawIllusts?.map { it.id })

        // 重过滤完成后不再重复执行
        assertFalse(state.refilterIfChanged(filterVersion = 0))
    }

    @Test
    fun restoreSnapshotKeepsRawForLaterRefilter() = runTest {
        val settings = FilterSettings()
        val state = FilterableIllustListState(
            filter = { raw -> settings.filter(raw) },
            initialFilterVersion = 0,
        )
        val snapshot = listOf(illust(id = 7L), illust(id = 8L, xRestrict = 1))
        state.restoreRaw(snapshot)
        assertEquals(listOf(7L, 8L), state.rawIllusts?.map { it.id })
        assertEquals(listOf(7L, 8L), state.filteredIllusts?.map { it.id })

        settings.hideR18 = true
        state.refilterIfChanged(filterVersion = 1)

        assertEquals(listOf(7L), state.filteredIllusts?.map { it.id })
        assertEquals(listOf(7L, 8L), state.rawIllusts?.map { it.id })
    }
}
