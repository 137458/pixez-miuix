package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlCaptionTextTest {

    @Test
    fun testDecodeHtmlEntities() {
        val input = "&quot;Hello &amp; World&quot; &lt;tag&gt; &#39;test&#39; &nbsp; &#65; &#x42; &copy; &mdash; &#128512;"
        val decoded = decodeHtmlEntities(input)
        assertEquals("\"Hello & World\" <tag> 'test'   A B © — \uD83D\uDE00", decoded)
    }

    @Test
    fun testDecodeUrlPercent() {
        assertEquals("https://twitter.com/artist", decodeUrlPercent("https%3A%2F%2Ftwitter.com%2Fartist"))
        assertEquals("你好 世界", decodeUrlPercent("%E4%BD%A0%E5%A5%BD+%E4%B8%96%E7%95%8C"))
    }

    @Test
    fun testUnwrapPixivJumpUrl() {
        val jumpUrl1 = "https://www.pixiv.net/jump.php?url=https%3A%2F%2Ftwitter.com%2Fartist"
        assertEquals("https://twitter.com/artist", unwrapPixivJumpUrl(jumpUrl1))

        val jumpUrl2 = "https://www.pixiv.net/jump.php?https%3A%2F%2Ffanbox.cc%2F%40artist"
        assertEquals("https://fanbox.cc/@artist", unwrapPixivJumpUrl(jumpUrl2))

        val normalUrl = "https://example.com/test"
        assertEquals(normalUrl, unwrapPixivJumpUrl(normalUrl))
    }

    @Test
    fun testParseHtmlAnchorTagsWithJumpUnwrap() {
        val input = "作者推特：<a href=\"https://www.pixiv.net/jump.php?url=https%3A%2F%2Ftwitter.com%2Fartist\" target=\"_blank\"><b>@artist</b></a> 欢迎关注"
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
    fun testAutoLinkPlainUrlsAndShortcuts() {
        val input = "个人主页：https://pixiv.me/test_user\n推特：twitter.com/artist.\n前作：illust_id=12345678\n作者：user_id=87654321"
        val annotated = parseHtmlCaption(
            html = input,
            linkColor = Color.Blue,
            onLinkClick = {},
        )
        val linkAnnotations = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals(4, linkAnnotations.size)

        val urls = linkAnnotations.map { (it.item as androidx.compose.ui.text.LinkAnnotation.Url).url }
        assertEquals("https://pixiv.me/test_user", urls[0])
        assertEquals("https://twitter.com/artist", urls[1])
        assertEquals("pixiv://illusts/12345678", urls[2])
        assertEquals("pixiv://users/87654321", urls[3])
    }

    @Test
    fun testHandleCaptionLinkDispatch() {
        var clickedUserId: Int? = null
        var clickedIllustId: Int? = null
        var clickedSeriesId: Int? = null
        var clickedNovelId: Int? = null
        var clickedTag: String? = null
        var clickedWebUrl: String? = null

        val reset = {
            clickedUserId = null
            clickedIllustId = null
            clickedSeriesId = null
            clickedNovelId = null
            clickedTag = null
            clickedWebUrl = null
        }

        // 1. Pixiv User 链接 (Web 与 App 协议)
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/en/users/789012",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onIllustSeriesClick = { clickedSeriesId = it },
            onNovelClick = { clickedNovelId = it },
            onTagClick = { clickedTag = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(789012, clickedUserId)

        // 2. Pixiv Illust 链接 (短链 i 与 member_illust.php)
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/i/554433",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onIllustSeriesClick = { clickedSeriesId = it },
            onNovelClick = { clickedNovelId = it },
            onTagClick = { clickedTag = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(554433, clickedIllustId)

        // 3. Pixiv 系列链接
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/user/100/series/9988",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onIllustSeriesClick = { clickedSeriesId = it },
            onNovelClick = { clickedNovelId = it },
            onTagClick = { clickedTag = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(9988, clickedSeriesId)

        // 4. Pixiv 小说链接
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/novel/show.php?id=332211",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onIllustSeriesClick = { clickedSeriesId = it },
            onNovelClick = { clickedNovelId = it },
            onTagClick = { clickedTag = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals(332211, clickedNovelId)

        // 5. Pixiv 标签链接
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/tags/%E5%8E%9F%E7%A5%9E/artworks",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onIllustSeriesClick = { clickedSeriesId = it },
            onNovelClick = { clickedNovelId = it },
            onTagClick = { clickedTag = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals("原神", clickedTag)

        // 6. Pixiv Jump.php 外链直达
        reset()
        handleCaptionLink(
            url = "https://www.pixiv.net/jump.php?url=https%3A%2F%2Ftwitter.com%2Fexample",
            onUserClick = { clickedUserId = it },
            onIllustClick = { clickedIllustId = it },
            onIllustSeriesClick = { clickedSeriesId = it },
            onNovelClick = { clickedNovelId = it },
            onTagClick = { clickedTag = it },
            onLinkClick = { clickedWebUrl = it },
        )
        assertEquals("https://twitter.com/example", clickedWebUrl)
    }
}

