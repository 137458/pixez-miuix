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
 * GitHub Release API 返回的完整版本信息。
 */
@Serializable
private data class GitHubRelease(
    val tag_name: String? = null,
    val name: String? = null,
    val body: String? = null,
    val html_url: String? = null,
    val published_at: String? = null,
)

/**
 * 结构化的应用发布版本信息。
 */
data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val changelog: String,
    val releaseUrl: String,
    val publishedAt: String?,
    val isNew: Boolean,
)

/**
 * 创建用于检查 GitHub Release 的 HttpClient，配置 JSON、超时与 User-Agent。
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
 * 复用的 GitHub API HttpClient。
 */
internal val defaultUpdateCheckClient: HttpClient by lazy {
    createUpdateCheckClient()
}

/**
 * 从 GitHub Release API 获取完整版本发布信息。
 */
suspend fun fetchLatestReleaseInfo(
    client: HttpClient = defaultUpdateCheckClient,
): Result<ReleaseInfo> {
    return try {
        val release: GitHubRelease = client
            .get("https://api.github.com/repos/137458/pixez-miuix/releases/latest") {
                header("User-Agent", "PixEz-MIUIX/${AppInfo.VERSION_NAME}")
            }
            .body()
        val tag = release.tag_name ?: ""
        val versionName = tag.removePrefix("v").ifBlank { "unknown" }
        val releaseInfo = ReleaseInfo(
            tagName = tag,
            versionName = versionName,
            title = release.name ?: "PixEz MIUIX $tag",
            changelog = release.body.orEmpty(),
            releaseUrl = release.html_url ?: "https://github.com/137458/pixez-miuix/releases",
            publishedAt = release.published_at,
            isNew = hasNewVersion(versionName),
        )
        Result.success(releaseInfo)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e("获取 Release 信息失败", e)
        Result.failure(e)
    }
}

/**
 * 从 GitHub Release API 异步获取最新版本号。
 */
suspend fun checkLatestVersion(
    client: HttpClient = defaultUpdateCheckClient,
): Result<String> {
    return fetchLatestReleaseInfo(client).map { it.versionName }
}

/**
 * 判断 [latest] 是否比当前应用版本新。
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
