package com.perol.pixez.shared.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * /webview/v2/novel HTML→JSON 提取器测试：
 * 覆盖正常提取、嵌套大括号、字符串内大括号、转义引号、novel 后无对象与无 novel: 场景。
 */
class NovelWebParserTest {

    @Test
    fun `正常 script 提取 novel JSON`() {
        val html = """<html><script>window.__INITIAL_STATE__ = {route:"/novel/1.php",novel:{"id":"42","title":"标题","text":"正文"}};</script></html>"""

        assertEquals(
            """{"id":"42","title":"标题","text":"正文"}""",
            NovelWebParser.extractNovelJsonFromHtml(html),
        )
    }

    @Test
    fun `嵌套大括号按配平完整提取`() {
        val html = """<script>novel:{"a":{"b":{"c":[1,2,{"d":3}]}},"id":"1"};</script>"""

        assertEquals(
            """{"a":{"b":{"c":[1,2,{"d":3}]}},"id":"1"}""",
            NovelWebParser.extractNovelJsonFromHtml(html),
        )
    }

    @Test
    fun `字符串内大括号不参与配平`() {
        val html = """<script>novel:{"text":"abc{def}ghi","n":1}</script>"""

        assertEquals(
            """{"text":"abc{def}ghi","n":1}""",
            NovelWebParser.extractNovelJsonFromHtml(html),
        )
    }

    @Test
    fun `字符串内转义引号与转义反斜杠不破坏配平`() {
        val html = """<script>novel:{"text":"say \"hi\" {x} \\ } ok","n":2}</script>"""

        assertEquals(
            """{"text":"say \"hi\" {x} \\ } ok","n":2}""",
            NovelWebParser.extractNovelJsonFromHtml(html),
        )
    }

    @Test
    fun `novel 后接空白换行也能定位对象起点`() {
        val html = """
            <script>
              novel: {
                "id": "7",
                "illusts": { "10-1": { "illust": { "images": { "medium": "m" } } } }
              }
            </script>
        """.trimIndent()

        val expected = "{\n" +
            "    \"id\": \"7\",\n" +
            "    \"illusts\": { \"10-1\": { \"illust\": { \"images\": { \"medium\": \"m\" } } } }\n" +
            "  }"

        assertEquals(expected, NovelWebParser.extractNovelJsonFromHtml(html))
    }

    @Test
    fun `无 novel 键时返回 null`() {
        val html = """<html><script>window.state={other:{a:1}};</script></html>"""

        assertNull(NovelWebParser.extractNovelJsonFromHtml(html))
    }

    @Test
    fun `novel 值非对象时跳过该次定位`() {
        val html = """<script>novel:null,marker:{};</script><script>novel:{"id":"9"}</script>"""

        assertEquals("""{"id":"9"}""", NovelWebParser.extractNovelJsonFromHtml(html))
    }

    @Test
    fun `仅有非对象 novel 值时返回 null`() {
        val html = """<script>novel:null;</script>"""

        assertNull(NovelWebParser.extractNovelJsonFromHtml(html))
    }

    @Test
    fun `空 HTML 返回 null`() {
        assertNull(NovelWebParser.extractNovelJsonFromHtml(""))
    }
}
