package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.AppInfo
import com.perol.pixez.shared.network.GitHubReleaseEndpoints
import com.perol.pixez.shared.network.TrustedUrlPolicy
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlin.concurrent.Volatile

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
 * 从 Release Assets 中匹配 Android 安装包（`.apk`）。
 */
internal fun selectPlatformReleaseAsset(
    assets: List<GitHubReleaseAsset>?,
): GitHubReleaseAsset? {
    val validAssets = assets?.filter {
        !it.name.isNullOrBlank() && !it.browser_download_url.isNullOrBlank()
    }.orEmpty()
    if (validAssets.isEmpty()) return null

    return validAssets.firstOrNull { it.name.orEmpty().endsWith(".apk", ignoreCase = true) }
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
 * 获取当前已安装版本的本地结构化信息，提供秒开体验与离线兜底。
 */
fun getLocalReleaseInfo(): ReleaseInfo = ReleaseInfo(
    tagName = "v${AppInfo.VERSION_NAME}",
    versionName = AppInfo.VERSION_NAME,
    title = "PixEz MIUIX v${AppInfo.VERSION_NAME}",
    changelog = AppInfo.CURRENT_CHANGELOG,
    releaseUrl = GitHubReleaseEndpoints.RELEASES_PAGE,
    publishedAt = null,
    isNew = false,
    downloadUrl = null,
    fileName = null,
    fileSize = null,
)

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

/**
 * 仅表示「REST API 通道不可用」：网络异常、非 2xx（含 403 限流耗尽）或条件请求缺少缓存。
 * 只有这类失败才回退到 Atom Feed；载荷错误与安全校验失败照原样上抛，避免掩盖真实问题。
 */
private class UpdateApiUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/**
 * 更新检查仓库：优先请求 GitHub Release API，限流耗尽时回退到 releases.atom + expanded_assets。
 *
 * 网络请求逻辑自 ui 层 UpdateChecker.kt 收编（架构审查 中-4），
 * [client] 由调用方注入并管理生命周期（AppDependencies.close()），
 * 取代 UI 直连的独立全局 client 与容器裸 HttpClient 双通道。
 */
class UpdateRepository(
    private val client: HttpClient,
) {
    private data class ApiReleaseCache(
        val etag: String,
        val releaseInfo: ReleaseInfo,
    )

    /** 最近一次成功检查到的版本发布信息缓存（供设置页重建首帧同步展示更新标记）。 */
    @Volatile
    var cachedReleaseInfo: ReleaseInfo? = null
        private set

    /** 上次 API 通道响应的 ETag 与对应载荷快照：命中 304 时原子复用，不被 Atom 回退覆盖。 */
    @Volatile
    private var apiCache: ApiReleaseCache? = null

    /**
     * 获取完整版本发布信息：API 优先，限流或网络异常时回退 Atom Feed。
     */
    suspend fun fetchLatestReleaseInfo(): Result<ReleaseInfo> {
        return try {
            Result.success(fetchReleaseInfoOrThrow())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("获取 Release 信息失败", e)
            Result.failure(e)
        }
    }

    /**
     * 双通道获取：API 通道确认不可用时才回退，兜底同样失败时合并两侧原因返回。
     */
    private suspend fun fetchReleaseInfoOrThrow(): ReleaseInfo {
        val apiFailure = try {
            return fetchFromApi()
        } catch (e: UpdateApiUnavailableException) {
            e
        }
        Napier.w("GitHub Release API 不可用，回退 Atom Feed: ${apiFailure.message}")
        return try {
            fetchFromAtomFeed()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(
                (apiFailure.message ?: "GitHub Release 请求异常") +
                    "；Atom 兜底同样失败：" + (e.message ?: e::class.simpleName.orEmpty()),
                apiFailure,
            )
        }
    }

    /** API 通道：带 ETag 条件请求，200 走完整解析，304 直接复用对应 API 缓存快照。 */
    private suspend fun fetchFromApi(): ReleaseInfo {
        val snapshot = apiCache
        val response = try {
            client.get(GitHubReleaseEndpoints.LATEST_API) {
                header("User-Agent", userAgent())
                if (snapshot != null) header(HttpHeaders.IfNoneMatch, snapshot.etag)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw UpdateApiUnavailableException(
                "GitHub Release 请求异常: " + (e.message ?: e::class.simpleName.orEmpty()),
                e,
            )
        }
        if (response.status == HttpStatusCode.NotModified) {
            val cached = snapshot?.releaseInfo
                ?: throw UpdateApiUnavailableException("GitHub Release 条件请求返回 304 但缺少本地缓存")
            val refreshed = cached.copy(isNew = hasNewVersion(cached.versionName))
            cachedReleaseInfo = refreshed
            return refreshed
        }
        if (!response.status.isSuccess()) {
            throw UpdateApiUnavailableException("GitHub Release 请求异常: HTTP ${response.status.value}")
        }
        val release: GitHubRelease = response.body()
        val tag = release.tag_name
        if (tag.isNullOrBlank()) {
            throw IllegalStateException("GitHub Release 响应缺少有效 tag_name，可能触发未认证速率限制")
        }
        val versionName = tag.removePrefix("v").ifBlank { "unknown" }
        val isNew = hasNewVersion(versionName)

        val matchedAsset = selectPlatformReleaseAsset(assets = release.assets)
        // asset 直链必须指向官方仓库（owner/repo 白名单），拒绝 GitHub 域内第三方仓库投毒。
        val downloadUrl = matchedAsset?.browser_download_url?.let {
            TrustedUrlPolicy.officialReleaseAssetUrl(it, GitHubReleaseEndpoints.OFFICIAL_REPO_PATH)
        }
        val fileName = matchedAsset?.name?.let {
            com.perol.pixez.shared.platform.FileNamePolicy.requireSafeBaseName(it)
        }
        val fileSize = matchedAsset?.size

        // 当远端日志为空且为当前版本时，优雅回退到本地内置日志
        val changelog = release.body?.takeIf { it.isNotBlank() }
            ?: if (!isNew) AppInfo.CURRENT_CHANGELOG else ""

        val safeReleaseUrl = release.html_url?.let { raw ->
            runCatching {
                TrustedUrlPolicy.officialReleasePageUrl(raw, GitHubReleaseEndpoints.OFFICIAL_REPO_PATH)
            }.getOrNull()
        } ?: GitHubReleaseEndpoints.RELEASES_PAGE

        val releaseInfo = ReleaseInfo(
            tagName = tag,
            versionName = versionName,
            title = release.name ?: "PixEz MIUIX $tag",
            changelog = changelog,
            releaseUrl = safeReleaseUrl,
            publishedAt = release.published_at,
            isNew = isNew,
            downloadUrl = downloadUrl,
            fileName = fileName,
            fileSize = fileSize,
            digest = matchedAsset?.digest,
        )
        val etag = response.headers[HttpHeaders.ETag]?.takeIf { it.isNotBlank() }
        apiCache = etag?.let { ApiReleaseCache(etag = it, releaseInfo = releaseInfo) }
        cachedReleaseInfo = releaseInfo
        return releaseInfo
    }

    /**
     * Atom 兜底通道：feed 不限流，正文是 XML 转义的 HTML，转换为 Markdown 后直接展示。
     * 资产直链需再请求 expanded_assets 片段；该片段失败只降级为「无直链」，不影响版本信息。
     */
    private suspend fun fetchFromAtomFeed(): ReleaseInfo {
        val response = try {
            client.get(GitHubReleaseEndpoints.RELEASES_ATOM) {
                header("User-Agent", userAgent())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(
                "GitHub Release Feed 请求异常: " + (e.message ?: e::class.simpleName.orEmpty()),
                e,
            )
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("GitHub Release Feed 请求异常: HTTP ${response.status.value}")
        }
        val entry = parseLatestFeedEntry(response.bodyAsText())
            ?: throw IllegalStateException("GitHub Release Feed 响应缺少有效 entry")
        val versionName = entry.tagName.removePrefix("v").ifBlank { "unknown" }
        val isNew = hasNewVersion(versionName)
        val asset = resolveFeedAsset(entry.tagName)
        val changelog = entry.changelogMarkdown.takeIf { it.isNotBlank() }
            ?: if (!isNew) AppInfo.CURRENT_CHANGELOG else ""
        val safeReleaseUrl = runCatching {
            TrustedUrlPolicy.officialReleasePageUrl(entry.releaseUrl, GitHubReleaseEndpoints.OFFICIAL_REPO_PATH)
        }.getOrDefault(GitHubReleaseEndpoints.RELEASES_PAGE)

        // Atom expanded_assets 仅提供两位小数估算体积（如 8.04 MB），不得作为 fileSize 传入
        // AppUpdateDownloader.restoreCompletedFile 参与精确字节相等校验，避免已下载包重启后被丢弃；
        // 下载进度由 HTTP Content-Length 接管，完成包完整性由 SHA-256 校验保证。
        val releaseInfo = ReleaseInfo(
            tagName = entry.tagName,
            versionName = versionName,
            title = entry.title,
            changelog = changelog,
            releaseUrl = safeReleaseUrl,
            publishedAt = entry.publishedAt,
            isNew = isNew,
            downloadUrl = asset?.browser_download_url,
            fileName = asset?.name,
            fileSize = null,
            digest = asset?.digest,
        )
        apiCache = null
        cachedReleaseInfo = releaseInfo
        return releaseInfo
    }

    /**
     * 解析 Atom 兜底通道下的安装包直链：网络异常、非 2xx 或缺失 APK 时降级为「无直链」；
     * 若解析出的直链或文件名违反安全白名单（fail-closed），则按原样上抛异常拒绝整个更新结果。
     */
    private suspend fun resolveFeedAsset(tagName: String): GitHubReleaseAsset? {
        val response = try {
            client.get(GitHubReleaseEndpoints.expandedAssetsUrl(tagName)) {
                header("User-Agent", userAgent())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("GitHub 发布资产列表请求失败，降级为无直链更新: " + (e.message ?: ""))
            return null
        }
        if (!response.status.isSuccess()) {
            Napier.w("GitHub 发布资产列表不可用: HTTP ${response.status.value}")
            return null
        }
        val asset = parseExpandedAssetApk(response.bodyAsText()) ?: return null
        // 与 API 通道同一条白名单：直链必须指向官方仓库，否则整体失败（fail closed）。
        val downloadUrl = TrustedUrlPolicy.officialReleaseAssetUrl(
            asset.browser_download_url.orEmpty(),
            GitHubReleaseEndpoints.OFFICIAL_REPO_PATH,
        )
        val fileName = com.perol.pixez.shared.platform.FileNamePolicy.requireSafeBaseName(asset.name.orEmpty())
        return asset.copy(name = fileName, browser_download_url = downloadUrl)
    }

    private fun userAgent(): String = "PixEz-MIUIX/" + AppInfo.VERSION_NAME
}
