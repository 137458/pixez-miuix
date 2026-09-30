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
        repo.activeRecommendedIllusts = list
        repo.activeRecommendedNextUrl = "https://app-api.pixiv.net/v1/illust/recommended?max_bookmark_id=102"

        assertEquals(2, repo.activeRecommendedIllusts?.size)
        assertEquals(101L, repo.activeRecommendedIllusts?.first()?.id)
        assertEquals("https://app-api.pixiv.net/v1/illust/recommended?max_bookmark_id=102", repo.activeRecommendedNextUrl)
    }

    @Test
    fun testClearActiveCache() {
        val repo = createRepository()
        repo.activeRecommendedIllusts = listOf(createSampleIllust(201L))
        repo.activeRecommendedNextUrl = "nextUrl"
        repo.activeRankingIllusts["day"] = listOf(createSampleIllust(301L)) to "rankingNext"

        repo.clearMemoryCache()

        assertNull(repo.activeRecommendedIllusts)
        assertNull(repo.activeRecommendedNextUrl)
        assertEquals(0, repo.activeRankingIllusts.size)
    }
}
