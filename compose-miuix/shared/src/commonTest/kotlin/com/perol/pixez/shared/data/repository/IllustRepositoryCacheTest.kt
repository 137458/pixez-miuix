package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
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

/**
 * 高-5 后的仓库内存缓存测试：仓库只存原始数据缓存（作品 LRU / 推荐响应 / Spotlight），
 * 跨页恢复快照已移至 ui/state 的恢复缓存（见 IllustFeedRestoreCacheTest / RankingFeedRestoreCacheTest）。
 */
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
    fun cacheIllustRoundTripsById() = runTest {
        val repo = createRepository()
        assertNull(repo.getCachedIllust(101L))

        repo.cacheIllust(createSampleIllust(101L))

        assertEquals(101L, repo.getCachedIllust(101L)?.id)
        assertNull(repo.getCachedIllust(102L), "未缓存的 ID 读回为 null")
    }

    @Test
    fun clearMemoryCacheEvictsIllustCache() = runTest {
        val repo = createRepository()
        repo.cacheIllust(createSampleIllust(201L))
        assertEquals(201L, repo.getCachedIllust(201L)?.id)

        repo.clearMemoryCache()

        assertNull(repo.getCachedIllust(201L), "清内存缓存必须连插画 LRU 一并失效（缓存清理缺失修复）")
    }

    @Test
    fun testSpotlightArticlesCacheAndClear() = runTest {
        val repo = createRepository()
        assertNull(repo.getCachedSpotlightArticles("novel"))
        repo.clearMemoryCache()
        assertNull(repo.getCachedSpotlightArticles("novel"))
    }
}
