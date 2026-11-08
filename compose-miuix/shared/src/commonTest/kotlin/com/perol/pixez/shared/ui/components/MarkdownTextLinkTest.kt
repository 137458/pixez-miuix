package com.perol.pixez.shared.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownTextLinkTest {

    @Test
    fun `链接产出可点击的 URL 注解且保留原有文本`() {
        val annotated = buildAnnotatedContent("请看 [更新说明](https://example.com/releases) 末尾", Color.Unspecified)

        assertEquals("请看 更新说明 末尾", annotated.text)
        val links = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals(1, links.size)
        assertEquals("https://example.com/releases", (links.single().item as LinkAnnotation.Url).url)
    }

    @Test
    fun `粗体与内联代码不产生链接注解`() {
        val annotated = buildAnnotatedContent("**粗体** 与 `code`", Color.Unspecified)

        assertEquals("粗体 与 code", annotated.text)
        assertTrue(annotated.getLinkAnnotations(0, annotated.length).isEmpty())
    }
}
