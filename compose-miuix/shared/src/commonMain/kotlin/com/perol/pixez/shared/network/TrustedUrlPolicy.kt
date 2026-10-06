package com.perol.pixez.shared.network

import io.ktor.http.URLProtocol
import io.ktor.http.Url

/**
 * Validates URLs received from API responses, persisted history, or user settings
 * before they are passed to an HTTP client.
 */
object TrustedUrlPolicy {
    private const val API_HOST = "app-api.pixiv.net"
    private const val IMAGE_HOST = PixivHosts.HOST_PXIMG
    private const val IMAGE_STATIC_HOST = "s.pximg.net"
    private const val SPOTLIGHT_HOST = "www.pixivision.net"

    /** 内置可信图片 host：pximg 官方系域名 + 应用预置镜像。 */
    private val IMAGE_HOSTS = setOf(IMAGE_HOST, IMAGE_STATIC_HOST, PixivHosts.HOST_PIXIV_RE)
    private val RELEASE_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

    fun apiPaginationUrl(raw: String): String =
        requireHost(raw, setOf(API_HOST), "Pixiv API 分页 URL")

    fun imageUrl(raw: String, mirrorHost: String? = null): String =
        requireUrl(raw, "Pixiv 图片 URL") { isTrustedImageHost(it, mirrorHost) }

    fun spotlightUrl(raw: String): String =
        requireHost(raw, setOf(SPOTLIGHT_HOST), "Pixivision 文章 URL")

    fun releaseAssetUrl(raw: String): String =
        requireHost(raw, RELEASE_HOSTS, "GitHub 更新包 URL")

    /**
     * 小组件图片 URL：内置白名单之外额外放行用户在设置里配置的图源镜像 host。
     *
     * 供逐跳重定向校验使用，每一跳都需通过本方法才会发起下一次请求。
     */
    fun widgetImageUrl(raw: String, mirrorHost: String? = null): String =
        requireUrl(raw, "小组件图片 URL") { isTrustedImageHost(it, mirrorHost) }

    /**
     * 把图源设置值（裸 host、host:port、或用户粘贴的完整 URL）归一化为可比较的小写 host。
     *
     * 纯函数：无法安全解析的写法一律返回 null，调用方据此不放宽白名单。
     */
    fun normalizeImageHost(raw: String?): String? {
        val trimmed = raw?.trim()?.lowercase() ?: return null
        if (trimmed.isEmpty()) return null

        val withoutScheme = when {
            trimmed.startsWith("//") -> trimmed.removePrefix("//")
            trimmed.contains("://") -> {
                val scheme = trimmed.substringBefore("://")
                if (scheme != "http" && scheme != "https") return null
                trimmed.substringAfter("://")
            }
            else -> trimmed
        }
        val authority = withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')

        if (authority.isEmpty() || authority.contains('@')) return null
        val host = if (authority.contains(':')) {
            val port = authority.substringAfter(':')
            if (port.isEmpty() || !port.all { it.isDigit() }) return null
            authority.substringBefore(':')
        } else {
            authority
        }
        return host.takeIf { isDnsHostName(it) }
    }

    /**
     * 图片 host 是否放行：精确匹配内置白名单，或等于规范化后的用户配置镜像 host。
     */
    fun isTrustedImageHost(host: String?, mirrorHost: String?): Boolean {
        val normalized = normalizeImageHost(host) ?: return false
        if (normalized in IMAGE_HOSTS) return true
        return normalized == normalizeImageHost(mirrorHost)
    }

    /** 仅含 DNS 标签字符、每标签非空且不以连字符首尾的 host。 */
    private fun isDnsHostName(host: String): Boolean {
        if (!host.contains('.')) return false
        return host.split('.').all { label ->
            label.isNotEmpty() &&
                label.first() != '-' &&
                label.last() != '-' &&
                label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        }
    }

    private inline fun requireUrl(raw: String, label: String, hostAllowed: (String) -> Boolean): String {
        val normalized = raw.trim()
        val url = runCatching { Url(normalized) }
            .getOrElse { throw IllegalArgumentException("$label 格式无效") }
        val host = url.host.lowercase()
        require(url.protocol == URLProtocol.HTTPS) { "$label 必须使用 HTTPS" }
        require(hostAllowed(host)) { "$label 主机不受信任" }
        require(url.user == null && url.password == null) { "$label 不允许携带用户凭据" }
        return normalized
    }

    private fun requireHost(raw: String, allowedHosts: Set<String>, label: String): String =
        requireUrl(raw, label) { it in allowedHosts }
}
