package com.perol.pixez.shared.network

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Pixiv API 固定请求头生成器。
 *
 * 复用原 Flutter 应用的逻辑：
 * - X-Client-Time 为当前 UTC 时间的 ISO-8601 格式（yyyy-MM-dd'T'HH:mm:ss+00:00）。
 * - X-Client-Hash 为 time + hashSalt 的 MD5 摘要。
 */
object PixivHeaders {
    // 与原 Flutter ApiClient / OAuthClient 保持一致。
    private const val HASH_SALT =
        "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    private const val APP_VERSION = "5.0.166"

    /**
     * 生成当前 UTC 时间字符串，格式固定为 yyyy-MM-dd'T'HH:mm:ss+00:00。
     */
    fun getIsoDate(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        // 手动补零，避免依赖 locale 格式化产生差异。
        fun Int.pad2() = toString().padStart(2, '0')
        return buildString {
            append(now.year)
            append('-')
            append(now.monthNumber.pad2())
            append('-')
            append(now.dayOfMonth.pad2())
            append('T')
            append(now.hour.pad2())
            append(':')
            append(now.minute.pad2())
            append(':')
            append(now.second.pad2())
            append("+00:00")
        }
    }

    /**
     * 计算字符串的 MD5 摘要，返回小写十六进制字符串。
     *
     * 使用各平台原生 MessageDigest / CommonCrypto 实现。
     */
    fun getHash(input: String): String {
        val digest = md5(input.encodeToByteArray())
        return digest.joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }

    /**
     * 获取公共请求头字典，适用于 app-api.pixiv.net 与 oauth.secure.pixiv.net。
     */
    fun commonHeaders(acceptLanguage: String = "zh-CN"): Map<String, String> {
        val time = getIsoDate()
        return mapOf(
            "X-Client-Time" to time,
            "X-Client-Hash" to getHash(time + HASH_SALT),
            "User-Agent" to "PixivAndroidApp/$APP_VERSION (Android 10.0; Pixel C)",
            "Accept-Language" to acceptLanguage,
            "App-OS" to "Android",
            "App-OS-Version" to "Android 10.0",
            "App-Version" to APP_VERSION,
        )
    }
}
