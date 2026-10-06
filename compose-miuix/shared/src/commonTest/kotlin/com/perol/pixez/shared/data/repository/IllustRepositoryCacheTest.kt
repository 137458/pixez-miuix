package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IllustRepositoryCacheTest {

    private fun createRepository(): IllustRepository {
        val dummyClient = HttpClient()
        return IllustRepository(apiClient = dummyClient, webClient = dummyClient)
    }

    private fun createSampleIllust(id: Long): Illust = Illust(
        id = id,
        title = "Test $id",
        type = "illust",
        imageUrls = com.perol.pixez.shared.data.model.ImageUrls(squareMedium = "", medium = "", large = ""),
        caption = "Caption",
        restrict = 0,
        user = com.perol.pixez.shared.data.model.IllustUser(
            id = 1L,
            name = "User",
            account = "user",
            profileImageUrls = com.perol.pixez.shared.data.model.IllustProfileImageUrls(""),
        ),
        tags = emptyList(),
        tools = emptyList(),
        createDate = "2026-01-01",
        pageCount = 1,
        width = 1000,
        height = 1000,
        sanityLevel = 2,
        xRestrict = 0,
        series = null,
        metaSinglePage = com.perol.pixez.shared.data.model.MetaSinglePage(null),
        metaPages = emptyList(),
        totalView = 100,
        totalBookmarks = 10,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        totalComments = 0,
        illustAIType = 0,
        illustBookStyle = 0,
    )

    @Test
    fun testRecommendedActiveCachePersistence() {
        val repo = createRepository()
        assertNull(repo.activeRecommendedIllusts)
        assertNull(repo.activeRecommendedNextUrl)

        val list = listOf(createSampleIllust(101L), createSampleIllust(102L))
        repo.updateRecommendedFeed(list, "https://app-api.pixiv.net/v1/illust/recommended?max_bookmark_id=102")

        assertEquals(2, repo.activeRecommendedIllusts?.size)
        assertEquals(101L, repo.activeRecommendedIllusts?.first()?.id)
        assertEquals("https://app-api.pixiv.net/v1/illust/recommended?max_bookmark_id=102", repo.activeRecommendedNextUrl)
    }

    @Test
    fun testRecommendedFeedUpdateOverwrites() {
        val repo = createRepository()
        repo.updateRecommendedFeed(listOf(createSampleIllust(101L)), "firstNext")

        // 边界：二次写入覆盖旧值，且 nextUrl 允许为 null（最后一页）
        repo.updateRecommendedFeed(listOf(createSampleIllust(202L)), null)

        assertEquals(1, repo.activeRecommendedIllusts?.size)
        assertEquals(202L, repo.activeRecommendedIllusts?.first()?.id)
        assertNull(repo.activeRecommendedNextUrl)
    }

    @Test
    fun testRankingPageCachePersistence() {
        val repo = createRepository()
        assertNull(repo.getCachedRankingPage("day_"))

        val list = listOf(createSampleIllust(301L), createSampleIllust(302L))
        repo.updateRankingPage("day_", list, "rankingNext")

        val cached = repo.getCachedRankingPage("day_")
        assertEquals(2, cached?.first?.size)
        assertEquals(301L, cached?.first?.first()?.id)
        assertEquals("rankingNext", cached?.second)
        // 未写入的 key 读回为 null
        assertNull(repo.getCachedRankingPage("week_"))

        // 边界：同 key 二次写入覆盖，且 nextUrl 允许为 null
        repo.updateRankingPage("day_", listOf(createSampleIllust(303L)), null)
        val overwritten = repo.getCachedRankingPage("day_")
        assertEquals(1, overwritten?.first?.size)
        assertEquals(303L, overwritten?.first?.first()?.id)
        assertNull(overwritten?.second)
    }

    @Test
    fun testClearActiveCache() {
        val repo = createRepository()
        repo.updateRecommendedFeed(listOf(createSampleIllust(201L)), "nextUrl")
        repo.updateRankingPage("day", listOf(createSampleIllust(301L)), "rankingNext")

        repo.clearMemoryCache()

        assertNull(repo.activeRecommendedIllusts)
        assertNull(repo.activeRecommendedNextUrl)
        assertNull(repo.getCachedRankingPage("day"))
    }
}
