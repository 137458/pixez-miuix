package com.perol.pixez.shared.data.model

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SauceNaoParserTest {

    @Test
    fun `解析标准 JSON 响应成功提取 Pixiv 结果`() {
        val json = """
        {
          "header": {
            "status": 0,
            "results_requested": 5
          },
          "results": [
            {
              "header": {
                "similarity": "94.62",
                "thumbnail": "https://img1.saucenao.com/t/2026/01/test1.jpg"
              },
              "data": {
                "ext_urls": [
                  "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=12345678"
                ],
                "title": "测试作品1",
                "pixiv_id": 12345678,
                "member_name": "画师A",
                "member_id": 99999
              }
            },
            {
              "header": {
                "similarity": "88.10",
                "thumbnail": "https://img1.saucenao.com/t/2026/01/test2.jpg"
              },
              "data": {
                "ext_urls": [
                  "https://www.pixiv.net/artworks/87654321"
                ],
                "title": "测试作品2",
                "pixiv_id": "87654321",
                "author_name": "画师B"
              }
            }
          ]
        }
        """.trimIndent()

        val results = SauceNaoParser.parse(json)
        assertEquals(2, results.size)

        val first = results[0]
        assertEquals(12345678L, first.illustId)
        assertEquals("测试作品1", first.title)
        assertEquals("画师A", first.author)
        assertEquals("94.62", first.similarity)
        assertEquals("https://img1.saucenao.com/t/2026/01/test1.jpg", first.thumbnail)

        val second = results[1]
        assertEquals(87654321L, second.illustId)
        assertEquals("测试作品2", second.title)
        assertEquals("画师B", second.author)
        assertEquals("88.10", second.similarity)
    }

    @Test
    fun `无 pixiv_id 字段但 ext_urls 包含 artworks 链接时正确提取`() {
        val json = """
        {
          "header": { "status": 0 },
          "results": [
            {
              "header": {
                "similarity": "90.00",
                "thumbnail": "https://img1.saucenao.com/thumb.jpg"
              },
              "data": {
                "ext_urls": [
                  "https://www.pixiv.net/artworks/55667788"
                ],
                "title": "通过ext_urls提取作品",
                "author": "画师C"
              }
            }
          ]
        }
        """.trimIndent()

        val results = SauceNaoParser.parse(json)
        assertEquals(1, results.size)
        assertEquals(55667788L, results[0].illustId)
        assertEquals("通过ext_urls提取作品", results[0].title)
        assertEquals("画师C", results[0].author)
    }

    @Test
    fun `过滤非 Pixiv 来源并对重复结果去重`() {
        val json = """
        {
          "header": { "status": 0 },
          "results": [
            {
              "header": { "similarity": "95.00" },
              "data": {
                "ext_urls": [ "https://danbooru.donmai.us/posts/123" ],
                "danbooru_id": 123
              }
            },
            {
              "header": { "similarity": "92.00" },
              "data": {
                "pixiv_id": 11112222,
                "title": "作品1"
              }
            },
            {
              "header": { "similarity": "80.00" },
              "data": {
                "pixiv_id": 11112222,
                "title": "重复作品1"
              }
            }
          ]
        }
        """.trimIndent()

        val results = SauceNaoParser.parse(json)
        assertEquals(1, results.size)
        assertEquals(11112222L, results[0].illustId)
        assertEquals("92.00", results[0].similarity)
    }

    @Test
    fun `解析 HTML 兜底响应并提取 Pixiv 作品`() {
        val html = """
        <!DOCTYPE html>
        <html>
        <body>
          <div class="result">
            <div class="resultimage"><img src="https://img1.saucenao.com/t/test.jpg" /></div>
            <div class="resultsimilarityinfo">96.5%</div>
            <div class="resulttitle">HTML标题</div>
            <div class="resultcontent">
              <a href="https://www.pixiv.net/artworks/99887766">Pixiv Link</a>
              <a href="https://www.pixiv.net/users/1234">画师X</a>
            </div>
          </div>
        </body>
        </html>
        """.trimIndent()

        val results = SauceNaoParser.parse(html)
        assertEquals(1, results.size)
        assertEquals(99887766L, results[0].illustId)
        assertEquals("96.5%", results[0].similarity)
        assertEquals("https://img1.saucenao.com/t/test.jpg", results[0].thumbnail)
    }


    @Test
    fun `解析真实 SauceNAO 表格版 HTML 并提取元数据`() {
        // 2026-10 实测 SauceNAO 网页端结构：每个结果为 <div class="result"><table>…</table></div>，
        // 标题位于 <strong> 内，pixiv 链接为 member_illust.php?mode=medium&illust_id=…。
        val html = """
        <div class="result">
        <table class="resulttable">
        <tr>
        <td class="resulttableimage">
        <div class="resultimage" >
        <a href="https://saucenao.com/search.php?db=999&url=https%3A%2F%2Fimg1.saucenao.com%2Fres%2Fpixiv%2F4797%2Fmanga%2F47976543_p13.jpg" class="linkify">
        <img id="resImage0" title="Index #5: Pixiv Images - 47976543_p13.jpg" src="https://img1.saucenao.com/res/pixiv/4797/manga/47976543_p13.jpg" border="0"/>
        </a>
        </div>
        </td>
        <td class="resulttablecontent">
        <div class="resultmatchinfo">
        <div class="resultsimilarityinfo">54.52%</div>
        <div class="resultmiscinfo">
        </div>
        </div>
        <div class="resultcontent">
        <div class="resulttitle">
        <strong>ツイッターまとめ</strong>
        <br />
        </div>
        <div class="resultcontentcolumn">
        <strong>pixiv ID: </strong>
        <a href="https://www.pixiv.net/member_illust.php?mode=medium&illust_id=47976543" class="linkify">47976543</a>
        <br />
        <strong>Member: </strong>
        <a href="https://www.pixiv.net/member.php?id=2230180" class="linkify">ふでまる</a>
        </div>
        </div>
        </td>
        </tr>
        </table>
        </div>
        <div class="result">
        <table class="resulttable">
        <tr>
        <td class="resulttableimage">
        <div class="resultimage" >
        <a href="https://www.furaffinity.net/view/32572892"><img id="resImage1" src="https://img3.saucenao.com/furaffinity/3257/32572892.jpg"/></a>
        </div>
        </td>
        <td class="resulttablecontent">
        <div class="resultmatchinfo">
        <div class="resultsimilarityinfo">51.00%</div>
        </div>
        </td>
        </tr>
        </table>
        </div>
        """.trimIndent()

        val results = SauceNaoParser.parse(html)
        assertEquals(1, results.size)
        assertEquals(47976543L, results[0].illustId)
        assertEquals("54.52%", results[0].similarity)
        assertEquals("ツイッターまとめ", results[0].title)
        assertEquals("ふでまる", results[0].author)
        assertEquals("https://img1.saucenao.com/res/pixiv/4797/manga/47976543_p13.jpg", results[0].thumbnail)
    }
    @Test
    fun `空响应或格式损坏时返回空列表而非抛出异常`() {
        assertTrue(SauceNaoParser.parse("").isEmpty())
        assertTrue(SauceNaoParser.parse("   ").isEmpty())
        assertTrue(SauceNaoParser.parse("{ invalid json }").isEmpty())
        assertTrue(SauceNaoParser.parse("<html><body>No pixiv results here</body></html>").isEmpty())
    }

    @Test
    fun `JSON 缩略图非 HTTPS 携带凭据或 host 不受信时置空`() {
        val json = """
        {
          "header": { "status": 0 },
          "results": [
            {
              "header": { "similarity": "90.0", "thumbnail": "http://img1.saucenao.com/t/a.jpg" },
              "data": { "pixiv_id": 101, "title": "A" }
            },
            {
              "header": { "similarity": "80.0", "thumbnail": "https://user:pass@img1.saucenao.com/b.jpg" },
              "data": { "pixiv_id": 102, "title": "B" }
            },
            {
              "header": { "similarity": "70.0", "thumbnail": "https://evil.example.com/c.jpg" },
              "data": { "pixiv_id": 103, "title": "C" }
            },
            {
              "header": { "similarity": "60.0", "thumbnail": "https://img1.saucenao.com/t/ok.jpg" },
              "data": { "pixiv_id": 104, "title": "D" }
            },
            {
              "header": { "similarity": "50.0", "thumbnail": "https://saucenao.com.evil.io/e.jpg" },
              "data": { "pixiv_id": 105, "title": "E" }
            }
          ]
        }
        """.trimIndent()

        val results = SauceNaoParser.parse(json)
        assertEquals(5, results.size)
        assertNull(results[0].thumbnail, "非 HTTPS 缩略图应置空回退占位")
        assertNull(results[1].thumbnail, "携带 userinfo 的缩略图应置空回退占位")
        assertNull(results[2].thumbnail, "host 不受信的缩略图应置空回退占位")
        assertEquals("https://img1.saucenao.com/t/ok.jpg", results[3].thumbnail)
        assertNull(results[4].thumbnail, "后缀仿冒 host 应置空回退占位")
    }

    @Test
    fun `HTML 缩略图非 HTTPS 时置空`() {
        val html = """
        <div class="result">
          <div class="resultimage"><img src="http://img1.saucenao.com/t/test.jpg" /></div>
          <div class="resultsimilarityinfo">96.5%</div>
          <div class="resulttitle">HTML标题</div>
          <div class="resultcontent">
            <a href="https://www.pixiv.net/artworks/99887766">Pixiv Link</a>
          </div>
        </div>
        """.trimIndent()

        val results = SauceNaoParser.parse(html)
        assertEquals(1, results.size)
        assertEquals(99887766L, results[0].illustId)
        assertNull(results[0].thumbnail, "非 HTTPS 缩略图应置空回退占位")
    }

    @Test
    fun `safeThumbnailUrl 放行 saucenao 与 pximg 白名单 HTTPS 地址`() {
        assertEquals(
            "https://img1.saucenao.com/t/test.jpg",
            SauceNaoParser.safeThumbnailUrl("https://img1.saucenao.com/t/test.jpg"),
        )
        assertEquals(
            "https://saucenao.com/thumbnails/a.jpg",
            SauceNaoParser.safeThumbnailUrl("https://saucenao.com/thumbnails/a.jpg"),
        )
        assertEquals(
            "https://i.pximg.net/img-master/img/a.jpg",
            SauceNaoParser.safeThumbnailUrl("https://i.pximg.net/img-master/img/a.jpg"),
        )
        assertEquals(
            "https://s.pximg.net/a.png",
            SauceNaoParser.safeThumbnailUrl("  https://s.pximg.net/a.png  "),
        )
        assertNull(SauceNaoParser.safeThumbnailUrl(null))
        assertNull(SauceNaoParser.safeThumbnailUrl("   "))
    }

    @Test
    fun `safeThumbnailUrl 拒绝非 HTTPS 凭据与非白名单 host`() {
        assertNull(SauceNaoParser.safeThumbnailUrl("http://img1.saucenao.com/t/a.jpg"))
        assertNull(SauceNaoParser.safeThumbnailUrl("https://user:pass@saucenao.com/a.jpg"))
        assertNull(SauceNaoParser.safeThumbnailUrl("https://user@i.pximg.net/a.jpg"))
        assertNull(SauceNaoParser.safeThumbnailUrl("https://evil.example.com/a.jpg"))
        assertNull(SauceNaoParser.safeThumbnailUrl("https://saucenao.com.evil.io/a.jpg"))
        assertNull(SauceNaoParser.safeThumbnailUrl("https://img1.evil-saucenao.com/a.jpg"))
        assertNull(SauceNaoParser.safeThumbnailUrl("javascript:alert(1)"))
    }

    /** 捕获 Napier W 级日志的测试桩：仅第三方日志通道打桩，不涉及自身逻辑。 */
    private class CapturingAntilog : Antilog() {
        val warnings = mutableListOf<Pair<String?, Throwable?>>()

        override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
            if (priority == LogLevel.WARNING) warnings += message to throwable
        }
    }

    @Test
    fun `JSON 解析失败降级 HTML 时输出 Napier 警告并携带原始异常`() {
        val antilog = CapturingAntilog()
        Napier.base(antilog)
        try {
            assertTrue(SauceNaoParser.parse("{ invalid json }").isEmpty())

            val warn = antilog.warnings.firstOrNull()
            assertTrue(warn != null, "JSON 解析失败应输出 Napier warn 日志，实际记录=${antilog.warnings}")
            assertTrue(warn.second != null, "warn 日志应携带原始解析异常")
        } finally {
            Napier.takeLogarithm()
        }
    }

    @Test
    fun `合法 JSON 正常解析不输出降级警告`() {
        val antilog = CapturingAntilog()
        Napier.base(antilog)
        try {
            val json = """
            {
              "header": { "status": 0 },
              "results": [
                {
                  "header": { "similarity": "95.5" },
                  "data": { "pixiv_id": 12345678, "title": "正常作品" }
                }
              ]
            }
            """.trimIndent()

            val results = SauceNaoParser.parse(json)
            assertTrue(results.isNotEmpty())
            assertTrue(antilog.warnings.isEmpty(), "正常解析不应产生 warn 日志：${antilog.warnings}")
        } finally {
            Napier.takeLogarithm()
        }
    }
}