package com.perol.pixez.shared.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 架构审查 UI-12：小说正文 → 渲染条目的纯计算契约。
 *
 * 三类数据锁住行为：标记展开（每个标记一个条目）、超长段落按 10 行分块、空文本不产生条目。
 */
class NovelViewerItemsTest {

    @Test
    fun emptyTextProducesNoItems() {
        assertTrue(buildNovelViewerItems("").isEmpty())
    }

    @Test
    fun shortTextIsSingleChunk() {
        val items = buildNovelViewerItems("第一行\n第二行")
        assertEquals(listOf(NovelViewerItem.TextChunk("第一行\n第二行")), items)
    }

    @Test
    fun exactlyTenLinesStayInOneChunk() {
        val text = (1..10).joinToString("\n") { "L$it" }
        assertEquals(listOf(NovelViewerItem.TextChunk(text)), buildNovelViewerItems(text))
    }

    @Test
    fun longTextIsChunkedByTenLines() {
        val text = (1..25).joinToString("\n") { "L$it" }
        val items = buildNovelViewerItems(text)

        val expected = listOf(
            (1..10).joinToString("\n") { "L$it" },
            (11..20).joinToString("\n") { "L$it" },
            (21..25).joinToString("\n") { "L$it" },
        )
        assertEquals(expected, items.map { (it as NovelViewerItem.TextChunk).text })
    }

    @Test
    fun elevenLinesSplitIntoTwoChunks() {
        val text = (1..11).joinToString("\n") { "L$it" }
        val items = buildNovelViewerItems(text)
        assertEquals(2, items.size)
        assertEquals((1..10).joinToString("\n") { "L$it" }, (items[0] as NovelViewerItem.TextChunk).text)
        assertEquals("L11", (items[1] as NovelViewerItem.TextChunk).text)
    }

    @Test
    fun markersExpandToTheirOwnItems() {
        val items = buildNovelViewerItems(
            "[chapter:序章]" +
                "[newpage]" +
                "[pixivimage:12345]" +
                "[uploadedimage:img-1]" +
                "[[jumpuri:链接 > https://example.com/a]]" +
                "[jump:3]",
        )

        assertEquals(
            listOf(
                NovelViewerItem.Chapter("序章"),
                NovelViewerItem.NewPageSpacer,
                NovelViewerItem.EmbeddedIllust(illustId = 12345L, key = "12345"),
                NovelViewerItem.UploadedPicture("img-1"),
                NovelViewerItem.JumpUriLink(title = "链接", url = "https://example.com/a"),
                NovelViewerItem.JumpRaw("[jump:3]"),
            ),
            items,
        )
    }
}
