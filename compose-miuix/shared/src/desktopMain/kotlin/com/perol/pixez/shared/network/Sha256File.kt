package com.perol.pixez.shared.network

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

private const val STREAM_BUFFER_SIZE = 64 * 1024

/**
 * 流式计算文件 SHA-256（64KB 分块，支持大安装包），返回小写 hex 或 null（IO 失败）。
 */
fun sha256File(file: File): String? = runCatching {
    MessageDigest.getInstance("SHA-256").run {
        FileInputStream(file).use { input ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                update(buffer, 0, read)
            }
        }
        digest().joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}.getOrNull()

/**
 * 从 GitHub release 的 digest 字段（"sha256:<hex>"）提取小写 hex；非 sha256 前缀或空值返回 null。
 */
fun expectedDigestHex(digest: String?): String? =
    digest
        ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.length == 64 && it.all { c -> c.isDigit() || c in 'a'..'f' } }

/**
 * 校验 [file] 的 SHA-256 是否与 [expectedDigest]（"sha256:<hex>"）一致。
 * [expectedDigest] 为 null（GitHub 未提供）时返回 false，调用方应视为"无法校验"并自行决策。
 */
fun verifyFileDigest(file: File, expectedDigest: String?): Boolean {
    val expected = expectedDigestHex(expectedDigest) ?: return false
    val actual = sha256File(file) ?: return false
    return actual == expected
}
