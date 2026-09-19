package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.components.MarkdownBlock
import com.perol.pixez.shared.ui.components.buildAnnotatedContent
import com.perol.pixez.shared.ui.components.parseMarkdownBlocks
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        assertTrue(hasNewVersion("v1.0.0", "0.9.109.0-miuix"))
        assertTrue(hasNewVersion("0.9.110.0-miuix", "0.9.109.0-miuix"))
        assertTrue(hasNewVersion("0.10.0", "0.9.109.0-miuix"))
        assertFalse(hasNewVersion("0.9.109.0-miuix", "0.9.109.0-miuix"))
        assertFalse(hasNewVersion("v0.9.109.0-miuix", "0.9.109.0-miuix"))
        assertFalse(hasNewVersion("0.9.108.0-miuix", "0.9.109.0-miuix"))
        assertFalse(hasNewVersion("0.8.0", "0.9.109.0-miuix"))
        assertFalse(hasNewVersion("", "0.9.109.0-miuix"))
    }

    @Test
    fun testGetLocalReleaseInfo() {
        val info = getLocalReleaseInfo()
        assertEquals("v${AppInfo.VERSION_NAME}", info.tagName)
        assertEquals(AppInfo.VERSION_NAME, info.versionName)
        assertFalse(info.isNew)
        assertEquals(AppInfo.CURRENT_CHANGELOG, info.changelog)
        assertTrue(info.changelog.isNotBlank())
        assertTrue(info.title.contains(AppInfo.VERSION_NAME))
    }

    @Test
    fun testParseMarkdownBlocks() {
        val md = """
            # Heading 1
            ## Heading 2
            ### Heading 3
            #### Heading 4
            ---
            - Bullet 1
            * Bullet 2
            + Bullet 3
            1. Numbered 1
            2. Numbered 2
            Regular paragraph text
        """.trimIndent()

        val blocks = parseMarkdownBlocks(md)
        assertEquals(11, blocks.size)
        assertEquals(MarkdownBlock.Heading(1, "Heading 1"), blocks[0])
        assertEquals(MarkdownBlock.Heading(2, "Heading 2"), blocks[1])
        assertEquals(MarkdownBlock.Heading(3, "Heading 3"), blocks[2])
        assertEquals(MarkdownBlock.Heading(4, "Heading 4"), blocks[3])
        assertEquals(MarkdownBlock.Divider, blocks[4])
        assertEquals(MarkdownBlock.BulletItem("Bullet 1"), blocks[5])
        assertEquals(MarkdownBlock.BulletItem("Bullet 2"), blocks[6])
        assertEquals(MarkdownBlock.BulletItem("Bullet 3"), blocks[7])
        assertEquals(MarkdownBlock.NumberedItem("1", "Numbered 1"), blocks[8])
        assertEquals(MarkdownBlock.NumberedItem("2", "Numbered 2"), blocks[9])
        assertEquals(MarkdownBlock.Paragraph("Regular paragraph text"), blocks[10])
    }

    @Test
    fun testParseEmptyMarkdown() {
        val emptyBlocks = parseMarkdownBlocks("")
        assertTrue(emptyBlocks.isEmpty())

        val whitespaceBlocks = parseMarkdownBlocks("   \n\n   \n")
        assertTrue(whitespaceBlocks.isEmpty())
    }

    @Test
    fun testParseCurrentChangelogProducesValidBlocks() {
        val blocks = parseMarkdownBlocks(AppInfo.CURRENT_CHANGELOG)
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.any { it is MarkdownBlock.BulletItem })
        assertTrue(blocks.any { it is MarkdownBlock.Heading })
    }

    @Test
    fun testBuildAnnotatedContentFormatting() {
        val text = "Support **bold** and `code` and [link](https://github.com)"
        val annotated = buildAnnotatedContent(text)
        assertEquals("Support bold and code and link", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
