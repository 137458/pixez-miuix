package com.perol.pixez.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixivEmojisTest {

    @Test
    fun testAllEmojisCount() {
        assertEquals(38, PixivEmojis.allEmojis.size)
        assertTrue(PixivEmojis.emojiMap.containsKey("(normal)"))
        assertTrue(PixivEmojis.emojiMap.containsKey("(heart)"))
        assertTrue(PixivEmojis.emojiMap.containsKey("(101)"))
    }

    @Test
    fun testParsePlainText() {
        val input = "这是一条纯文本评论"
        val (annotatedString, inlineContent) = PixivEmojis.parseEmojiAnnotatedString(input)
        assertEquals("这是一条纯文本评论", annotatedString.text)
        assertTrue(inlineContent.isEmpty())
    }

    @Test
    fun testParseWithSingleEmoji() {
        val input = "太赞了(smile3)"
        val (annotatedString, inlineContent) = PixivEmojis.parseEmojiAnnotatedString(input)
        assertEquals("太赞了(smile3)", annotatedString.text)
        assertEquals(1, inlineContent.size)
        assertTrue(inlineContent.containsKey("(smile3)"))
    }

    @Test
    fun testParseWithMultipleEmojisAndAliases() {
        val input = "(normal) 好看！ (heart) 支持 (101)"
        val (annotatedString, inlineContent) = PixivEmojis.parseEmojiAnnotatedString(input)
        assertEquals("(normal) 好看！ (heart) 支持 (101)", annotatedString.text)
        assertEquals(3, inlineContent.size)
        assertTrue(inlineContent.containsKey("(normal)"))
        assertTrue(inlineContent.containsKey("(heart)"))
        assertTrue(inlineContent.containsKey("(101)"))
    }

    @Test
    fun testParseWithInvalidAndEdgeCaseParentheses() {
        val input = "hello (unknown_tag) () (normal"
        val (annotatedString, inlineContent) = PixivEmojis.parseEmojiAnnotatedString(input)
        assertEquals("hello (unknown_tag) () (normal", annotatedString.text)
        assertTrue(inlineContent.isEmpty())
    }
}
