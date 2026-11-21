package com.perol.pixez.shared.ui.state

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 高-5 排行榜跨页恢复缓存断言：多 key 快照与写入序 LRU 淘汰语义
 * 自迁移前的仓库 activeRankingPages（8 key 上限）原样保持。
 */
class RankingFeedRestoreCacheTest {

    private fun illust(id: Long): Illust = Illust(
        id = id,
        title = "Illust $id",
        type = "illust",
        imageUrls = ImageUrls(squareMedium = "", medium = "", large = ""),
        caption = "",
        restrict = 0,
        user = IllustUser(
            id = 1L,
            name = "Artist",
            account = "artist",
            profileImageUrls = IllustProfileImageUrls(medium = ""),
        ),
        tags = emptyList(),
        tools = emptyList(),
        createDate = "2026-10-11T00:00:00+09:00",
        pageCount = 1,
        width = 1000,
        height = 1000,
        sanityLevel = 2,
        xRestrict = 0,
        metaSinglePage = MetaSinglePage(originalImageUrl = ""),
        metaPages = emptyList(),
        totalView = 0,
        totalBookmarks = 0,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        totalComments = 0,
        illustAIType = 0,
        illustBookStyle = 0,
    )

    @Test
    fun untrackedKeyReturnsNull() {
        val cache = RankingFeedRestoreCache()

        assertNull(cache.get("day_"))
    }

    @Test
    fun updateRecordsRawIllustIdsAndNextUrl() {
        val cache = RankingFeedRestoreCache()

        cache.update("day_", listOf(illust(301L), illust(302L)), "rankingNext")

        val cached = cache.get("day_")
        assertEquals(listOf(301L, 302L), cached?.illustIds, "快照必须存原始列表的 ID，而非过滤后列表")
        assertEquals("rankingNext", cached?.nextUrl)
        // 未写入的 key 读回为 null
        assertNull(cache.get("week_"))
    }

    @Test
    fun sameKeyOverwritesAndAllowsNullNextUrl() {
        val cache = RankingFeedRestoreCache()
        cache.update("day_", listOf(illust(301L), illust(302L)), "rankingNext")

        // 边界：同 key 二次写入覆盖，且下一页 URL 允许为 null
        cache.update("day_", listOf(illust(303L)), null)

        val overwritten = cache.get("day_")
        assertEquals(listOf(303L), overwritten?.illustIds)
        assertNull(overwritten?.nextUrl)
    }

    @Test
    fun lruEvictsOldestKeyBeyondCapacity() {
        val cache = RankingFeedRestoreCache()

        // 常规：写入 9 个不同 key，超过容量上限（8）后最久未写的 key_0 被淘汰
        repeat(9) { index ->
            cache.update("key_$index", listOf(illust(index.toLong() + 1)), null)
        }
        assertNull(cache.get("key_0"), "超出容量后最久未写 key 应被淘汰")
        for (index in 1..8) {
            assertEquals(1, cache.get("key_$index")?.illustIds?.size, "key_$index 应仍在缓存")
        }
    }

    @Test
    fun rewritePromotesKeyToLatest() {
        val cache = RankingFeedRestoreCache()
        repeat(9) { index ->
            cache.update("key_$index", listOf(illust(index.toLong() + 1)), null)
        }

        // 边界：同 key 重写视为最近使用，被提升到最新，不再是最早淘汰对象
        cache.update("key_1", listOf(illust(999L)), "refreshed")
        cache.update("key_9", listOf(illust(1000L)), null)

        val promoted = cache.get("key_1")
        assertEquals(listOf(999L), promoted?.illustIds, "重写后的 key_1 应保留")
        assertEquals("refreshed", promoted?.nextUrl)
        assertNull(cache.get("key_2"), "key_1 被提升后，key_2 成为最久未写，应被新写入淘汰")
        assertEquals(1, cache.get("key_3")?.illustIds?.size, "key_3 应仍在缓存")
        assertEquals(1, cache.get("key_9")?.illustIds?.size, "最近写入的 key_9 应仍在缓存")
    }

    @Test
    fun snapshotExportsAllPagesInInsertionOrder() {
        val cache = RankingFeedRestoreCache()
        cache.update("day_", listOf(illust(1L)), "dayNext")
        cache.update("week_", listOf(illust(2L)), null)
        cache.update("day_", listOf(illust(3L)), "refreshed")

        val exported = cache.snapshot()

        assertEquals(setOf("day_", "week_"), exported.keys)
        assertEquals(listOf(3L), exported["day_"]?.illustIds)
    }

    @Test
    fun initialPagesAreRestoredWithLruLimit() {
        val initial = buildMap {
            for (index in 0 until 9) {
                put("key_$index", IllustFeedSnapshot(illustIds = listOf(index.toLong()), nextUrl = null))
            }
        }

        val cache = RankingFeedRestoreCache(initial)

        assertNull(cache.get("key_0"), "恢复注入超过容量时同样按 LRU 淘汰最旧 key")
        assertEquals(listOf(8L), cache.get("key_8")?.illustIds)
    }
}
