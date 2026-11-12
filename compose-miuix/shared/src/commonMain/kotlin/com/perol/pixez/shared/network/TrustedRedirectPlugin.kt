package com.perol.pixez.shared.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.takeFrom
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom

private val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
private val PIXIVISION_IMAGE_HOSTS = setOf("pixivision.net", "www.pixivision.net", "embed.pixiv.net")
private val API_PIXIV_HOSTS = setOf(
    "app-api.pixiv.net",
    "oauth.secure.pixiv.net",
    "accounts.pixiv.net",
    "www.pixiv.net",
    "pixiv.net",
)

/**
 * 可信重定向插件配置。
 *
 * 所有通过 HTTP 客户端的 3xx 重定向都必须经过 [validateTarget] 逐跳校验（HTTPS、无用户凭据、受信任域名），
 * 防止恶意或被劫持的上游返回 `Location` 将请求导向内网（SSRF）、明文 HTTP 或攻击者域名并泄露请求头。
 */
class TrustedRedirectConfig {
    var maxRedirects: Int = 5
    var validateTarget: ((currentUrl: Url, targetUrl: String) -> String)? = null
}

/**
 * 逐跳可信重定向 Ktor 插件（须配合引擎 `followRedirects = false` 使用）。
 */
val TrustedRedirectPlugin = createClientPlugin("TrustedRedirect", ::TrustedRedirectConfig) {
    val maxRedirects = pluginConfig.maxRedirects
    val validator = requireNotNull(pluginConfig.validateTarget) {
        "TrustedRedirectPlugin requires validateTarget"
    }

    client.plugin(HttpSend).intercept { request ->
        var call = execute(request)
        var redirects = 0
        while (call.response.status.value in REDIRECT_STATUS_CODES) {
            redirects++
            require(redirects <= maxRedirects) {
                "Too many redirects (>$maxRedirects)"
            }
            val location = call.response.headers[HttpHeaders.Location]
                ?: throw IllegalStateException("Redirect (${call.response.status}) missing Location header")
            val currentUrl = call.request.url
            val nextRawUrl = resolveRedirectUrl(currentUrl, location)
            val verifiedNextUrl = validator(currentUrl, nextRawUrl)
            val verifiedHost = Url(verifiedNextUrl).host
            val nextRequest = HttpRequestBuilder().takeFrom(request).apply {
                url.takeFrom(verifiedNextUrl)
                // 跨域重定向时剥离敏感认证与会话头
                if (!verifiedHost.equals(currentUrl.host, ignoreCase = true)) {
                    headers.remove(HttpHeaders.Authorization)
                    headers.remove(HttpHeaders.Cookie)
                }
                if (call.response.status == HttpStatusCode.SeeOther ||
                    ((call.response.status == HttpStatusCode.MovedPermanently ||
                        call.response.status == HttpStatusCode.Found) && method == HttpMethod.Post)
                ) {
                    method = HttpMethod.Get
                    setBody(EmptyContent)
                    headers.remove(HttpHeaders.ContentType)
                    headers.remove(HttpHeaders.ContentLength)
                }
            }
            call = execute(nextRequest)
        }
        call
    }
}

/**
 * 将 HTTP `Location` 响应头解析为基于 [baseUrl] 的绝对 URL 字符串。
 */
internal fun resolveRedirectUrl(baseUrl: Url, location: String): String {
    val trimmed = location.trim()
    require(trimmed.isNotEmpty()) { "Redirect Location must not be empty" }
    val lower = trimmed.lowercase()
    if (lower.startsWith("http://") ||
        lower.startsWith("https://") ||
        (lower.contains("://") && !trimmed.startsWith("//"))
    ) {
        return trimmed
    }
    val builder = URLBuilder(baseUrl)
    builder.encodedParameters.clear()
    builder.fragment = ""
    if (trimmed.startsWith("//")) {
        builder.takeFrom("${baseUrl.protocol.name}:$trimmed")
    } else if (trimmed.startsWith("/")) {
        builder.encodedPath = ""
        builder.takeFrom(trimmed)
    } else {
        val baseDir = baseUrl.encodedPath.substringBeforeLast('/', "")
        builder.encodedPath = if (baseDir.isEmpty()) "/$trimmed" else "$baseDir/$trimmed"
        if (trimmed.contains('?') || trimmed.contains('#')) {
            val baseOnly = builder.buildString().substringBefore(trimmed)
            val rebuilder = URLBuilder(baseOnly)
            rebuilder.takeFrom(trimmed)
            return rebuilder.buildString()
        }
    }
    return builder.buildString()
}

/**
 * 下载与图片客户端（`downloadClient`）逐跳重定向校验策略。
 *
 * - 若当前处于 Pixivision 域名：仅允许跳转到 Pixivision 或受信任图片/镜像 CDN；
 * - 若当前处于 SauceNAO 域名：仅允许跳转到 SauceNAO 或受信任图片/镜像 CDN；
 * - 其它图片/Ugoira 请求：严格受限于 [TrustedUrlPolicy.imageUrl]。
 */
internal fun validateDownloadRedirectTarget(
    currentUrl: Url,
    targetUrl: String,
    mirrorHost: String?,
): String {
    val currentHost = currentUrl.host.lowercase()
    if (isPixivisionHost(currentHost)) {
        return runCatching { TrustedUrlPolicy.spotlightUrl(targetUrl) }
            .getOrElse { TrustedUrlPolicy.imageUrl(targetUrl, mirrorHost) }
    }
    if (isSauceNaoHost(currentHost)) {
        if (isHttpsNoUserInfo(targetUrl) { isSauceNaoHost(it) }) {
            return targetUrl.trim()
        }
        return TrustedUrlPolicy.imageUrl(targetUrl, mirrorHost)
    }
    return TrustedUrlPolicy.imageUrl(targetUrl, mirrorHost)
}

/**
 * 网页抓取客户端（`webClient`）逐跳重定向校验策略。
 */
internal fun validateWebRedirectTarget(
    currentUrl: Url,
    targetUrl: String,
): String {
    val currentHost = currentUrl.host.lowercase()
    if (isSauceNaoHost(currentHost)) {
        require(isHttpsNoUserInfo(targetUrl) { isSauceNaoHost(it) }) {
            "Untrusted SauceNAO redirect target: $targetUrl"
        }
        return targetUrl.trim()
    }
    return TrustedUrlPolicy.spotlightUrl(targetUrl)
}

/**
 * 公告板客户端（`boardHttpClient`）逐跳重定向校验策略：仅允许 GitHub Raw 与 jsDelivr CDN。
 */
internal fun validateBoardRedirectTarget(targetUrl: String): String {
    require(
        isHttpsNoUserInfo(targetUrl) { host ->
            host == "raw.githubusercontent.com" ||
                host == "fastly.jsdelivr.net" ||
                host == "cdn.jsdelivr.net"
        },
    ) {
        "Untrusted Board redirect target: $targetUrl"
    }
    return targetUrl.trim()
}

/**
 * Pixiv API / OAuth 客户端逐跳重定向校验策略。
 */
internal fun validateApiRedirectTarget(targetUrl: String): String {
    require(isHttpsNoUserInfo(targetUrl) { host ->
        host in API_PIXIV_HOSTS || host.endsWith(".pixiv.net") || host.endsWith(".pixiv.me")
    }) {
        "Untrusted Pixiv API redirect target: $targetUrl"
    }
    return targetUrl.trim()
}

/**
 * UI 图片加载器（`PixivAsyncImage`）远端 URL 入口校验：
 * 放行本地 URI（`file:` / `content:` / `data:`）与非 URL 资源模型；
 * 对远端 HTTP/HTTPS URL 要求 HTTPS、无用户凭据且命中 Pixiv 图片/镜像、Pixivision 或 SauceNAO 白名单，
 * 不合规 URL 返回 `null` 以阻断 Coil 发起不安全请求。
 */
internal fun sanitizeRemoteImageModel(model: Any?, mirrorHost: String?): Any? {
    val raw = model as? String ?: return model
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return model
    val lower = trimmed.lowercase()
    if (lower.startsWith("file:") || lower.startsWith("content:") || lower.startsWith("data:")) {
        return model
    }
    if (!trimmed.contains("://")) {
        return model
    }
    val allowed = isHttpsNoUserInfo(trimmed) { host ->
        TrustedUrlPolicy.isTrustedImageHost(host, mirrorHost) ||
            isPixivisionHost(host) ||
            isSauceNaoHost(host)
    }
    return if (allowed) model else null
}

private fun isPixivisionHost(host: String): Boolean =
    host in PIXIVISION_IMAGE_HOSTS || host.endsWith(".pixivision.net")

private fun isSauceNaoHost(host: String): Boolean =
    host == "saucenao.com" || host.endsWith(".saucenao.com")

private inline fun isHttpsNoUserInfo(raw: String, hostAllowed: (String) -> Boolean): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return false
    val url = runCatching { Url(trimmed) }.getOrNull() ?: return false
    if (url.protocol != URLProtocol.HTTPS) return false
    if (url.user != null || url.password != null) return false
    val host = url.host.lowercase()
    if (host.isEmpty()) return false
    return hostAllowed(host)
}
