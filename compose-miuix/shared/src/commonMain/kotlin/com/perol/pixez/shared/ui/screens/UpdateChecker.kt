package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.network.TrustedUrlPolicy
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.AppConstants
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import com.perol.pixez.shared.network.createPlatformHttpClient
import com.perol.pixez.shared.platform.isAndroidPlatform
import com.perol.pixez.shared.platform.isDesktopPlatform

/**
 * GitHub Release Asset 信息。
 */
@Serializable
internal data class GitHubReleaseAsset(
    val name: String? = null,
    val browser_download_url: String? = null,
    val size: Long? = null,
    // GitHub 返回形如 "sha256:<hex>"，用于安装包完整性校验
    val digest: String? = null,
)

/**
 * 按当前运行平台从 Release Assets 中择优匹配安装包：
 * - Desktop 优先匹配 `.exe`，其次 `.msi`，再次 `.zip`；绝不误选 `.apk`。
 * - Android 匹配 `.apk`。
 */
internal fun selectPlatformReleaseAsset(
    assets: List<GitHubReleaseAsset>?,
    isDesktop: Boolean = isDesktopPlatform(),
    isAndroid: Boolean = isAndroidPlatform(),
): GitHubReleaseAsset? {
    val validAssets = assets?.filter {
        !it.name.isNullOrBlank() && !it.browser_download_url.isNullOrBlank()
    }.orEmpty()
    if (validAssets.isEmpty()) return null

    return when {
        isDesktop -> {
            val desktopExtensions = listOf(".exe", ".msi", ".zip")
            desktopExtensions.firstNotNullOfOrNull { ext ->
                validAssets.firstOrNull { it.name.orEmpty().endsWith(ext, ignoreCase = true) }
            }
        }
        isAndroid -> {
            validAssets.firstOrNull { it.name.orEmpty().endsWith(".apk", ignoreCase = true) }
        }
        else -> null
    }
}

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
    val assets: List<GitHubReleaseAsset>? = null,
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
    val downloadUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val digest: String? = null,
)

/**
 * 创建用于检查 GitHub Release 的 HttpClient，复用平台网络引擎（含 DoH / 代理支持）。
 */
internal fun createUpdateCheckClient(): HttpClient = createPlatformHttpClient {
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
 * 获取当前已安装版本的本地结构化信息，提供秒开体验与离线兜底。
 */
fun getLocalReleaseInfo(): ReleaseInfo = ReleaseInfo(
    tagName = "v${AppInfo.VERSION_NAME}",
    versionName = AppInfo.VERSION_NAME,
    title = "PixEz MIUIX v${AppInfo.VERSION_NAME}",
    changelog = AppInfo.CURRENT_CHANGELOG,
    releaseUrl = com.perol.pixez.shared.ui.AppConstants.Urls.GITHUB_RELEASES,
    publishedAt = null,
    isNew = false,
    downloadUrl = null,
    fileName = null,
    fileSize = null,
)

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
        val response = client
            .get(AppConstants.Urls.GITHUB_RELEASES_LATEST_API) {
                header("User-Agent", "PixEz-MIUIX/${AppInfo.VERSION_NAME}")
            }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("GitHub Release 请求异常: HTTP ${response.status.value}")
        }
        val release: GitHubRelease = response.body()
        val tag = release.tag_name
        if (tag.isNullOrBlank()) {
            throw IllegalStateException("GitHub Release 响应缺少有效 tag_name，可能触发未认证速率限制")
        }
        val versionName = tag.removePrefix("v").ifBlank { "unknown" }
        val isNew = hasNewVersion(versionName)

        val matchedAsset = selectPlatformReleaseAsset(release.assets)
        val downloadUrl = matchedAsset?.browser_download_url?.let(TrustedUrlPolicy::releaseAssetUrl)
        val fileName = matchedAsset?.name?.let {
            com.perol.pixez.shared.platform.FileNamePolicy.requireSafeBaseName(it)
        }
        val fileSize = matchedAsset?.size

        // 当远端日志为空且为当前版本时，优雅回退到本地内置日志
        val changelog = release.body?.takeIf { it.isNotBlank() }
            ?: if (!isNew) AppInfo.CURRENT_CHANGELOG else ""

        val releaseInfo = ReleaseInfo(
            tagName = tag,
            versionName = versionName,
            title = release.name ?: "PixEz MIUIX $tag",
            changelog = changelog,
            releaseUrl = release.html_url ?: "https://github.com/137458/pixez-miuix/releases",
            publishedAt = release.published_at,
            isNew = isNew,
            downloadUrl = downloadUrl,
            fileName = fileName,
            fileSize = fileSize,
            digest = matchedAsset?.digest,
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
