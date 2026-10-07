package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * commonTest 通用的内存版 Settings 替身：
 * multiplatform-settings 1.3.0 核心构件无目标无关的 Settings 构造方式（无参工厂在独立的
 * no-arg 构件中），故按接口最小实现，供同包测试构造 SettingsRepository 使用。
 */
internal class InMemorySettings : Settings {
    private val storage = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = storage.keys.toSet()
    override val size: Int get() = storage.size

    override fun clear() = storage.clear()
    override fun remove(key: String) { storage.remove(key) }
    override fun hasKey(key: String): Boolean = storage.containsKey(key)

    override fun putInt(key: String, value: Int) { storage[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = storage[key] as? Int

    override fun putLong(key: String, value: Long) { storage[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = storage[key] as? Long

    override fun putString(key: String, value: String) { storage[key] = value }
    override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue
    override fun getStringOrNull(key: String): String? = storage[key] as? String

    override fun putFloat(key: String, value: Float) { storage[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = storage[key] as? Float

    override fun putDouble(key: String, value: Double) { storage[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = storage[key] as? Double

    override fun putBoolean(key: String, value: Boolean) { storage[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = storage[key] as? Boolean
}

class IllustRepositoryCacheTest {

    private fun createRepository(): IllustRepository {
        val dummyClient = HttpClient()
        return IllustRepository(
            apiClient = dummyClient,
            webClient = dummyClient,
            settingsRepository = SettingsRepository(InMemorySettings()),
        )
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
    fun testRankingCacheLruEviction() {
        val repo = createRepository()

        // 常规：写入 9 个不同 key，超过容量上限（8）后最久未用的 key_0 被淘汰
        repeat(9) { index ->
            repo.updateRankingPage("key_$index", listOf(createSampleIllust(index.toLong() + 1)), null)
        }
        assertNull(repo.getCachedRankingPage("key_0"), "超出容量后最久未用 key 应被淘汰")
        for (index in 1..8) {
            assertEquals(1, repo.getCachedRankingPage("key_$index")?.first?.size, "key_$index 应仍在缓存")
        }

        // 边界：同 key 重写视为最近使用，被提升到最新，不再是最早淘汰对象
        repo.updateRankingPage("key_1", listOf(createSampleIllust(999L)), "refreshed")
        repo.updateRankingPage("key_9", listOf(createSampleIllust(1000L)), null)

        val promoted = repo.getCachedRankingPage("key_1")
        assertEquals(1, promoted?.first?.size, "重写后的 key_1 应保留")
        assertEquals(999L, promoted?.first?.first()?.id)
        assertEquals("refreshed", promoted?.second)
        assertNull(repo.getCachedRankingPage("key_2"), "key_1 被提升后，key_2 成为最久未用，应被新写入淘汰")
        assertEquals(1, repo.getCachedRankingPage("key_3")?.first?.size, "key_3 应仍在缓存")
        assertEquals(1, repo.getCachedRankingPage("key_9")?.first?.size, "最近写入的 key_9 应仍在缓存")
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

    @Test
    fun testSpotlightArticlesCacheAndClear() {
        val repo = createRepository()
        assertNull(repo.getCachedSpotlightArticles("novel"))
        repo.clearMemoryCache()
        assertNull(repo.getCachedSpotlightArticles("novel"))
    }
}
