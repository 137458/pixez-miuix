package com.perol.pixez.shared.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs

/**
 * 跨平台 DeepLink 解析测试：可信 OAuth 回调、任意 URL 的 code 注入拒绝、
 * 作品/画师链接与纯数字 ID 的解析边界（含 Long 化后的符号守卫）。
 */
class DeepLinkParserTest {

    // ---------- OAuth code 可信来源 ----------

    @Test
    fun `pixez scheme 的 account host 提取 OAuth code`() {
        val parsed = DeepLinkParser.parse("pixez://account?code=abc123")
        assertIs<ParsedDeepLink.OAuthLogin>(parsed)
        assertEquals("abc123", parsed.code)
    }

    @Test
    fun `pixiv net 官方回调路径提取 OAuth code`() {
        val parsed = DeepLinkParser.parse("https://www.pixiv.net/users/auth/pixiv/callback?code=xyz")
        assertIs<ParsedDeepLink.OAuthLogin>(parsed)
        assertEquals("xyz", parsed.code)
    }

    @Test
    fun `混排文本中携带可信 OAuth 链接时提取 code`() {
        val parsed = DeepLinkParser.parse("请复制此链接完成登录 pixez://account?code=mix999 谢谢")
        assertIs<ParsedDeepLink.OAuthLogin>(parsed)
        assertEquals("mix999", parsed.code)
    }

    @Test
    fun `任意 host 的 code 参数不触发 OAuth 登录（防注入）`() {
        assertNull(DeepLinkParser.parse("pixez://evil.com?code=X"))
        assertNull(DeepLinkParser.parse("https://evil.com/?code=X"))
        assertNull(DeepLinkParser.parse("看看这个 https://evil.com/?code=X 好东西"))
    }

    @Test
    fun `pixiv net 非 callback 路径的 code 不触发 OAuth 登录`() {
        val parsed = DeepLinkParser.parse("https://www.pixiv.net/other/page?code=X")
        assertIsNullOrNotOAuth(parsed)
    }

    private fun assertIsNullOrNotOAuth(parsed: ParsedDeepLink?) {
        if (parsed is ParsedDeepLink.OAuthLogin) {
            throw AssertionError("非 callback 路径不应触发 OAuth 登录: $parsed")
        }
    }

    // ---------- 作品 / 画师 / 纯数字 ----------

    @Test
    fun `artworks 链接解析作品 ID`() {
        val parsed = DeepLinkParser.parse("https://www.pixiv.net/artworks/123456")
        assertIs<ParsedDeepLink.IllustDetail>(parsed)
        assertEquals(123456L, parsed.illustId)
    }

    @Test
    fun `混排文本中的作品 ID 解析`() {
        val parsed = DeepLinkParser.parse("快看这张图 https://www.pixiv.net/artworks/987654 很棒")
        assertIs<ParsedDeepLink.IllustDetail>(parsed)
        assertEquals(987654L, parsed.illustId)
    }

    @Test
    fun `users 链接解析画师 ID`() {
        val parsed = DeepLinkParser.parse("pixiv://users/11451")
        assertIs<ParsedDeepLink.UserDetail>(parsed)
        assertEquals(11451L, parsed.userId)
    }

    @Test
    fun `6 到 10 位纯数字视为作品 ID`() {
        val parsed = DeepLinkParser.parse("123456")
        assertIs<ParsedDeepLink.IllustDetail>(parsed)
        assertEquals(123456L, parsed.illustId)
    }

    @Test
    fun `带符号或全零的纯数字串不解析为作品 ID`() {
        assertNull(DeepLinkParser.parse("-123456"))
        assertNull(DeepLinkParser.parse("+123456"))
        assertNull(DeepLinkParser.parse("000000"))
    }

    @Test
    fun `scheme 内 illust 段支持 Long ID`() {
        val parsed = DeepLinkParser.parse("pixez://illust/9999999999")
        assertIs<ParsedDeepLink.IllustDetail>(parsed)
        assertEquals(9999999999L, parsed.illustId)
    }

}

