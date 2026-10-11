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
 * 高-5 跨页恢复快照断言：恢复缓存只存原始数据 ID 列表与下一页 URL（不存过滤后派生态），
 * 写入序与覆盖语义与迁移前的仓库 ActiveFeed 保持一致。
 */
class IllustFeedRestoreCacheTest {

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
    fun initialCacheIsEmpty() {
        val cache = IllustFeedRestoreCache()

        assertNull(cache.snapshot)
    }

    @Test
    fun initialSnapshotFromConstructorIsExposed() {
        // 进程死亡恢复路径：stateKeeper 保存态经构造注入，必须立即可读
        val initial = IllustFeedSnapshot(illustIds = listOf(7L, 8L), nextUrl = "restored")

        val cache = IllustFeedRestoreCache(initial)

        assertEquals(listOf(7L, 8L), cache.snapshot?.illustIds, "构造注入的恢复快照必须立即生效")
        assertEquals("restored", cache.snapshot?.nextUrl)
    }

    @Test
    fun updateRecordsRawIllustIdsAndNextUrl() {
        val cache = IllustFeedRestoreCache()

        cache.update(listOf(illust(101L), illust(102L)), "https://app-api.pixiv.net/v1/illust/recommended?next=102")

        val snapshot = cache.snapshot
        assertEquals(listOf(101L, 102L), snapshot?.illustIds, "快照必须存原始列表的 ID，而非过滤后列表")
        assertEquals("https://app-api.pixiv.net/v1/illust/recommended?next=102", snapshot?.nextUrl)
    }

    @Test
    fun secondUpdateOverwritesAndAllowsNullNextUrl() {
        val cache = IllustFeedRestoreCache()
        cache.update(listOf(illust(101L)), "firstNext")

        // 边界：二次写入覆盖旧值，且下一页 URL 允许为 null（最后一页）
        cache.update(listOf(illust(202L)), null)

        assertEquals(listOf(202L), cache.snapshot?.illustIds)
        assertNull(cache.snapshot?.nextUrl)
    }
}
