package com.perol.pixez.shared.navigation

import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.navigation.RootComponent
import io.ktor.http.Url
import io.ktor.http.decodeURLQueryComponent

/**
 * 跨平台统一 DeepLink / URL / 剪贴板文本解析结果。
 */
sealed interface ParsedDeepLink {
    data class OAuthLogin(val code: String) : ParsedDeepLink
    data class MainTab(val tab: RootComponent.MainTab) : ParsedDeepLink
    data class IllustDetail(val illustId: Long) : ParsedDeepLink
    data class UserDetail(val userId: Long) : ParsedDeepLink
    data class Search(val keyword: String) : ParsedDeepLink
    data object DownloadTasks : ParsedDeepLink
    data object History : ParsedDeepLink
}

object DeepLinkParser {
    private val ILLUST_REGEX = Regex("""(?:artworks/|illust_id=)(\d+)""", RegexOption.IGNORE_CASE)
    private val USER_REGEX = Regex("""(?:users/|member\.php\?id=)(\d+)""", RegexOption.IGNORE_CASE)
    private val CODE_PARAM_REGEX = Regex("""(?:[?&]|^)code=([^&#\s]+)""")

    /**
     * 解析启动参数、URI Scheme (`pixiv://`, `pixez://`)、网页链接或剪贴板文本。
     */
    fun parse(raw: String?): ParsedDeepLink? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        // 1. 尝试解析标准或自定义 URI (pixiv://, pixez://, https://...)
        parseStructuredUri(text)?.let { return it }

        // 2. 尝试从混排文本（如分享文案、剪贴板内容）中提取 OAuth code 或作品/画师链接
        CODE_PARAM_REGEX.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { code ->
            return ParsedDeepLink.OAuthLogin(decodeComponentSafe(code))
        }

        ILLUST_REGEX.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { id ->
            return ParsedDeepLink.IllustDetail(id)
        }

        USER_REGEX.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { id ->
            return ParsedDeepLink.UserDetail(id)
        }

        // 3. 纯 6~10 位数字视为插画 ID
        val pureId = text.toLongOrNull()
        if (pureId != null && text.length in 6..10) {
            return ParsedDeepLink.IllustDetail(pureId)
        }

        return null
    }

    /**
     * 将解析出的 [ParsedDeepLink] 分发至 [RootComponent] 或 OAuth 登录回调。
     */
    fun dispatch(
        parsed: ParsedDeepLink,
        rootComponent: RootComponent,
        onOAuthCode: (String) -> Unit,
    ) {
        when (parsed) {
            is ParsedDeepLink.OAuthLogin -> onOAuthCode(parsed.code)
            is ParsedDeepLink.MainTab -> rootComponent.onMainTabSelected(parsed.tab)
            is ParsedDeepLink.IllustDetail -> rootComponent.onIllustClicked(parsed.illustId)
            is ParsedDeepLink.UserDetail -> rootComponent.onUserClicked(parsed.userId)
            is ParsedDeepLink.Search -> {
                if (parsed.keyword.isNotBlank()) {
                    rootComponent.onSearchClicked(parsed.keyword)
                } else {
                    rootComponent.onMainTabSelected(RootComponent.MainTab.Search)
                }
            }
            ParsedDeepLink.DownloadTasks -> rootComponent.onDownloadTaskClicked()
            ParsedDeepLink.History -> rootComponent.onHistoryClicked()
        }
    }

    private fun parseStructuredUri(text: String): ParsedDeepLink? {
        if (!text.contains("://")) return null
        val url = runCatching { Url(text) }.getOrNull() ?: return null
        val scheme = url.protocol.name.lowercase()
        val host = url.host.lowercase()
        val pathSegments = url.segments.filter { it.isNotBlank() }
        val path = "/" + pathSegments.joinToString("/")

        when {
            scheme == AppConstants.Scheme.SCHEME_PIXEZ || scheme == "pixiv" -> {
                when (host) {
                    "account", "oauth" -> {
                        val code = url.parameters["code"]?.trim().orEmpty()
                        if (code.isNotEmpty()) return ParsedDeepLink.OAuthLogin(code)
                    }
                    "ranking" -> return ParsedDeepLink.MainTab(RootComponent.MainTab.Ranking)
                    "search" -> {
                        val word = url.parameters["word"] ?: url.parameters["keyword"] ?: url.parameters["q"]
                        return ParsedDeepLink.Search(word?.trim().orEmpty())
                    }
                    "downloads", "download_task" -> return ParsedDeepLink.DownloadTasks
                    "history" -> return ParsedDeepLink.History
                    "illust", "artworks" -> {
                        val id = pathSegments.lastOrNull()?.toLongOrNull()
                            ?: url.parameters["id"]?.toLongOrNull()
                        if (id != null) return ParsedDeepLink.IllustDetail(id)
                    }
                    "users", "user" -> {
                        val id = pathSegments.lastOrNull()?.toLongOrNull()
                            ?: url.parameters["id"]?.toLongOrNull()
                        if (id != null) return ParsedDeepLink.UserDetail(id)
                    }
                }
            }
            host == "pixiv.net" || host.endsWith(".pixiv.net") ||
                host == "pixiv.me" || host.endsWith(".pixiv.me") -> {
                if (path.contains("/users/auth/pixiv/callback")) {
                    val code = url.parameters["code"]?.trim().orEmpty()
                    if (code.isNotEmpty()) return ParsedDeepLink.OAuthLogin(code)
                }
                val artworksIndex = pathSegments.indexOfFirst { it.equals("artworks", ignoreCase = true) }
                if (artworksIndex >= 0) {
                    val id = pathSegments.getOrNull(artworksIndex + 1)?.toLongOrNull()
                    if (id != null) return ParsedDeepLink.IllustDetail(id)
                }
                val usersIndex = pathSegments.indexOfFirst { it.equals("users", ignoreCase = true) }
                if (usersIndex >= 0) {
                    val id = pathSegments.getOrNull(usersIndex + 1)?.toLongOrNull()
                    if (id != null) return ParsedDeepLink.UserDetail(id)
                }
                url.parameters["illust_id"]?.toLongOrNull()?.let { return ParsedDeepLink.IllustDetail(it) }
                if (path.endsWith("/member.php")) {
                    url.parameters["id"]?.toLongOrNull()?.let { return ParsedDeepLink.UserDetail(it) }
                }
            }
        }
        return null
    }

    private fun decodeComponentSafe(value: String): String =
        runCatching { value.decodeURLQueryComponent() }.getOrDefault(value)
}
