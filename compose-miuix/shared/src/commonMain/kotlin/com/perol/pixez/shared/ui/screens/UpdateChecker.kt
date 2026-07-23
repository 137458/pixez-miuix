package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.AppInfo
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * GitHub Release API 返回的最新版本信息。
 *
 * @param tag_name 版本标签，例如 "v0.9.42"。
 */
@Serializable
private data class GitHubRelease(
    val tag_name: String? = null,
)

/**
 * 创建用于检查 GitHub Release 的 HttpClient，配置 JSON、超时与 User-Agent。
 * 由 [AppDependencies] 持有生命周期，也可作为默认客户端使用。
 */
internal fun createUpdateCheckClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            },
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 10_000
    }
}

/**
 * 复用的 GitHub API HttpClient，供没有注入能力的调用方作为默认参数使用。
 */
internal val defaultUpdateCheckClient: HttpClient by lazy {
    createUpdateCheckClient()
}

/**
 * 从 GitHub Release API 异步获取最新版本号。
 *
 * @param client 执行请求的 HttpClient，默认使用顶层单例以保持兼容。
 * @return 成功返回最新版本号（已去除前导 "v"），失败返回异常。
 */
suspend fun checkLatestVersion(
    client: HttpClient = defaultUpdateCheckClient,
): Result<String> {
    return try {
        val release: GitHubRelease = client
            .get("https://api.github.com/repos/Notsfsssf/pixez-flutter/releases/latest") {
                header("User-Agent", "PixEz-MIUIX/${AppInfo.VERSION_NAME}")
            }
            .body()
        val latest = release.tag_name?.removePrefix("v")
        if (latest.isNullOrBlank()) {
            Result.failure(Exception("未找到版本号"))
        } else {
            Result.success(latest)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e("检查更新失败", e)
        Result.failure(e)
    }
}

/**
 * 判断 [latest] 是否比当前应用版本新。
 *
 * 版本号会去除前导 "v" 与后缀（如 "-miuix"），按 "major.minor.patch" 分段比较。
 *
 * @param latest 从远程获取的最新版本号。
 * @param current 当前应用版本号，默认 [AppInfo.VERSION_NAME]。
 */
fun hasNewVersion(
    latest: String,
    current: String = AppInfo.VERSION_NAME,
): Boolean {
    val normalizedLatest = latest.normalizeVersion()
    val normalizedCurrent = current.normalizeVersion()
    return compareVersion(normalizedLatest, normalizedCurrent) > 0
}

/**
 * 将版本字符串归一化为 "major.minor.patch" 形式。
 */
private fun String.normalizeVersion(): String {
    return this.trimStart('v').substringBefore('-').substringBefore('+')
}

/**
 * 按版本号各段数字大小比较。
 *
 * 遇到无法解析为整数的段时安全降级为 0，并记录警告日志；不会因外部返回异常版本格式而崩溃。
 *
 * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等。
 */
private fun compareVersion(v1: String, v2: String): Int {
    fun parseParts(version: String, full: String): List<Int> = version.split('.').map { segment ->
        segment.toIntOrNull() ?: run {
            Napier.w("无法解析版本号段: '$segment'（完整版本: '$full'），降级为 0 处理")
            0
        }
    }
    val parts1 = parseParts(v1, v1)
    val parts2 = parseParts(v2, v2)
    val maxLength = maxOf(parts1.size, parts2.size)
    for (i in 0 until maxLength) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) return p1.compareTo(p2)
    }
    return 0
}
