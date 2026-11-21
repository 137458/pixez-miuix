package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.ImageUrls
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [resolveIllustCoverUrl] 的封面画质映射验证。
 *
 * 列表卡片与详情页转场缩略图共用这一条映射，一旦口径漂移，
 * 卡片展开转场首帧就会因缓存未命中而闪白。
 */
class IllustCoverUrlTest {

    private val imageUrls = ImageUrls(
        squareMedium = "https://i.pximg.net/square.jpg",
        medium = "https://i.pximg.net/medium.jpg",
        large = "https://i.pximg.net/large.jpg",
    )

    @Test
    fun `默认画质取 medium`() {
        assertEquals("https://i.pximg.net/medium.jpg", resolveIllustCoverUrl(imageUrls, null))
        assertEquals("https://i.pximg.net/medium.jpg", resolveIllustCoverUrl(imageUrls, 0))
    }

    @Test
    fun `高画质取 large 低画质取方图`() {
        assertEquals("https://i.pximg.net/large.jpg", resolveIllustCoverUrl(imageUrls, 1))
        assertEquals("https://i.pximg.net/square.jpg", resolveIllustCoverUrl(imageUrls, 2))
    }

    @Test
    fun `未知画质档位回退为 medium`() {
        assertEquals("https://i.pximg.net/medium.jpg", resolveIllustCoverUrl(imageUrls, 99))
    }

    @Test
    fun `首选画质为空串时按 medium 再方图逐级回退`() {
        val blankLarge = ImageUrls(
            squareMedium = "https://i.pximg.net/square.jpg",
            medium = "",
            large = "",
        )
        assertEquals("https://i.pximg.net/square.jpg", resolveIllustCoverUrl(blankLarge, 1))

        val allBlankExceptMedium = ImageUrls(
            squareMedium = "",
            medium = "https://i.pximg.net/medium.jpg",
            large = "",
        )
        assertEquals("https://i.pximg.net/medium.jpg", resolveIllustCoverUrl(allBlankExceptMedium, 1))
    }
}
