package com.perol.pixez.shared.platform

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FileNamePolicyTest {

    @Test
    fun `requireSafeBaseName accepts valid file names`() {
        assertEquals("12345_p0.jpg", FileNamePolicy.requireSafeBaseName("12345_p0.jpg"))
        assertEquals("插画_タイトル.png", FileNamePolicy.requireSafeBaseName("插画_タイトル.png"))
        assertEquals("name with spaces.gif", FileNamePolicy.requireSafeBaseName("  name with spaces.gif  "))
    }

    @Test
    fun `requireSafeBaseName rejects invalid and dangerous names`() {
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("   ") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName(".") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("..") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("../hack.jpg") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("folder/file.png") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("folder\\file.png") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("file\u0000.png") }
        assertFailsWith<IllegalArgumentException> { FileNamePolicy.requireSafeBaseName("file\r\n.png") }
    }

    @Test
    fun `sanitizeSegment replaces forbidden chars and controls with underscore`() {
        assertEquals("a_b_c_d_e_f_g_h_i_", FileNamePolicy.sanitizeSegment("a/b\\c:d*e?f\"g<h>i|"))
        assertEquals("line1__line2_tab", FileNamePolicy.sanitizeSegment("line1\r\nline2\ttab"))
        assertEquals("safe_name", FileNamePolicy.sanitizeSegment("safe_name"))
        assertEquals("画师名_12345", FileNamePolicy.sanitizeSegment("画师名/12345"))
    }

    @Test
    fun `sanitizeSegment handles empty or dot inputs with default name`() {
        assertEquals("untitled", FileNamePolicy.sanitizeSegment(""))
        assertEquals("untitled", FileNamePolicy.sanitizeSegment("   "))
        assertEquals("untitled", FileNamePolicy.sanitizeSegment("."))
        assertEquals("untitled", FileNamePolicy.sanitizeSegment(".."))
    }

    @Test
    fun `sanitizeSegment strips DEL and C1 control characters`() {
        assertEquals("file_.png", FileNamePolicy.sanitizeSegment("file\u007f.png"))
        assertEquals("a_b", FileNamePolicy.sanitizeSegment("a\u0085b"))
        assertEquals("a_b", FileNamePolicy.sanitizeSegment("a\u009fb"))
    }

    @Test
    fun `sanitizeSegment output always passes requireSafeBaseName`() {
        val samples = listOf(
            "a..b",
            "test..title",
            "...",
            "..hidden",
            "a\u007fb",
            "x\u009fy",
            "normal.jpg",
            "a/b\\c:d*e?f\"g<h>i|",
            "line1\r\nline2\ttab",
        )
        for (sample in samples) {
            val cleaned = FileNamePolicy.sanitizeSegment(sample)
            val accepted = runCatching { FileNamePolicy.requireSafeBaseName(cleaned) }.isSuccess
            assertTrue(accepted, "sanitizeSegment(\"$sample\") 得到 \"$cleaned\"，未通过 requireSafeBaseName")
        }
    }
}
