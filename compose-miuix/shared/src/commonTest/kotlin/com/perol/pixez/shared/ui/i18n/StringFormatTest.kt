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

    // IB-4 回归：format 改为按 token 位置一次重建，参数不得再被正则替换语义解释。
    @Test
    fun testStringFormatArgumentWithDollarIsLiteral() {
        assertEquals("Cost \$5", "Cost %s".format("\$5"))
        assertEquals("\$1\$2", "%1\$s".format("\$1\$2"))
        assertEquals("\$\$\$", "%s".format("\$\$\$"))
        assertEquals("\$&", "%1\$s".format("\$&"))
    }

    @Test
    fun testStringFormatArgumentWithBackslashIsLiteral() {
        assertEquals("C:\\Temp\\pixez", "%s".format("C:\\Temp\\pixez"))
        assertEquals("\\d+", "%1\$s".format("\\d+"))
        assertEquals("\\", "%s".format("\\"))
    }

    @Test
    fun testStringFormatLiteralPercent() {
        // %% 是百分号字面量，不消耗参数
        assertEquals("100% 完成", "100%% 完成".format("未被使用"))
        assertEquals("占比 50%", "占比 %s%%".format(50))
        assertEquals("写入 %s 不替换", "写入 %%s 不替换".format("ignored"))
        assertEquals("纯文本 100%", "纯文本 100%".format("ignored"))
    }

    @Test
    fun testStringFormatMissingArgumentsKeepPlaceholders() {
        assertEquals("a 与 %s", "%s 与 %s".format("a"))
        assertEquals("only %2\$s", "%1\$s %2\$s".format("only"))
        assertEquals("%2\$s", "%2\$s".format("only"))
    }

    @Test
    fun testStringFormatExtraArgumentsAreIgnored() {
        assertEquals("只有一个 a", "只有一个 %s".format("a", "b", "c"))
        assertEquals("x", "x".format("a", "b"))
    }

    // 存储占用展示：GB 两位小数（由调用方传 gbDecimals = 2），小于 1KB 按字节展示。
    @Test
    fun testFormatFileSizeWithTwoGbDecimals() {
        assertEquals("0 B", formatFileSize(0L, gbDecimals = 2))
        assertEquals("0 B", formatFileSize(-5L, gbDecimals = 2))
        assertEquals("512 B", formatFileSize(512L, gbDecimals = 2))
        assertEquals("1.0 KB", formatFileSize(1024L, gbDecimals = 2))
        assertEquals("1.5 KB", formatFileSize(1536L, gbDecimals = 2))
        assertEquals("1.0 MB", formatFileSize(1024L * 1024L, gbDecimals = 2))
        assertEquals("1.0 GB", formatFileSize(1024L * 1024L * 1024L, gbDecimals = 2))
        assertEquals("3.45 GB", formatFileSize((1024L * 1024L * 1024L * 3.456).toLong(), gbDecimals = 2))
    }

    // 更新包体积展示：GB 一位小数（默认）。
    @Test
    fun testFormatFileSizeDefaultsToOneGbDecimal() {
        assertEquals("1.0 GB", formatFileSize(1024L * 1024L * 1024L))
        assertEquals("3.4 GB", formatFileSize((1024L * 1024L * 1024L * 3.456).toLong()))
        assertEquals("12.5 MB", formatFileSize(12L * 1024L * 1024L + 512L * 1024L))
    }
}
