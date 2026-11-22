package com.perol.pixez.shared.data.repository

/**
 * GitHub 发布源兜底解析。
 *
 * GitHub REST API 按出口 IP 限流（未认证 60 次/小时），耗尽后一律 403；
 * [com.perol.pixez.shared.network.GitHubReleaseEndpoints.RELEASES_ATOM] 与
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

/**
 * 解析 releases.atom 的第一条 entry（feed 按发布时间倒序，首条即最新发布）。
 * 无法解析出标签时返回 null，由调用方转成明确的失败原因。
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

    val alternateUrl = Regex("""<link[^>]*href="([^"]*""" + RELEASES_TAG_PATH + """[^"]*)"""")
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val tagName = alternateUrl
        ?.substringAfter(RELEASES_TAG_PATH, "")
        ?.takeIf { it.isNotBlank() }
        ?: Regex("""<id>[^<]*/([^/<]+)</id>""")
            .find(block)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: return null

    val title = Regex("""<title>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL)
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.let { decodeXmlEntities(it) }
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: ("PixEz MIUIX " + tagName)
    val publishedAt = Regex("""<updated>([^<]*)</updated>""")
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val changelog = Regex("""<content[^>]*>(.*?)</content>""", RegexOption.DOT_MATCHES_ALL)
        .find(block)
        ?.groupValues
        ?.get(1)
        ?.let { htmlFragmentToMarkdown(decodeXmlEntities(it)) }
        ?: ""

    return GitHubFeedEntry(
        tagName = tagName,
        title = title,
        releaseUrl = alternateUrl
            ?: ("https://github.com/" + com.perol.pixez.shared.network.GitHubReleaseEndpoints.OFFICIAL_REPO_PATH + RELEASES_TAG_PATH + tagName),
        publishedAt = publishedAt,
        changelogMarkdown = changelog,
    )
}

/**
 * 把 GitHub 发布说明的 HTML 片段转换为 Markdown。
 *
 * 只覆盖 Release 正文常见的结构（标题、列表、段落、行内代码、链接、强调），
 * 输出直接喂给 [com.perol.pixez.shared.ui.components.MarkdownText]。
 */
internal fun htmlFragmentToMarkdown(html: String): String {
    if (html.isBlank()) return ""
    var text = html
    text = Regex("""<h([1-6])[^>]*>""").replace(text) { match ->
        "\n" + "#".repeat(match.groupValues[1].toIntOrNull() ?: 2) + " "
    }
    text = Regex("""</h[1-6]>""").replace(text, "\n")
    text = Regex("""<li[^>]*>""").replace(text, "\n- ")
    text = Regex("""</li>""").replace(text, "\n")
    text = Regex("""</?[uo]l[^>]*>""").replace(text, "\n")
    text = Regex("""<br\s*/?>""").replace(text, "\n")
    text = Regex("""</?p[^>]*>""").replace(text, "\n")
    text = Regex("""</?(strong|b)[^>]*>""").replace(text, "**")
    text = Regex("""</?(em|i)[^>]*>""").replace(text, "*")
    text = Regex("""</?code[^>]*>""").replace(text, "`")
    text = Regex("""<a\s+[^>]*href="([^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).replace(text) { match ->
        val label = match.groupValues[2].trim()
        "[" + label + "](" + match.groupValues[1].trim() + ")"
    }
    text = Regex("""<[^>]+>""").replace(text, "")
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
 * 文件名与摘要都在同一个列表项里，因此按列表项窗口取摘要。
 */
internal fun parseExpandedAssetApk(html: String): GitHubReleaseAsset? {
    if (html.isBlank()) return null
    val anchor = Regex("""href="([^"]*?/releases/download/[^"]*?\.apk)"""", RegexOption.IGNORE_CASE)
        .find(html)
        ?: return null
    val href = anchor.groupValues[1].trim()
    if (href.isBlank()) return null
    val absoluteUrl = when {
        href.startsWith("https://") || href.startsWith("http://") -> href
        href.startsWith("/") -> "https://github.com" + href
        else -> "https://github.com/" + href
    }
    val name = absoluteUrl.substringBefore('?').substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null

    val itemStart = html.lastIndexOf("<li", anchor.range.first)
    val itemEnd = html.indexOf("</li>", anchor.range.last).let { if (it < 0) html.length else it }
    val item = if (itemStart >= 0 && itemStart < itemEnd) html.substring(itemStart, itemEnd) else html
    val digest = Regex("""sha256:([0-9a-fA-F]{64})""")
        .find(item)
        ?.groupValues
        ?.get(1)
        ?.let { "sha256:" + it.lowercase() }
    return GitHubReleaseAsset(
        name = name,
        browser_download_url = absoluteUrl,
        size = parseAssetSizeBytes(item),
        digest = digest,
    )
}

/** 解析资产列表中的体积文本（如 8.04 MB），无法解析时返回 null。 */
private fun parseAssetSizeBytes(item: String): Long? {
    val match = Regex("""(\d+(?:\.\d+)?)\s*(KB|MB|GB)""", RegexOption.IGNORE_CASE).find(item) ?: return null
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
    text = Regex("""&#(\d+);""").replace(text) { match ->
        match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
    }
    text = Regex("""&#x([0-9a-fA-F]+);""").replace(text) { match ->
        match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
    }
    return text.replace("&amp;", "&")
}
