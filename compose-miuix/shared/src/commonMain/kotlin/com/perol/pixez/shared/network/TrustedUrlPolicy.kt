package com.perol.pixez.shared.network

import io.ktor.http.URLProtocol
import io.ktor.http.Url

/**
 * Validates URLs received from API responses, persisted history, or user settings
 * before they are passed to an HTTP client.
 */
object TrustedUrlPolicy {
    private const val API_HOST = "app-api.pixiv.net"
    private const val IMAGE_HOST = "i.pximg.net"
    private const val IMAGE_STATIC_HOST = "s.pximg.net"
    private const val SPOTLIGHT_HOST = "www.pixivision.net"
    private val RELEASE_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

    fun apiPaginationUrl(raw: String): String =
        requireHost(raw, setOf(API_HOST), "Pixiv API 分页 URL")

    fun imageUrl(raw: String): String =
        requireHost(raw, setOf(IMAGE_HOST, IMAGE_STATIC_HOST), "Pixiv 图片 URL")

    fun spotlightUrl(raw: String): String =
        requireHost(raw, setOf(SPOTLIGHT_HOST), "Pixivision 文章 URL")

    fun releaseAssetUrl(raw: String): String =
        requireHost(raw, RELEASE_HOSTS, "GitHub 更新包 URL")

    private fun requireHost(raw: String, allowedHosts: Set<String>, label: String): String {
        val normalized = raw.trim()
        val url = runCatching { Url(normalized) }
            .getOrElse { throw IllegalArgumentException("$label 格式无效") }
        val host = url.host.lowercase()
        require(url.protocol == URLProtocol.HTTPS) { "$label 必须使用 HTTPS" }
        require(host in allowedHosts) { "$label 主机不受信任" }
        require(url.user == null && url.password == null) { "$label 不允许携带用户凭据" }
        return normalized
    }
}
