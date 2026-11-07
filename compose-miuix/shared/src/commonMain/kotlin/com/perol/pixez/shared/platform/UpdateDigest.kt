package com.perol.pixez.shared.platform

/**
 * 安装包摘要校验结论。
 *
 * 三态而不是布尔：没有摘要与摘要不匹配必须区分开。前者是发布流程没提供 digest，
 * 只跳过校验、不能拦下载；后者是内容与发布信息不一致，必须删包并报错。
 */
sealed interface DigestCheck {

    /** 远端没有可用 SHA-256 摘要，跳过校验。 */
    data object Skipped : DigestCheck

    /** 摘要一致。 */
    data object Matched : DigestCheck

    /** 摘要不一致；[actual] 为 null 表示本地计算失败，同样不得放行。 */
    data class Mismatched(val expected: String, val actual: String?) : DigestCheck
}

/**
 * 安装包 SHA-256 摘要解析与校验。
 *
 * 只认 GitHub `digest` 字段里的 `sha256:<64 hex>`（前缀大小写不敏感）。GitHub 也可能下发
 * 其它算法，但不认识的算法一律当作“没有摘要”跳过，而不是按错误的算法误判失败。
 * 平台相关的文件流式计算由 androidMain / desktopMain / iosMain 的 actual 提供。
 */
object UpdateDigest {

    private const val SHA256_PREFIX = "sha256:"
    private val Sha256Hex = Regex("[0-9a-fA-F]{64}")

    /** 解析 `sha256:<64 位 hex>`，返回小写 hex；缺失、其它算法或格式非法时返回 null。 */
    fun expectedSha256(digest: String?): String? {
        val raw = digest?.trim().orEmpty()
        if (!raw.startsWith(SHA256_PREFIX, ignoreCase = true)) return null
        return raw.substringAfter(':').trim()
            .takeIf { Sha256Hex.matches(it) }
            ?.lowercase()
    }

    /**
     * 按已解析的期望摘要校验实际值。
     *
     * [expectedSha256] 为 null 时三态为 [DigestCheck.Skipped]；实际值缺失或与期望不一致均为
     * [DigestCheck.Mismatched]——拿到了摘要却算不出本地摘要，同样不能放行。
     */
    fun verify(expectedSha256: String?, actualSha256: String?): DigestCheck {
        if (expectedSha256 == null) return DigestCheck.Skipped
        val actual = actualSha256?.trim()?.lowercase()
        return if (actual != null && actual == expectedSha256.lowercase()) {
            DigestCheck.Matched
        } else {
            DigestCheck.Mismatched(expected = expectedSha256, actual = actual)
        }
    }

    /** 校验失败时给内部日志与排查用的文案，带两段前缀。 */
    fun mismatchMessage(expected: String, actual: String?): String =
        "SHA-256 不匹配（期望 ${expected.take(12)}…，实际 ${actual?.take(12) ?: "无法计算"}）"
}

/** 流式计算文件 SHA-256，返回小写 hex；读取失败返回 null。 */
expect fun sha256HexOfFile(path: String): String?

/** 删除下载结果文件；返回是否删除成功（失败不影响状态复位）。 */
expect fun deleteUpdateFile(path: String): Boolean
