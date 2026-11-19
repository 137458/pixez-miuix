package com.perol.pixez.shared.ui.coui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 评论输入栏纯规则单元测试 [CouiCommentComposer]：
 * 1. 发送门槛与页面原有判断一致（发送中、空白草稿、未登录均不可发）。
 * 2. 表情按光标插入：常规插入、选区替换、越界与逆序选区的钳制。
 */
class CouiCommentComposerTest {

    @Test
    fun `发送门槛要求非空白文本且不在发送中且已登录`() {
        assertTrue(CouiCommentComposer.canSend("你好", isSending = false, isLoggedIn = true))
        assertFalse(CouiCommentComposer.canSend("   ", isSending = false, isLoggedIn = true))
        assertFalse(CouiCommentComposer.canSend("", isSending = false, isLoggedIn = true))
        assertFalse(CouiCommentComposer.canSend("\n\t", isSending = false, isLoggedIn = true))
        assertFalse(CouiCommentComposer.canSend("你好", isSending = true, isLoggedIn = true))
        assertFalse(CouiCommentComposer.canSend("你好", isSending = false, isLoggedIn = false))
        assertFalse(CouiCommentComposer.canSend("你好", isSending = false, isLoggedIn = null))
    }

    @Test
    fun `光标在中间插入表情保留两侧文本并移动光标`() {
        val result = CouiCommentComposer.placeEmoji("ab", selectionStart = 1, selectionEnd = 1, emoji = "\uD83D\uDE00")
        assertEquals("a\uD83D\uDE00b", result.text)
        assertEquals(3, result.cursor)
    }

    @Test
    fun `选中区间时表情替换选区`() {
        val result = CouiCommentComposer.placeEmoji("abc", selectionStart = 0, selectionEnd = 3, emoji = "\uD83D\uDE00")
        assertEquals("\uD83D\uDE00", result.text)
        assertEquals(2, result.cursor)
    }

    @Test
    fun `空草稿插入表情只剩表情且光标在末尾`() {
        val result = CouiCommentComposer.placeEmoji("", selectionStart = 0, selectionEnd = 0, emoji = "\uD83D\uDE00")
        assertEquals("\uD83D\uDE00", result.text)
        assertEquals(2, result.cursor)
    }

    @Test
    fun `越界与逆序选区被钳制到草稿范围内`() {
        val overflow = CouiCommentComposer.placeEmoji("abc", selectionStart = 9, selectionEnd = 9, emoji = "x")
        assertEquals("abcx", overflow.text)
        assertEquals(4, overflow.cursor)

        val reversed = CouiCommentComposer.placeEmoji("abcd", selectionStart = 3, selectionEnd = 1, emoji = "x")
        assertEquals("axd", reversed.text)
        assertEquals(2, reversed.cursor)

        val negative = CouiCommentComposer.placeEmoji("abcd", selectionStart = -3, selectionEnd = 0, emoji = "x")
        assertEquals("xabcd", negative.text)
        assertEquals(1, negative.cursor)
    }
}
