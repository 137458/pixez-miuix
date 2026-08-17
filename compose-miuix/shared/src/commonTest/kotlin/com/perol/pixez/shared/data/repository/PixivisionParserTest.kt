package com.perol.pixez.shared.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PixivisionParserTest {

    @Test
    fun testParsePixivisionHtml() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="【特辑】治愈系蓝天插画特辑 - pixivision" />
                <meta property="og:description" content="[pixivision] 仰望晴空，感受微风拂过的清爽心情。" />
                <meta property="og:image" content="https://pixivision.net/images/spotlight_cover.jpg" />
            </head>
            <body>
                <div class="am__work-container">
                    <article class="am__work">
                        <a href="https://www.pixiv.net/artworks/12345678">
                            <img src="https://i.pximg.net/c/600x1200_90/img-master/img/2024/01/01/00/00/00/12345678_p0_master1200.jpg" alt="晴空与少女" />
                        </a>
                        <h3 class="am__title">晴空与少女</h3>
                        <div class="am__work-user">
                            <a href="https://www.pixiv.net/users/87654321">
                                <img class="avatar" src="https://i.pximg.net/user-profile/avatar.jpg" />
                                <p class="am__user">画师小明</p>
                            </a>
                        </div>
                    </article>
                    <article class="am__work">
                        <a href="https://www.pixiv.net/artworks/99887766">
                            <img src="https://i.pximg.net/c/600x1200_90/img-master/img/2024/02/02/00/00/00/99887766_p0_master1200.jpg" alt="云端之上" />
                        </a>
                        <h3 class="am__title">云端之上</h3>
                        <div class="am__work-user">
                            <a href="https://www.pixiv.net/users/11223344">
                                <img class="avatar" src="https://i.pximg.net/user-profile/avatar2.jpg" />
                                <p class="am__user">画师小红</p>
                            </a>
                        </div>
                    </article>
                </div>
            </body>
            </html>
        """.trimIndent()

        val detail = PixivisionParser.parse(sampleHtml, "https://www.pixivision.net/zh/a/1234")

        assertEquals("【特辑】治愈系蓝天插画特辑", detail.title)
        assertEquals("治愈系蓝天插画特辑", detail.pureTitle)
        assertEquals("仰望晴空，感受微风拂过的清爽心情。", detail.description)
        assertEquals("https://pixivision.net/images/spotlight_cover.jpg", detail.coverUrl)
        assertEquals(2, detail.works.size)

        val firstWork = detail.works[0]
        assertEquals("晴空与少女", firstWork.title)
        assertEquals("画师小明", firstWork.user)
        assertEquals(12345678, firstWork.illustId)
        assertEquals(87654321, firstWork.userId)
        assertNotNull(firstWork.showImage)
        assertTrue(firstWork.showImage?.contains("12345678") == true)

        val secondWork = detail.works[1]
        assertEquals("云端之上", secondWork.title)
        assertEquals("画师小红", secondWork.user)
        assertEquals(99887766, secondWork.illustId)
        assertEquals(11223344, secondWork.userId)
    }

    @Test
    fun testParseLazyLoadedPixivisionHtml() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="日落黄昏特辑" />
                <header>
                    <p>夕阳西下，余晖染红了天际。</p>
                </header>
            </head>
            <body>
                <article class="am__work">
                    <header>
                        <div class="am__work__user">
                            <a href="https://www.pixiv.net/users/556677">
                                <img class="am__work__userImage" src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" data-src="https://i.pximg.net/user-profile/avatar3.jpg" />
                                <p class="am__work__userName">黄昏画家</p>
                            </a>
                        </div>
                    </header>
                    <div class="am__work__main">
                        <a href="https://www.pixiv.net/artworks/77889900">
                            <img class="am__work__mainImage" src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" data-src="https://embed.pixiv.net/spotlight.php?id=77889900" />
                        </a>
                        <h3 class="am__work__title">日落余晖</h3>
                    </div>
                </article>
            </body>
            </html>
        """.trimIndent()

        val detail = PixivisionParser.parse(sampleHtml, "https://www.pixivision.net/zh/a/5678")

        assertEquals("日落黄昏特辑", detail.title)
        assertEquals("夕阳西下，余晖染红了天际。", detail.description)
        assertEquals(1, detail.works.size)

        val work = detail.works[0]
        assertEquals("日落余晖", work.title)
        assertEquals("黄昏画家", work.user)
        assertEquals(77889900, work.illustId)
        assertEquals(556677, work.userId)
        assertEquals("https://embed.pixiv.net/spotlight.php?id=77889900", work.showImage)
        assertEquals("https://i.pximg.net/user-profile/avatar3.jpg", work.userImage)
    }

    @Test
    fun testParseModernPixivisionArtistStructure() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="星空夜景特辑" />
            </head>
            <body>
                <div class="am__work">
                    <div class="am__work__user">
                        <a href="https://www.pixiv.net/users/998877" target="_blank" class="am__work__userLink">
                            <div class="am__work__userAvatar">
                                <img src="data:image/gif;base64,..." data-src="https://i.pximg.net/user-profile/star.jpg" class="am__work__userImage">
                            </div>
                            <div class="am__work__userInfo">
                                <p class="am__work__userName">星空旅人</p>
                            </div>
                        </a>
                    </div>
                    <div class="am__work__main">
                        <a href="https://www.pixiv.net/artworks/10101010">
                            <img data-src="https://i.pximg.net/c/600x1200_90/img-master/img/2024/03/03/00/00/00/10101010_p0_master1200.jpg" />
                        </a>
                        <h3 class="am__work__title">银河漫游</h3>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val detail = PixivisionParser.parse(sampleHtml, "https://www.pixivision.net/zh/a/9999")
        assertEquals(1, detail.works.size)
        val work = detail.works[0]
        assertEquals("银河漫游", work.title)
        assertEquals("星空旅人", work.user)
        assertEquals(10101010, work.illustId)
        assertEquals(998877, work.userId)
        assertEquals("https://i.pximg.net/c/600x1200_90/img-master/img/2024/03/03/00/00/00/10101010_p0_master1200.jpg", work.showImage)
    }

    @Test
    fun testParsePixivisionCollectionArticle() {
        val collectionHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="激发更多配色灵感！ - 各种主题色插画特辑【大合辑】 - pixivision" />
                <meta property="og:description" content="在pixivision，我们每天都会为大家准备各种「插画特辑」！" />
                <meta property="og:image" content="https://embed.pixiv.net/pixivision/zh/a/11967/ogimage.jpg" />
            </head>
            <body>
                <div class="_feature-article-body">
                    <div class="article-item _feature-article-body__paragraph">
                        <div class="fab__paragraph _medium-editor-text">
                            <div>这次我们特别整理了各种以色彩搭配为主题的插画特辑。</div>
                        </div>
                    </div>
                    <div class="article-item _feature-article-body__article_card">
                        <article class="_article-card spotlight">
                            <div class="arc__thumbnail-container">
                                <a href="/zh/a/11512">
                                    <div class="_thumbnail" style="background-image: url(https://i.pximg.net/c/1200x630/132794947_p0.jpg)"></div>
                                </a>
                            </div>
                            <div class="arc__title-container">
                                <h2 class="arc__title"><a href="/zh/a/11512">过目难忘 - 红色主题插画特辑 -</a></h2>
                            </div>
                            <div class="arc__footer-date-pr">
                                <time datetime="2026-03-27">2026.03.27</time>
                            </div>
                        </article>
                    </div>
                    <div class="article-item _feature-article-body__article_card">
                        <article class="_article-card spotlight">
                            <div class="arc__thumbnail-container">
                                <a href="/zh/a/11113">
                                    <div class="_thumbnail" style="background-image: url(https://i.pximg.net/c/1200x630/136033906_p0.jpg)"></div>
                                </a>
                            </div>
                            <div class="arc__title-container">
                                <h2 class="arc__title"><a href="/zh/a/11113">令人心动的世界♡ - 粉色主题插画特辑 -</a></h2>
                            </div>
                            <div class="arc__footer-date-pr">
                                <time datetime="2025-12-04">2025.12.04</time>
                            </div>
                        </article>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val detail = PixivisionParser.parse(collectionHtml, "https://www.pixivision.net/zh/a/11967")

        assertEquals("激发更多配色灵感！ - 各种主题色插画特辑【大合辑】", detail.title)
        assertEquals("激发更多配色灵感！ - 各种主题色插画特辑", detail.pureTitle)
        assertTrue(detail.isCollection)
        assertEquals(0, detail.works.size)
        assertEquals(2, detail.subArticles.size)

        val firstSub = detail.subArticles[0]
        assertEquals(11512, firstSub.id)
        assertEquals("过目难忘 - 红色主题插画特辑 -", firstSub.title)
        assertEquals("https://www.pixivision.net/zh/a/11512", firstSub.articleUrl)
        assertEquals("https://i.pximg.net/c/1200x630/132794947_p0.jpg", firstSub.thumbnail)
        assertEquals("2026-03-27", firstSub.publishDate)

        val secondSub = detail.subArticles[1]
        assertEquals(11113, secondSub.id)
        assertEquals("令人心动的世界♡ - 粉色主题插画特辑 -", secondSub.title)
        assertEquals("https://www.pixivision.net/zh/a/11113", secondSub.articleUrl)
        assertEquals("https://i.pximg.net/c/1200x630/136033906_p0.jpg", secondSub.thumbnail)
        assertEquals("2025-12-04", secondSub.publishDate)
    }
}

