package com.perol.pixez.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlUtilsTest {

    @Test
    fun testDecodeNamedEntities() {
        val input = "&amp; &lt; &gt; &quot; &#39; &apos; &#039; &#x27; &#x2F; &nbsp; &copy; &trade;"
        val expected = "& < > \" ' ' ' ' /   © ™"
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun testDecodeNumericEntitiesDecimalAndHex() {
        // &#65; -> A, &#x42; -> B, &#128512; (0x1F600) -> 😀
        val input = "&#65;&#x42;&#128512;&#x1F600;"
        val expected = "AB😀😀"
        assertEquals(expected, HtmlUtils.decodeHtmlEntities(input))
    }

    @Test
    fun testStripHtmlTags() {
        val input = "<p class=\"intro\">Hello <b>World</b>! <a href=\"#\">Click</a></p>"
        val expected = "Hello World! Click"
        assertEquals(expected, HtmlUtils.stripHtmlTags(input))
    }

    @Test
    fun testDecodeUrlPercent() {
        assertEquals("https://twitter.com/artist", HtmlUtils.decodeUrlPercent("https%3A%2F%2Ftwitter.com%2Fartist"))
        assertEquals("你好 世界", HtmlUtils.decodeUrlPercent("%E4%BD%A0%E5%A5%BD+%E4%B8%96%E7%95%8C"))
    }
}
