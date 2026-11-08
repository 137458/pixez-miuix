package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.Color
import com.perol.pixez.shared.ui.AppConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownTextTest {

    @Test
    fun testDetectChangelogCategory() {
        assertEquals(CategoryKind.NEW, detectChangelogCategory("新增"))
        assertEquals(CategoryKind.NEW, detectChangelogCategory("Features"))
        assertEquals(CategoryKind.NEW, detectChangelogCategory("New"))
        assertEquals(CategoryKind.NEW, detectChangelogCategory("新特性"))

        assertEquals(CategoryKind.OPTIMIZATION, detectChangelogCategory("优化"))
        assertEquals(CategoryKind.OPTIMIZATION, detectChangelogCategory("Improvements"))
        assertEquals(CategoryKind.OPTIMIZATION, detectChangelogCategory("改进"))

        assertEquals(CategoryKind.FIX, detectChangelogCategory("修复"))
        assertEquals(CategoryKind.FIX, detectChangelogCategory("Bug Fixes"))
        assertEquals(CategoryKind.FIX, detectChangelogCategory("Fixes"))

        assertNull(detectChangelogCategory("普通段落标题"))
    }

    @Test
    fun testParseMarkdownBlocks() {
        val sample = """
            # 一级标题

            ## 新增
            - 支持小说搜索与阅读
            - 弹窗视觉升级

            1. 第一步
            2. 第二步

            普通文本段落
            ---
        """.trimIndent()

        val blocks = parseMarkdownBlocks(sample)

        assertTrue(blocks.any { it is MarkdownBlock.Heading && it.level == 1 && it.text == "一级标题" })
        assertTrue(blocks.any { it is MarkdownBlock.Heading && it.level == 2 && it.text == "新增" })
        assertTrue(blocks.any { it is MarkdownBlock.BulletItem && it.text == "支持小说搜索与阅读" })
        assertTrue(blocks.any { it is MarkdownBlock.NumberedItem && it.number == "1" && it.text == "第一步" })
        assertTrue(blocks.any { it is MarkdownBlock.Paragraph && it.text == "普通文本段落" })
        assertTrue(blocks.any { it is MarkdownBlock.Divider })
    }

    @Test
    fun testBuildAnnotatedContent() {
        val raw = "欢迎使用 **PixEz** 客户端，查看 `CHANGELOG.md`。"
        val annotated = buildAnnotatedContent(raw, Color.Unspecified)
        assertEquals("欢迎使用 PixEz 客户端，查看 CHANGELOG.md。", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun testUpdateConstantsGuard() {
        // 保证更新日志卡片最大展示高度充足，解决截断过窄问题
        assertTrue(AppConstants.Update.CHANGELOG_MAX_HEIGHT_DP >= 360)
        assertTrue(AppConstants.Update.CHANGELOG_FADE_EDGE_DP >= 20)
        assertTrue(AppConstants.Update.SCROLLBAR_WIDTH_DP >= 2)
    }
}
