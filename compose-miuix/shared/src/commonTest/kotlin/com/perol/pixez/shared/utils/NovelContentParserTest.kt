package com.perol.pixez.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 小说正文内嵌标记解析器测试：
 * 覆盖全部标记类型、ID-页码 变体、混合正文、畸形标记按原始文本兜底与空文本。
 * 用两组以上差异化数据驱动（半角/全角分隔符、带页码/不带页码、合法/畸形）。
 */
class NovelContentParserTest {

    private fun parse(vararg sources: String): List<List<NovelSpan>> =
        sources.map { NovelContentParser.parse(it) }

    @Test
    fun `纯文本无标记解析为单个 Text 段并保留换行`() {
        val result = NovelContentParser.parse("第一段正文\n第二段正文")

        assertEquals(listOf(NovelSpan.Text("第一段正文\n第二段正文")), result)
    }

    @Test
    fun `newpage 标记解析为 NewPage`() {
        val results = parse("[newpage]", "前文\n[newpage]\n后文")

        assertEquals(listOf(NovelSpan.NewPage), results[0])
        assertEquals(
            listOf(
                NovelSpan.Text("前文\n"),
                NovelSpan.NewPage,
                NovelSpan.Text("\n后文"),
            ),
            results[1],
        )
    }

    @Test
    fun `chapter 标记提取标题`() {
        val results = parse("[chapter:第一章 序章]", "[chapter:]")

        assertEquals(listOf(NovelSpan.Chapter("第一章 序章")), results[0])
        assertEquals(listOf(NovelSpan.Chapter("")), results[1], "空标题也按 Chapter 解析")
    }

    @Test
    fun `pixivimage 不带页码时 illustId 生效且 page 为 0`() {
        val result = NovelContentParser.parse("[pixivimage:12345]")

        assertEquals(listOf(NovelSpan.PixivImage(illustId = 12345L, page = 0, key = "12345")), result)
    }

    @Test
    fun `pixivimage 带 ID-页码 变体时正确拆分`() {
        val results = parse("[pixivimage:12345-2]", "[pixivimage:987-0]")

        assertEquals(listOf(NovelSpan.PixivImage(illustId = 12345L, page = 2, key = "12345-2")), results[0])
        assertEquals(listOf(NovelSpan.PixivImage(illustId = 987L, page = 0, key = "987-0")), results[1])
    }

    @Test
    fun `uploadedimage 标记保留字符串 ID`() {
        val results = parse("[uploadedimage:98765]", "[uploadedimage:000123]")

        assertEquals(listOf(NovelSpan.UploadedImage("98765")), results[0])
        assertEquals(listOf(NovelSpan.UploadedImage("000123")), results[1], "前导零 ID 不应经数字转换丢失")
    }

    @Test
    fun `jumpuri 半角分隔符拆出标题与链接`() {
        val result = NovelContentParser.parse("[[jumpuri:角色页 > https://www.pixiv.net/users/123]]")

        assertEquals(
            listOf(NovelSpan.JumpUri(title = "角色页", url = "https://www.pixiv.net/users/123")),
            result,
        )
    }

    @Test
    fun `jumpuri 全角分隔符同样拆分`() {
        val result = NovelContentParser.parse("[[jumpuri:续篇 ＞ https://www.pixiv.net/novel/show.php?id=9]]")

        assertEquals(
            listOf(
                NovelSpan.JumpUri(
                    title = "续篇",
                    url = "https://www.pixiv.net/novel/show.php?id=9",
                ),
            ),
            result,
        )
    }

    @Test
    fun `jumpuri 无链接时按原始文本兜底`() {
        val raw = "[[jumpuri:只有标题]]"

        assertEquals(listOf(NovelSpan.Text(raw)), NovelContentParser.parse(raw))
    }

    @Test
    fun `jump 标记解析页码并保留原文供展示兜底`() {
        val results = parse("[jump:2]", "[jump:10]")

        assertEquals(listOf(NovelSpan.Jump(page = 2, raw = "[jump:2]")), results[0])
        assertEquals(listOf(NovelSpan.Jump(page = 10, raw = "[jump:10]")), results[1])
    }

    @Test
    fun `混合正文按顺序产出各类型 span`() {
        val source = "开头一段\n[newpage]\n[chapter:第二章]\n中段正文\n[uploadedimage:777]\n[pixivimage:12345-1]\n[jump:2]"

        val result = NovelContentParser.parse(source)

        assertEquals(
            listOf(
                NovelSpan.Text("开头一段\n"),
                NovelSpan.NewPage,
                NovelSpan.Text("\n"),
                NovelSpan.Chapter("第二章"),
                NovelSpan.Text("\n中段正文\n"),
                NovelSpan.UploadedImage("777"),
                NovelSpan.Text("\n"),
                NovelSpan.PixivImage(12345L, 1, "12345-1"),
                NovelSpan.Text("\n"),
                NovelSpan.Jump(2, "[jump:2]"),
            ),
            result,
        )
    }

    @Test
    fun `畸形标记按原始文本兜底`() {
        val results = parse(
            "[pixivimage:abc]",
            "[pixivimage:12-x]",
            "[pixivimage:]",
            "[chapter未闭合",
            "[[rb:漢字＞かな]]",
            "[jump:abc]",
            "[unknown:tag]",
        )

        assertEquals(listOf(NovelSpan.Text("[pixivimage:abc]")), results[0])
        assertEquals(listOf(NovelSpan.Text("[pixivimage:12-x]")), results[1])
        assertEquals(listOf(NovelSpan.Text("[pixivimage:]")), results[2])
        assertEquals(listOf(NovelSpan.Text("[chapter未闭合")), results[3])
        assertEquals(listOf(NovelSpan.Text("[[rb:漢字＞かな]]")), results[4], "rb 按普通文本兜底")
        assertEquals(listOf(NovelSpan.Text("[jump:abc]")), results[5])
        assertEquals(listOf(NovelSpan.Text("[unknown:tag]")), results[6])
    }

    @Test
    fun `未闭合的标记残余文本不被吞掉`() {
        val result = NovelContentParser.parse("正文 [pixivimage:12345 后续")

        assertEquals(
            listOf(
                NovelSpan.Text("正文 "),
                NovelSpan.Text("[pixivimage:12345 后续"),
            ),
            result,
            "畸形未闭合标记应按原文兜底，不得丢失内容",
        )
    }

    @Test
    fun `空文本解析为空列表`() {
        assertTrue(NovelContentParser.parse("").isEmpty())
    }

    @Test
    fun `方括号不配对时收敛为文本且不误吞正文`() {
        val results = parse("a]b[[c]d", "a[[c]]d")

        assertEquals(
            listOf(NovelSpan.Text("a]"), NovelSpan.Text("b"), NovelSpan.Text("[[c]d")),
            results[0],
            "残缺双层括号保持原样兜底",
        )
        assertEquals(
            listOf(NovelSpan.Text("a"), NovelSpan.Text("[[c]]"), NovelSpan.Text("d")),
            results[1],
            "完整双层括号按整段未知标记兜底",
        )
    }
}
