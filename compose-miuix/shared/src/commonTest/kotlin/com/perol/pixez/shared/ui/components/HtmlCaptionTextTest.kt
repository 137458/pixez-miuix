package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlCaptionTextTest {

    @Test
    fun testDecodeHtmlEntities() {
        val input = "&quot;Hello &amp; World&quot; &lt;tag&gt; &#39;test&#39; &nbsp; &#65; &#x42;"
        val decoded = decodeHtmlEntities(input)
        assertEquals("\"Hello & World\" <tag> 'test'   A B", decoded)
    }

    @Test
    fun testParseHtmlAnchorTags() {
        val input = "作者推特：<a href=\"https://twitter.com/artist\">@artist</a> 欢迎关注"
        var clickedUrl = ""
        val annotated = parseHtmlCaption(
            html = input,
            linkColor = Color.Blue,
            onLinkClick = { clickedUrl = it },
        )

        assertEquals("作者推特：@artist 欢迎关注", annotated.text)
        val linkAnnotations = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals(1, linkAnnotations.size)
        val firstLink = linkAnnotations.first().item
        assertEquals("https://twitter.com/artist", (firstLink as androidx.compose.ui.text.LinkAnnotation.Url).url)
    }

    @Test
    fun testParseLineBreaksAndParagraphs() {
        val input = "第一行<br />第二行<br>第三行<p>段落一</p><p>段落二</p>"
        val annotated = parseHtmlCaption(
            html = input,
            linkColor = Color.Blue,
            onLinkClick = {},
        )
        assertEquals("第一行\n第二行\n第三行\n段落一\n\n段落二", annotated.text)
    }

    @Test
    fun testAutoLinkPlainUrls() {
        val input = "个人主页：https://pixiv.me/test_user 欢迎访问"
        val annotated = parseHtmlCaption(
            html = input,
            linkColor = Color.Blue,
            onLinkClick = {},
        )
        assertEquals("个人主页：https://pixiv.me/test_user 欢迎访问", annotated.text)
        val linkAnnotations = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals(1, linkAnnotations.size)
        val firstLink = linkAnnotations.first().item
        assertEquals("https://pixiv.me/test_user", (firstLink as androidx.compose.ui.text.LinkAnnotation.Url).url)
    }

    @Test
    fun testHandleCaptionLinkDispatch() {
        var clickedUserId: Int? = null
        var clickedIllustId: Int? = null
        var clickedWebUrl: String? = null

        val reset = {
            clickedUserId = null
            clickedIllustId = null
            clickedWebUrl = null
        }

        // 1. Pixiv User App 协议
        reset()
        handleCaptionLink(
            url = "pixiv://users/123456",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(123456, clickedUserId)
        assertEquals(null, clickedIllustId)
        assertEquals(null, clickedWebUrl)

        // 2. Pixiv User Web 链接
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/users/789012",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(789012, clickedUserId)
        assertEquals(null, clickedIllustId)
        assertEquals(null, clickedWebUrl)

        // 3. Pixiv Illust App 协议
        reset()
        handleCaptionLink(
            url = "pixiv://illusts/987654",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(null, clickedUserId)
        assertEquals(987654, clickedIllustId)
        assertEquals(null, clickedWebUrl)

        // 4. Pixiv Illust Web 链接
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/artworks/554433",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(null, clickedUserId)
        assertEquals(554433, clickedIllustId)
        assertEquals(null, clickedWebUrl)

        // 5. 外部网页链接
        reset()
        handleCaptionLink(
            url = "https://twitter.com/example",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(null, clickedUserId)
        assertEquals(null, clickedIllustId)
        assertEquals("https://twitter.com/example", clickedWebUrl)
    }
}
