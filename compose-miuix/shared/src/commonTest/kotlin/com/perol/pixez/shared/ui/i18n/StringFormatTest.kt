package com.perol.pixez.shared.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class StringFormatTest {

    @Test
    fun testFormatFileSizeZeroAndNegative() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("0 B", formatFileSize(-1L))
        assertEquals("0 B", formatFileSize(-1024L))
    }

    @Test
    fun testFormatFileSizeBytes() {
        assertEquals("1 B", formatFileSize(1L))
        assertEquals("500 B", formatFileSize(500L))
        assertEquals("1023 B", formatFileSize(1023L))
    }

    @Test
    fun testFormatFileSizeKilobytes() {
        assertEquals("1.0 KB", formatFileSize(1024L))
        assertEquals("1.5 KB", formatFileSize(1536L))
        assertEquals("10.0 KB", formatFileSize(10240L))
        assertEquals("1023.9 KB", formatFileSize(1024L * 1024L - 1L))
    }

    @Test
    fun testFormatFileSizeMegabytes() {
        val oneMb = 1024L * 1024L
        assertEquals("1.0 MB", formatFileSize(oneMb))
        assertEquals("2.5 MB", formatFileSize((oneMb * 2.5).toLong()))
        assertEquals("512.0 MB", formatFileSize(oneMb * 512L))
    }

    @Test
    fun testFormatFileSizeGigabytes() {
        val oneGb = 1024L * 1024L * 1024L
        assertEquals("1.0 GB", formatFileSize(oneGb))
        assertEquals("3.4 GB", formatFileSize((oneGb * 3.4 + 0.5).toLong()))
    }

    @Test
    fun testStringFormatEmptyArgs() {
        assertEquals("plain text", "plain text".format())
    }

    @Test
    fun testStringFormatNoPlaceholders() {
        assertEquals("no placeholders", "no placeholders".format("arg1", 123))
    }

    @Test
    fun testStringFormatSequentialPlaceholders() {
        assertEquals("Hello World", "Hello %s".format("World"))
        assertEquals("Count: 42, Status: ok", "Count: %d, Status: %s".format(42, "ok"))
        assertEquals("Progress: 75.5%", "Progress: %f%".format(75.5))
    }

    @Test
    fun testStringFormatPositionalPlaceholders() {
        assertEquals("second first", "%2\$s %1\$s".format("first", "second"))
        assertEquals("repeat repeat", "%1\$s %1\$s".format("repeat"))
    }

    @Test
    fun testStringFormatWithNull() {
        assertEquals("value is null", "value is %s".format(null))
        assertEquals("pos null", "pos %1\$s".format(null))
    }
}
