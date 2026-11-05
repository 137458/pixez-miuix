package com.perol.pixez.shared.data.cache

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 作品内存缓存模块（架构审查 DN3）：容量/淘汰/清理规则由模块独占，读写方只依赖本模块。 */
class IllustMemoryCacheTest {

    private fun illust(id: Long) = Illust(
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
        createDate = "2026-01-01T00:00:00+09:00",
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
    fun `默认容量为 500 且按 ID 读写`() = runTest {
        assertEquals(500, IllustMemoryCache.DEFAULT_CAPACITY)
        val cache = IllustMemoryCache()

        assertNull(cache.get(1L))
        cache.put(illust(1L))
        assertEquals(1L, cache.get(1L)?.id)
    }

    @Test
    fun `批量写入后可逐条读取`() = runTest {
        val cache = IllustMemoryCache()

        cache.putAll(listOf(illust(10L), illust(11L), illust(12L)))

        assertEquals(listOf(10L, 11L, 12L), listOf(10L, 11L, 12L).map { cache.get(it)?.id })
    }

    @Test
    fun `超出容量时淘汰最久未访问项`() = runTest {
        val cache = IllustMemoryCache(capacity = 2)
        cache.put(illust(1L))
        cache.put(illust(2L))
        cache.get(1L) // 访问 1，使其成为最近使用
        cache.put(illust(3L))

        assertEquals(1L, cache.get(1L)?.id, "刚访问过的项不应被淘汰")
        assertNull(cache.get(2L), "最久未访问项应被淘汰")
        assertEquals(3L, cache.get(3L)?.id)
    }

    @Test
    fun `clear 清空全部条目`() = runTest {
        val cache = IllustMemoryCache()
        cache.putAll(listOf(illust(1L), illust(2L)))

        cache.clear()

        assertNull(cache.get(1L))
        assertNull(cache.get(2L))
    }
}
