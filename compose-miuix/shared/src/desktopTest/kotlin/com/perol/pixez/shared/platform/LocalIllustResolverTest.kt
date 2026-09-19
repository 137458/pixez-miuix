package com.perol.pixez.shared.platform

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.memory.MemoryCache
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.MetaSinglePage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalIllustResolverTest {

    private val context = PlatformContext.INSTANCE

    @BeforeTest
    fun setUp() {
        SingletonImageLoader.reset()
        val loader = ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(10 * 1024 * 1024)
                    .build()
            }
            .build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @AfterTest
    fun tearDown() {
        SingletonImageLoader.reset()
    }

    private fun createDummyIllust(id: Int = 123456): Illust {
        return Illust(
            id = id,
            title = "Test Artwork",
            type = "illust",
            imageUrls = ImageUrls(
                squareMedium = "https://i.pximg.net/c/360x360_70/custom-thumb.jpg",
                medium = "https://i.pximg.net/c/540x540_70/img-master/img/2026/09/19/123456_p0_master1200.jpg",
                large = "https://i.pximg.net/c/600x1200_90/img-master/img/2026/09/19/123456_p0_master1200.jpg",
            ),
            caption = "Test Caption",
            restrict = 0,
            user = IllustUser(
                id = 999,
                name = "Artist",
                account = "artist_acc",
                profileImageUrls = IllustProfileImageUrls(""),
                isFollowed = false,
            ),
            tags = emptyList(),
            tools = emptyList(),
            createDate = "2026-09-19T00:00:00+00:00",
            pageCount = 1,
            width = 1000,
            height = 2000,
            sanityLevel = 2,
            xRestrict = 0,
            series = null,
            metaSinglePage = MetaSinglePage(
                originalImageUrl = "https://i.pximg.net/img-original/img/2026/09/19/123456_p0.jpg"
            ),
            metaPages = emptyList(),
            totalView = 100,
            totalBookmarks = 50,
            isBookmarked = false,
            visible = true,
            isMuted = false,
            illustAIType = 1,
            illustBookStyle = 0,
        )
    }

    private class FakeImage(
        override val size: Long = 100L,
        override val width: Int = 10,
        override val height: Int = 10,
        override val shareable: Boolean = true,
    ) : coil3.Image {
        override fun draw(canvas: org.jetbrains.skia.Canvas) {}
    }

    @Test
    fun testIsUrlInCoilMemoryCache() {
        val originalUrl = "https://i.pximg.net/img-original/img/2026/09/19/123456_p0.jpg"
        assertFalse(isUrlInCoilMemoryCache(context, originalUrl))

        val loader = SingletonImageLoader.get(context)
        val memCache = loader.memoryCache
        if (memCache != null) {
            memCache[MemoryCache.Key(originalUrl)] = MemoryCache.Value(
                image = FakeImage(),
            )
            assertTrue(isUrlInCoilMemoryCache(context, originalUrl))
            assertTrue(isUrlInCoilCache(context, originalUrl))
        }
    }

    @Test
    fun testResolveOptimizedImageModelPrefersMemoryCache() {
        val illust = createDummyIllust()
        val originalUrl = illust.metaSinglePage?.originalImageUrl.orEmpty()
        val targetUrl = illust.imageUrls.large

        // 缓存未命中时返回 targetUrl
        val unhitModel = resolveOptimizedImageModel(
            context = context,
            illust = illust,
            pageIndex = 0,
            targetUrl = targetUrl,
            originalUrl = originalUrl,
        )
        assertEquals(targetUrl, unhitModel)

        // 内存缓存命中时应优先返回 originalUrl
        val loader = SingletonImageLoader.get(context)
        val memCache = loader.memoryCache
        if (memCache != null) {
            memCache[MemoryCache.Key(originalUrl)] = MemoryCache.Value(
                image = FakeImage(),
            )
            val hitModel = resolveOptimizedImageModel(
                context = context,
                illust = illust,
                pageIndex = 0,
                targetUrl = targetUrl,
                originalUrl = originalUrl,
            )
            assertEquals(originalUrl, hitModel)
        }
    }
}
