package com.perol.pixez.shared.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 可信 URL 策略测试：[TrustedUrlPolicy] 是 API 分页、图片、特辑与更新包请求的
 * 安全闸门，必须拒绝伪造 host（子域后缀拼接、嵌入 host）、非 HTTPS 与携带凭据的 URL。
 */
class TrustedUrlPolicyTest {

    @Test
    fun `apiPaginationUrl 接受受信任 API host 并原样返回`() {
        val raw = "https://app-api.pixiv.net/v1/search/illust?word=w&offset=30"

        val result = TrustedUrlPolicy.apiPaginationUrl(raw)

        assertEquals(raw, result)
    }

    @Test
    fun `imageUrl 接受两个受信任图片 host`() {
        assertEquals("https://i.pximg.net/c/600x600/img.jpg", TrustedUrlPolicy.imageUrl("https://i.pximg.net/c/600x600/img.jpg"))
        assertEquals("https://s.pximg.net/a.png", TrustedUrlPolicy.imageUrl("https://s.pximg.net/a.png"))
    }

    @Test
    fun `伪造子域后缀拼接的 host 被拒绝`() {
        // contains/endsWith 式匹配会放行此类 host，策略要求精确匹配
        listOf(
            "https://app-api.pixiv.net.attacker.com/v1/search/illust",
            "https://i.pximg.net.evil.io/img.jpg",
            "https://evil-app-api.pixiv.net/x",
        ).forEach { raw ->
            assertFailsWith<IllegalArgumentException>("应拒绝伪造 host: $raw") {
                TrustedUrlPolicy.apiPaginationUrl(raw)
            }
        }
    }

    @Test
    fun `非受信任 host 的图片 URL 被拒绝`() {
        listOf(
            "https://example.com/a.jpg",
            "https://pximg.net/a.jpg",
            "https://user-content.pixiv.net/a.jpg",
        ).forEach { raw ->
            assertFailsWith<IllegalArgumentException>("应拒绝非受信任图片 host: $raw") {
                TrustedUrlPolicy.imageUrl(raw)
            }
        }
    }

    @Test
    fun `HTTP 明文 URL 被拒绝`() {
        listOf(
            "http://app-api.pixiv.net/v1/x",
            "http://i.pximg.net/a.jpg",
            "http://www.pixivision.net/a/123",
        ).forEach { raw ->
            assertFailsWith<IllegalArgumentException>("应拒绝非 HTTPS: $raw") {
                when {
                    raw.contains("app-api") -> TrustedUrlPolicy.apiPaginationUrl(raw)
                    raw.contains("pixivision") -> TrustedUrlPolicy.spotlightUrl(raw)
                    else -> TrustedUrlPolicy.imageUrl(raw)
                }
            }
        }
    }

    @Test
    fun `携带用户凭据的 URL 被拒绝`() {
        assertFailsWith<IllegalArgumentException>("应拒绝 userinfo 凭据") {
            TrustedUrlPolicy.apiPaginationUrl("https://user:pass@app-api.pixiv.net/v1/x")
        }
        assertFailsWith<IllegalArgumentException>("应拒绝 userinfo 用户名") {
            TrustedUrlPolicy.imageUrl("https://user@i.pximg.net/a.jpg")
        }
    }

    @Test
    fun `格式无效或空白的 URL 被拒绝`() {
        listOf("not a url", "https://", "     ", "http://").forEach { raw ->
            assertFailsWith<IllegalArgumentException>("应拒绝无效 URL: '$raw'") {
                TrustedUrlPolicy.apiPaginationUrl(raw)
            }
        }
    }

    @Test
    fun `host 大小写不敏感匹配`() {
        assertEquals("https://APP-API.PIXIV.NET/v1/x", TrustedUrlPolicy.apiPaginationUrl("https://APP-API.PIXIV.NET/v1/x"))
        assertEquals("https://I.PXIMG.NET/a.jpg", TrustedUrlPolicy.imageUrl("https://I.PXIMG.NET/a.jpg"))
    }

    @Test
    fun `spotlightUrl 仅接受 pixivision 域`() {
        assertEquals("https://www.pixivision.net/zh/a/123", TrustedUrlPolicy.spotlightUrl("https://www.pixivision.net/zh/a/123"))
        assertFailsWith<IllegalArgumentException> {
            TrustedUrlPolicy.spotlightUrl("https://www.pixiv.net/zh/a/123")
        }
    }

    @Test
    fun `releaseAssetUrl 仅接受 GitHub 更新包 host 集合`() {
        assertTrue(
            TrustedUrlPolicy.releaseAssetUrl("https://github.com/perol-nxfirever/PixEz/releases/download/v1/app.apk").isNotEmpty(),
        )
        assertTrue(
            TrustedUrlPolicy.releaseAssetUrl("https://objects.githubusercontent.com/release-bucket/app.apk").isNotEmpty(),
        )
        assertFailsWith<IllegalArgumentException> {
            TrustedUrlPolicy.releaseAssetUrl("https://evil.github.com.clone.example/app.apk")
        }
    }

    @Test
    fun `输入首尾空白被容忍且返回规范化后的字符串`() {
        val result = TrustedUrlPolicy.apiPaginationUrl("  https://app-api.pixiv.net/v1/x  ")

        assertEquals("https://app-api.pixiv.net/v1/x", result, "返回值应为去除首尾空白后的 URL")
    }

    // region S-3 小组件镜像源：host 规范化与逐跳放行判定

    @Test
    fun `normalizeImageHost 接受裸 host 并小写规范化`() {
        assertEquals("i.pixiv.re", TrustedUrlPolicy.normalizeImageHost("i.pixiv.re"))
        assertEquals("i.pixiv.re", TrustedUrlPolicy.normalizeImageHost("  I.PIXIV.RE  "))
        assertEquals("i.pximg.net", TrustedUrlPolicy.normalizeImageHost("i.pximg.net"))
    }

    @Test
    fun `normalizeImageHost 从带 scheme 与端口路径的写法中只取 host`() {
        assertEquals("i.pixiv.re", TrustedUrlPolicy.normalizeImageHost("https://i.pixiv.re"))
        assertEquals("i.pixiv.re", TrustedUrlPolicy.normalizeImageHost("http://i.pixiv.re:8443/path?q=1#frag"))
        assertEquals("i.pixiv.re", TrustedUrlPolicy.normalizeImageHost("//i.pixiv.re/img.jpg"))
        assertEquals("i.pixiv.re", TrustedUrlPolicy.normalizeImageHost("i.pixiv.re/path"))
    }

    @Test
    fun `normalizeImageHost 拒绝空白与非法 host 写法`() {
        listOf(
            null, "", "   ", "not a host", "i pixiv.re",
            "user:pass@i.pixiv.re", "@i.pixiv.re", "i.pixiv.re-", "-i.pixiv.re",
            "i.pixiv.re.", ".pixiv.re", "/just/a/path", "ftp://x", "http://", "i.pixiv.re:",
        ).forEach { raw ->
            assertNull(TrustedUrlPolicy.normalizeImageHost(raw), "应判定为非法 host: $raw")
        }
    }

    @Test
    fun `isTrustedImageHost 放行 pximg 系与预置镜像且精确匹配`() {
        assertTrue(TrustedUrlPolicy.isTrustedImageHost("i.pximg.net", null))
        assertTrue(TrustedUrlPolicy.isTrustedImageHost("s.pximg.net", null))
        assertTrue(TrustedUrlPolicy.isTrustedImageHost("I.PXIMG.NET", null))
        assertTrue(TrustedUrlPolicy.isTrustedImageHost("i.pixiv.re", null))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("example.com", null))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("i.pximg.net.evil.io", null))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("ei.pximg.net", null))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost(null, null))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("", null))
    }

    @Test
    fun `isTrustedImageHost 仅额外放行规范化后的用户配置镜像 host`() {
        assertTrue(TrustedUrlPolicy.isTrustedImageHost("mirror.example.com", "mirror.example.com"))
        assertTrue(TrustedUrlPolicy.isTrustedImageHost("MIRROR.EXAMPLE.COM", "https://mirror.example.com/"))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("mirror.example.com", "other.example.com"))
        // 配置值非法时不得放宽白名单
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("mirror.example.com", "mirror.example.com.evil.io"))
        assertFalse(TrustedUrlPolicy.isTrustedImageHost("evil.io", "  "))
    }

    @Test
    fun `widgetImageUrl 接受白名单与镜像 host 且要求 HTTPS`() {
        assertEquals(
            "https://i.pximg.net/c/600x600/a.jpg",
            TrustedUrlPolicy.widgetImageUrl("https://i.pximg.net/c/600x600/a.jpg", mirrorHost = "i.pixiv.re"),
        )
        assertEquals(
            "https://i.pixiv.re/c/600x600/a.jpg",
            TrustedUrlPolicy.widgetImageUrl("https://i.pixiv.re/c/600x600/a.jpg", mirrorHost = null),
        )
        assertEquals(
            "https://mirror.example.com/a.jpg",
            TrustedUrlPolicy.widgetImageUrl("https://mirror.example.com/a.jpg", mirrorHost = "mirror.example.com"),
        )
        assertFailsWith<IllegalArgumentException> {
            TrustedUrlPolicy.widgetImageUrl("http://i.pximg.net/a.jpg", mirrorHost = null)
        }
        assertFailsWith<IllegalArgumentException> {
            TrustedUrlPolicy.widgetImageUrl("https://user@i.pximg.net/a.jpg", mirrorHost = null)
        }
    }

    @Test
    fun `widgetImageUrl 未配置镜像时拒绝任意非白名单 host`() {
        listOf(
            "https://attacker.com/a.jpg",
            "https://i.pximg.net.attacker.com/a.jpg",
            "https://mirror.example.com/a.jpg",
        ).forEach { raw ->
            assertFailsWith<IllegalArgumentException>("应拒绝非放行 host: $raw") {
                TrustedUrlPolicy.widgetImageUrl(raw, mirrorHost = null)
            }
        }
    }

    // endregion
}
