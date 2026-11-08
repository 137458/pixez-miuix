package com.perol.pixez.shared.network

import com.perol.pixez.shared.platform.DigestCheck
import com.perol.pixez.shared.platform.UpdateDigest
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 安装包 SHA-256 校验测试（N-5）：
 * GitHub release API 返回 digest 字段（"sha256:<hex>"），下载完成后流式计算文件
 * 摘要与期望值比对，防止截断/被替换的安装包进入安装环节。
 */
class Sha256FileDigestTest {

    private fun tempFile(content: ByteArray): File =
        File.createTempFile("digest-test", ".bin").apply { writeBytes(content) }

    @Test
    fun `sha256File 计算已知向量`() {
        // sha256("hello") 的标准值
        val file = tempFile("hello".encodeToByteArray())
        val hex = sha256File(file)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hex)
        file.delete()
    }

    @Test
    fun `sha256File 空文件返回标准空摘要`() {
        val file = tempFile(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256File(file),
        )
        file.delete()
    }

    @Test
    fun `sha256File 对大文件分块计算结果与一次性一致`() {
        val content = ByteArray(200 * 1024) { (it % 251).toByte() }
        val file = tempFile(content)
        val streamed = sha256File(file)
        val expected = sha256(content).toHexString()
        assertEquals(expected, streamed, "分块摘要应与一次性摘要一致")
        file.delete()
    }

    @Test
    fun `UpdateDigest 解析 sha256 前缀并忽略大小写`() {
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            UpdateDigest.expectedSha256(
                "sha256:2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824",
            ),
        )
        assertNull(UpdateDigest.expectedSha256("md5:abcdef"), "非 sha256 前缀不支持")
        assertNull(UpdateDigest.expectedSha256(null))
        assertNull(UpdateDigest.expectedSha256("sha256:not-a-hex"), "非法 hex 视为无摘要")
    }

    @Test
    fun `UpdateDigest 三态校验区分无摘要与不一致`() {
        val expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        assertEquals(DigestCheck.Matched, UpdateDigest.verify(expected, expected.uppercase()))
        assertTrue(
            UpdateDigest.verify(expected, "0".repeat(64)) is DigestCheck.Mismatched,
            "摘要不一致必须判失败",
        )
        assertEquals(DigestCheck.Skipped, UpdateDigest.verify(null, null), "无摘要时跳过校验")
        assertTrue(
            UpdateDigest.verify(expected, null) is DigestCheck.Mismatched,
            "拿到摘要却算不出本地摘要不得放行",
        )
    }
}
