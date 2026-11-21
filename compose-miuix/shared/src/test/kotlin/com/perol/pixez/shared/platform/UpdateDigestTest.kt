package com.perol.pixez.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class UpdateDigestTest {

    private val validHex = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

    @Test
    fun `解析 sha256 前缀并忽略前缀大小写`() {
        assertEquals(validHex, UpdateDigest.expectedSha256("sha256:$validHex"))
        assertEquals(validHex, UpdateDigest.expectedSha256("SHA256:${validHex.uppercase()}"))
        assertEquals(validHex, UpdateDigest.expectedSha256("  sha256:$validHex  "))
    }

    @Test
    fun `非 sha256 或格式非法时视为没有摘要`() {
        assertNull(UpdateDigest.expectedSha256(null))
        assertNull(UpdateDigest.expectedSha256(""))
        assertNull(UpdateDigest.expectedSha256("md5:abcdef"))
        assertNull(UpdateDigest.expectedSha256("sha256:${validHex.take(63)}"))
        assertNull(UpdateDigest.expectedSha256("sha256:${validHex}z"))
    }

    @Test
    fun `没有期望摘要时跳过校验`() {
        assertSame(DigestCheck.Skipped, UpdateDigest.verify(null, validHex))
        assertSame(DigestCheck.Skipped, UpdateDigest.verify(null, null))
    }

    @Test
    fun `摘要一致时通过`() {
        assertSame(DigestCheck.Matched, UpdateDigest.verify(validHex, validHex.uppercase()))
    }

    @Test
    fun `摘要不一致或本地无法计算时判为不匹配`() {
        val mismatch = UpdateDigest.verify(validHex, "b".repeat(64))
        assertEquals(DigestCheck.Mismatched(expected = validHex, actual = "b".repeat(64)), mismatch)

        val unreadable = UpdateDigest.verify(validHex, null)
        assertEquals(DigestCheck.Mismatched(expected = validHex, actual = null), unreadable)
    }
}
