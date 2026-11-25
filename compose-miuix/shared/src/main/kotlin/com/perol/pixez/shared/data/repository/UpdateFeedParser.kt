package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.network.GitHubReleaseEndpoints
import com.perol.pixez.shared.network.TrustedUrlPolicy

/**
 * GitHub 发布源兜底解析。
 *
 * GitHub REST API 按出口 IP 限流（未认证 60 次/小时），耗尽后一律 403；
 * [GitHubReleaseEndpoints.RELEASES_ATOM] 与
 * expanded_assets 片段不带配额限制，因此这里只做纯文本解析，不引入 XML 依赖。
 */

/** releases.atom 中一条发布记录的解析结果。 */
internal data class GitHubFeedEntry(
    val tagName: String,
    val title: String,
    val releaseUrl: String,
    val publishedAt: String?,
    val changelogMarkdown: String,
)

/** 发布标签在 URL 中的路径片段。 */
private const val RELEASES_TAG_PATH = "/releases/tag/"
private const val RELEASES_DOWNLOAD_PATH = "/releases/download/"

private val ALTERNATE_LINK_REGEX =
    Regex("""<link[^>]*href="([^"]*""" + RELEASES_TAG_PATH + """[^"]*)"""")
private val ENTRY_ID_TAG_REGEX = Regex("""<id>[^<]*/([^/<]+)</id>""")
private val ENTRY_TITLE_REGEX =
    Regex("""<title\b[^>]*>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL)
private val ENTRY_UPDATED_REGEX = Regex("""<updated>([^<]*)</updated>""")
private val ENTRY_CONTENT_REGEX =
    Regex("""<content[^>]*>(.*?)</content>""", RegexOption.DOT_MATCHES_ALL)

private val HTML_H_OPEN_REGEX = Regex("""<h([1-6])[^>]*>""")
private val HTML_H_CLOSE_REGEX = Regex("""</h[1-6]>""")
private val HTML_LI_OPEN_REGEX = Regex("""<li[^>]*>""")
private val HTML_LI_CLOSE_REGEX = Regex("""</li>""")
private val HTML_LIST_WRAPPER_REGEX = Regex("""</?[uo]l[^>]*>""")
private val HTML_BR_REGEX = Regex("""<br\s*/?>""")
private val HTML_P_REGEX = Regex("""</?p[^>]*>""")
private val HTML_BOLD_REGEX = Regex("""</?(strong|b)[^>]*>""")
private val HTML_ITALIC_REGEX = Regex("""</?(em|i)[^>]*>""")
private val HTML_CODE_REGEX = Regex("""</?code[^>]*>""")
private val HTML_ANCHOR_REGEX =
    Regex("""<a\s+[^>]*href="([^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
private val HTML_ANY_TAG_REGEX = Regex("""<[^>]+>""")

private val EXPANDED_HREF_REGEX = Regex("""href="([^"]+)"""", RegexOption.IGNORE_CASE)
private val LI_TAG_OPEN_REGEX = Regex("""<li(?:\s|>)""", RegexOption.IGNORE_CASE)
private val LI_TAG_CLOSE_REGEX = Regex("""</li>""", RegexOption.IGNORE_CASE)
private val SHA256_REGEX = Regex("""sha256:([0-9a-fA-F]{64})(?![0-9a-fA-F])""", RegexOption.IGNORE_CASE)
private val ASSET_SIZE_TAG_REGEX =
    Regex(""">\s*(\d+(?:\.\d+)?)\s*(KB|MB|GB)\s*<""", RegexOption.IGNORE_CASE)
private val ASSET_SIZE_FALLBACK_REGEX =
    Regex("""(?:^|[\s(>])(\d+(?:\.\d+)?)\s*(KB|MB|GB)(?=$|[\s)<])""", RegexOption.IGNORE_CASE)

private val DECIMAL_ENTITY_REGEX = Regex("""&#(\d+);""")
private val HEX_ENTITY_REGEX = Regex("""&#x([0-9a-fA-F]+);""")

/**
 * 解析 releases.atom 的第一条 entry（feed 按发布时间倒序，首条即最新发布）。
 * 无法解析出合法单段标签时返回 null，由调用方转成明确的失败原因。
 */
internal fun parseLatestFeedEntry(atomXml: String): GitHubFeedEntry? {
    val entryStart = atomXml.indexOf("<entry")
    if (entryStart < 0) return null
    val entryEnd = atomXml.indexOf("</entry>", entryStart)
    val block = if (entryEnd < 0) {
        atomXml.substring(entryStart)
    } else {
        atomXml.substring(entryStart, entryEnd + "</entry>".length)
    }

    val rawAlternateUrl = ALTERNATE_LINK_REGEX
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.let { decodeXmlEntities(it).trim() }
        ?.takeIf { it.isNotBlank() }
    val trustedAlternateUrl = rawAlternateUrl?.let { candidate ->
        runCatching {
            TrustedUrlPolicy.officialReleasePageUrl(candidate, GitHubReleaseEndpoints.OFFICIAL_REPO_PATH)
        }.getOrNull()?.takeIf { it.contains(RELEASES_TAG_PATH) }
    }

    val tagFromAlternate = trustedAlternateUrl
        ?.substringAfter(RELEASES_TAG_PATH, "")
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.substringBefore('/')
        ?.let { extractSafeTagSegmentOrNull(it) }
    val tagFromId = ENTRY_ID_TAG_REGEX
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.let { decodeXmlEntities(it) }
        ?.let { extractSafeTagSegmentOrNull(it) }
    val tagName = tagFromAlternate ?: tagFromId ?: return null

    val title = ENTRY_TITLE_REGEX
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.let { decodeXmlEntities(it) }
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: ("PixEz MIUIX $tagName")
    val publishedAt = ENTRY_UPDATED_REGEX
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val changelog = ENTRY_CONTENT_REGEX
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.let { htmlFragmentToMarkdown(decodeXmlEntities(it)) }
        ?: ""

    return GitHubFeedEntry(
        tagName = tagName,
        title = title,
        releaseUrl = trustedAlternateUrl ?: GitHubReleaseEndpoints.releaseTagUrl(tagName),
        publishedAt = publishedAt,
        changelogMarkdown = changelog,
    )
}

private fun extractSafeTagSegmentOrNull(raw: String): String? =
    runCatching { GitHubReleaseEndpoints.requireSafeTagSegment(raw) }.getOrNull()

/**
 * 把 GitHub 发布说明的 HTML 片段转换为 Markdown。
 *
 * 只覆盖 Release 正文常见的结构（标题、列表、段落、行内代码、链接、强调），
 * 输出直接喂给 [com.perol.pixez.shared.ui.components.MarkdownText]。
 */
internal fun htmlFragmentToMarkdown(html: String): String {
    if (html.isBlank()) return ""
    var text = html
    text = HTML_H_OPEN_REGEX.replace(text) { match ->
        "\n" + "#".repeat(match.groupValues[1].toIntOrNull() ?: 2) + " "
    }
    text = HTML_H_CLOSE_REGEX.replace(text, "\n")
    text = HTML_LI_OPEN_REGEX.replace(text, "\n- ")
    text = HTML_LI_CLOSE_REGEX.replace(text, "\n")
    text = HTML_LIST_WRAPPER_REGEX.replace(text, "\n")
    text = HTML_BR_REGEX.replace(text, "\n")
    text = HTML_P_REGEX.replace(text, "\n")
    text = HTML_BOLD_REGEX.replace(text, "**")
    text = HTML_ITALIC_REGEX.replace(text, "*")
    text = HTML_CODE_REGEX.replace(text, "`")
    text = HTML_ANCHOR_REGEX.replace(text) { match ->
        val label = match.groupValues[2].trim()
        "[" + label + "](" + match.groupValues[1].trim() + ")"
    }
    text = HTML_ANY_TAG_REGEX.replace(text, "")
    return decodeHtmlEntities(text)
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}

/**
 * 解析 expanded_assets 片段，取出首个 apk 资产直链。
 *
 * 该片段是 GitHub 发布页资产列表的独立 HTML 分片（无配额、无需登录），
 * 文件名与摘要都在同一个列表项里，因此按列表项窗口取摘要；找不到列表项时绝不回退扫描整页。
 */
internal fun parseExpandedAssetApk(html: String): GitHubReleaseAsset? {
    if (html.isBlank()) return null
    val matched = EXPANDED_HREF_REGEX.findAll(html).firstNotNullOfOrNull { match ->
        val decodedHref = decodeHtmlEntities(match.groupValues[1]).trim()
        if (decodedHref.isBlank() || decodedHref.startsWith("//")) return@firstNotNullOfOrNull null
        val pathWithoutQuery = decodedHref.substringBefore('#').substringBefore('?')
        if (!pathWithoutQuery.contains(RELEASES_DOWNLOAD_PATH) ||
            !pathWithoutQuery.endsWith(".apk", ignoreCase = true)
        ) {
            return@firstNotNullOfOrNull null
        }
        match to decodedHref
    } ?: return null

    val (anchor, href) = matched
    val absoluteUrl = when {
        href.startsWith("https://", ignoreCase = true) || href.startsWith("http://", ignoreCase = true) -> href
        href.startsWith("/") -> GitHubReleaseEndpoints.WEB_ORIGIN + href
        else -> GitHubReleaseEndpoints.WEB_ORIGIN + "/" + href
    }
    val name = absoluteUrl
        .substringBefore('#')
        .substringBefore('?')
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() }
        ?: return null

    val itemStart = LI_TAG_OPEN_REGEX
        .findAll(html.substring(0, anchor.range.first))
        .lastOrNull()
        ?.range
        ?.first
    val itemEnd = LI_TAG_CLOSE_REGEX.find(html, anchor.range.last)?.range?.first
    val item = if (itemStart != null && itemEnd != null && itemStart < itemEnd) {
        html.substring(itemStart, itemEnd)
    } else {
        null
    }
    val digest = item?.let { block ->
        SHA256_REGEX
            .find(block)
            ?.groupValues
            ?.get(1)
            ?.let { "sha256:" + it.lowercase() }
    }
    return GitHubReleaseAsset(
        name = name,
        browser_download_url = absoluteUrl,
        size = item?.let(::parseAssetSizeBytes),
        digest = digest,
    )
}

/** 解析资产列表中的体积文本（如 8.04 MB），无法解析时返回 null。 */
private fun parseAssetSizeBytes(item: String): Long? {
    val match = ASSET_SIZE_TAG_REGEX.find(item)
        ?: ASSET_SIZE_FALLBACK_REGEX.find(item)
        ?: return null
    val value = match.groupValues[1].toDoubleOrNull() ?: return null
    val factor = when (match.groupValues[2].uppercase()) {
        "KB" -> 1024.0
        "MB" -> 1024.0 * 1024.0
        "GB" -> 1024.0 * 1024.0 * 1024.0
        else -> return null
    }
    return (value * factor).toLong().takeIf { it > 0L }
}

/** 解码 XML/HTML 实体（&amp; 放最后，避免二次解码）。 */
private fun decodeXmlEntities(raw: String): String = decodeHtmlEntities(raw)

private fun decodeHtmlEntities(raw: String): String {
    var text = raw
    text = text.replace("&lt;", "<").replace("&gt;", ">")
    text = text.replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
    text = text.replace("&nbsp;", " ")
    text = DECIMAL_ENTITY_REGEX.replace(text) { match ->
        decodeCodePointOrKeep(match.groupValues[1].toIntOrNull(), match.value)
    }
    text = HEX_ENTITY_REGEX.replace(text) { match ->
        decodeCodePointOrKeep(match.groupValues[1].toIntOrNull(16), match.value)
    }
    return text.replace("&amp;", "&")
}

private fun decodeCodePointOrKeep(codePoint: Int?, original: String): String {
    if (codePoint == null || !Character.isValidCodePoint(codePoint) || codePoint in 0xD800..0xDFFF) {
        return original
    }
    return String(Character.toChars(codePoint))
}
