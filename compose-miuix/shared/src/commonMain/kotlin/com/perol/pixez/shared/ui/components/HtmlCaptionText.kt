package com.perol.pixez.shared.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.platform.openBrowser
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作品简介与文案富文本渲染组件。
 *
 * 核心特性：
 * 1. 完整解析 Pixiv 格式简介（包括 `<a href="...">`、`<br>`、`<p>`、`<b>`、`<i>`、`<u>`、`<s>` 以及 HTML 实体转义）。
 * 2. 自动识别纯文本中的裸写 URL、Pixiv 快捷 ID（illust_id、user_id、series_id、novel_id 等）并赋予可点击超链接能力。
 * 3. 智能解包 Pixiv 出站跳转链接（`jump.php?url=...`），直达目标地址并消除中间跳转拦截。
 * 4. 采用 Compose Multiplatform 现代 [LinkAnnotation.Url] 机制，支持视觉下划线、悬停与点击反馈。
 * 5. 智能路由分发：
 *    - 画师主页：`pixiv://users/{id}`、`pixiv.net/users/{id}`、`user_id=123` 等 -> [onUserClick]
 *    - 插画详情：`pixiv://illusts/{id}`、`pixiv.net/artworks/{id}`、`illust_id=123` 等 -> [onIllustClick]
 *    - 插画系列：`pixiv://series/{id}`、`pixiv.net/user/{uid}/series/{id}` 等 -> [onIllustSeriesClick]
 *    - 小说详情：`pixiv://novels/{id}`、`pixiv.net/novel/show.php?id={id}` 等 -> [onNovelClick]
 *    - 标签搜索：`pixiv://tags/{tag}`、`pixiv.net/tags/{tag}` 等 -> [onTagClick]
 *    - 外部 Web URL -> 调用 [onLinkClick]（默认 [openBrowser]）
 * 6. 支持长文案折叠/展开与文本框长按选取复制（[SelectionContainer]）。
 */
@Composable
fun HtmlCaptionText(
    html: String,
    modifier: Modifier = Modifier,
    onUserClick: ((Int) -> Unit)? = null,
    onIllustClick: ((Int) -> Unit)? = null,
    onIllustSeriesClick: ((Int) -> Unit)? = null,
    onNovelClick: ((Int) -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    onLinkClick: (String) -> Unit = { openBrowser(it) },
    style: TextStyle = MiuixTheme.textStyles.body2,
    color: Color = MiuixTheme.colorScheme.onSurface,
    linkColor: Color = MiuixTheme.colorScheme.primary,
    collapsible: Boolean = true,
    collapsedMaxLines: Int = 4,
    selectable: Boolean = true,
) {
    if (html.isBlank()) return

    val strings = LocalStrings.current
    var isExpanded by remember { mutableStateOf(false) }

    val linkClickListener = remember(onUserClick, onIllustClick, onIllustSeriesClick, onNovelClick, onTagClick, onLinkClick) {
        { rawUrl: String ->
            handleCaptionLink(
                url = rawUrl,
                onUserClick = onUserClick,
                onIllustClick = onIllustClick,
                onIllustSeriesClick = onIllustSeriesClick,
                onNovelClick = onNovelClick,
                onTagClick = onTagClick,
                onLinkClick = onLinkClick,
            )
        }
    }

    val annotatedString = remember(html, linkColor, linkClickListener) {
        parseHtmlCaption(
            html = html,
            linkColor = linkColor,
            onLinkClick = linkClickListener,
        )
    }

    // 计算是否有较多行数可能需要折叠控制
    val isLongText = remember(annotatedString) {
        annotatedString.text.lines().size > collapsedMaxLines || annotatedString.text.length > 200
    }

    Column(modifier = modifier.animateContentSize()) {
        val textComposable = @Composable {
            Text(
                text = annotatedString,
                style = style,
                color = color,
                maxLines = if (collapsible && !isExpanded && isLongText) collapsedMaxLines else Int.MAX_VALUE,
                overflow = if (collapsible && !isExpanded && isLongText) TextOverflow.Ellipsis else TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (selectable) {
            SelectionContainer {
                textComposable()
            }
        } else {
            textComposable()
        }

        if (collapsible && isLongText) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = if (isExpanded) strings.collapse else strings.expand,
                    style = MiuixTheme.textStyles.footnote1,
                    color = linkColor,
                )
            }
        }
    }
}

/**
 * 解码 URL 百分号编码（例如 `%2F` -> `/`），纯 Kotlin 实现以支持跨平台。
 */
fun decodeUrlPercent(input: String): String {
    if (!input.contains('%') && !input.contains('+')) return input
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < input.length) {
        val c = input[i]
        when (c) {
            '+' -> {
                bytes.add(' '.code.toByte())
                i++
            }
            '%' -> {
                if (i + 2 < input.length) {
                    val hex = input.substring(i + 1, i + 3)
                    val b = hex.toIntOrNull(16)
                    if (b != null) {
                        bytes.add(b.toByte())
                        i += 3
                        continue
                    }
                }
                bytes.addAll(c.toString().encodeToByteArray().toList())
                i++
            }
            else -> {
                bytes.addAll(c.toString().encodeToByteArray().toList())
                i++
            }
        }
    }
    return try {
        bytes.toByteArray().decodeToString()
    } catch (_: Throwable) {
        input
    }
}

/**
 * 解包 Pixiv 外链跳转地址（jump.php），提取出真实目标 URL 并解码。
 */
fun unwrapPixivJumpUrl(url: String): String {
    val jumpIndex = url.indexOf("jump.php")
    if (jumpIndex < 0) return url
    val queryIndex = url.indexOf('?', jumpIndex)
    if (queryIndex < 0 || queryIndex + 1 >= url.length) return url
    val queryString = url.substring(queryIndex + 1)
    val rawTarget = if (queryString.startsWith("url=", ignoreCase = true)) {
        queryString.substring(4)
    } else {
        queryString
    }
    val decoded = decodeUrlPercent(rawTarget).trim()
    return if (decoded.isNotBlank()) decoded else url
}

/**
 * 链接路由分发：优先将 Pixiv 站内深层链接重定向为 App 内部页面跳转，外部链接唤起浏览器。
 */
fun handleCaptionLink(
    url: String,
    onUserClick: ((Int) -> Unit)? = null,
    onIllustClick: ((Int) -> Unit)? = null,
    onIllustSeriesClick: ((Int) -> Unit)? = null,
    onNovelClick: ((Int) -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    onLinkClick: (String) -> Unit = { openBrowser(it) },
) {
    val cleanUrl = unwrapPixivJumpUrl(url.trim())

    // 1. Pixiv User 协议或 Web 用户主页链接
    val userMatch = Regex(
        """(?:pixiv://(?:users?|members?)/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?(?:users/|u/))(\d+)""" +
        """|https?://(?:www\.)?pixiv\.net/member\.php\?(?:[^#]*&)?id=(\d+)""",
        RegexOption.IGNORE_CASE
    ).find(cleanUrl)
    if (userMatch != null) {
        val userId = (userMatch.groups[1]?.value ?: userMatch.groups[2]?.value)?.toIntOrNull()
        if (userId != null && onUserClick != null) {
            onUserClick(userId)
            return
        }
    }

    // 2. Pixiv Illust 协议或 Web 画作详情链接
    val illustMatch = Regex(
        """(?:pixiv://(?:illusts?|artworks)/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?(?:artworks/|i/))(\d+)""" +
        """|https?://(?:www\.)?pixiv\.net/member_illust\.php\?(?:[^#]*&)?illust_id=(\d+)""",
        RegexOption.IGNORE_CASE
    ).find(cleanUrl)
    if (illustMatch != null) {
        val illustId = (illustMatch.groups[1]?.value ?: illustMatch.groups[2]?.value)?.toIntOrNull()
        if (illustId != null && onIllustClick != null) {
            onIllustClick(illustId)
            return
        }
    }

    // 3. Pixiv 系列（Series）链接
    val seriesMatch = Regex(
        """(?:pixiv://(?:illust/)?series/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?(?:user/\d+/series/|series/))(\d+)""",
        RegexOption.IGNORE_CASE
    ).find(cleanUrl)
    if (seriesMatch != null) {
        val seriesId = seriesMatch.groupValues[1].toIntOrNull()
        if (seriesId != null && onIllustSeriesClick != null) {
            onIllustSeriesClick(seriesId)
            return
        }
    }

    // 4. Pixiv 小说（Novel）链接
    val novelMatch = Regex(
        """(?:pixiv://novels?/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?novels/)(\d+)""" +
        """|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?novel/show\.php\?(?:[^#]*&)?id=(\d+)""",
        RegexOption.IGNORE_CASE
    ).find(cleanUrl)
    if (novelMatch != null) {
        val novelId = (novelMatch.groups[1]?.value ?: novelMatch.groups[2]?.value)?.toIntOrNull()
        if (novelId != null && onNovelClick != null) {
            onNovelClick(novelId)
            return
        }
    }

    // 5. Pixiv 标签（Tag）链接
    val tagMatch = Regex(
        """(?:pixiv://tags/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?tags/)([^/?#]+)""",
        RegexOption.IGNORE_CASE
    ).find(cleanUrl)
    if (tagMatch != null) {
        val rawTag = decodeUrlPercent(tagMatch.groupValues[1])
        if (rawTag.isNotBlank() && onTagClick != null) {
            onTagClick(rawTag)
            return
        }
    }

    // 6. 通用外部链接与协议补全
    val targetUrl = when {
        cleanUrl.startsWith("http://", ignoreCase = true) ||
        cleanUrl.startsWith("https://", ignoreCase = true) ||
        cleanUrl.startsWith("mailto:", ignoreCase = true) -> cleanUrl
        cleanUrl.startsWith("www.", ignoreCase = true) ||
        cleanUrl.contains(".com", ignoreCase = true) ||
        cleanUrl.contains(".net", ignoreCase = true) ||
        cleanUrl.contains(".org", ignoreCase = true) ||
        cleanUrl.contains(".jp", ignoreCase = true) ||
        cleanUrl.contains(".cc", ignoreCase = true) ||
        cleanUrl.contains(".me", ignoreCase = true) ||
        cleanUrl.contains(".pm", ignoreCase = true) ||
        cleanUrl.contains(".tv", ignoreCase = true) -> "https://$cleanUrl"
        else -> cleanUrl
    }
    onLinkClick(targetUrl)
}

/**
 * 解析 Pixiv HTML 简介文案为带有 [LinkAnnotation.Url] 的 [AnnotatedString]。
 */
fun parseHtmlCaption(
    html: String,
    linkColor: Color,
    onLinkClick: (String) -> Unit,
): AnnotatedString {
    // 1. 规范化换行标签、段落与块级标签
    val preprocessed = html
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</(?:p|div|li|h[1-6])>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""<(?:p|div|li|h[1-6])[^>]*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()

    // 2. 构造分词正则：匹配 <a href="...">...</a>、<b>...</b>、<i>...</i>、<u>...</u>、<s>...</s> 及其他未识别 HTML 标签
    val tagPattern = Regex(
        """<a\s+[^>]*href=(?:["']([^"']*)["']|([^\s>]+))[^>]*>(.*?)</a>""" +
        """|<(b|strong)>(.*?)</\3>""" +
        """|<(i|em)>(.*?)</\5>""" +
        """|<(u|ins)>(.*?)</\7>""" +
        """|<(s|strike|del)>(.*?)</\9>""" +
        """|<[^>]+>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium,
        ),
        hoveredStyle = SpanStyle(
            color = linkColor.copy(alpha = 0.8f),
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium,
        ),
        pressedStyle = SpanStyle(
            color = linkColor.copy(alpha = 0.6f),
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium,
        ),
    )

    return buildAnnotatedString {
        var lastIndex = 0
        tagPattern.findAll(preprocessed).forEach { matchResult ->
            val range = matchResult.range

            // 匹配标签前的普通文本（可能包含裸写 URL 或 Pixiv ID 快捷方式）
            if (range.first > lastIndex) {
                val plainChunk = preprocessed.substring(lastIndex, range.first)
                appendPlainChunkWithAutoLinks(
                    chunk = plainChunk,
                    linkStyles = linkStyles,
                    onLinkClick = onLinkClick,
                )
            }

            val href = matchResult.groups[1]?.value ?: matchResult.groups[2]?.value
            val anchorText = matchResult.groups[3]?.value
            val isBold = matchResult.groups[4] != null
            val boldText = matchResult.groups[5]?.value
            val isItalic = matchResult.groups[6] != null
            val italicText = matchResult.groups[7]?.value
            val isUnderline = matchResult.groups[8] != null
            val underlineText = matchResult.groups[9]?.value
            val isStrike = matchResult.groups[10] != null
            val strikeText = matchResult.groups[11]?.value

            when {
                href != null && anchorText != null -> {
                    // 如果 anchorText 中含有嵌套标签，清除标签后解码实体
                    val cleanAnchorText = decodeHtmlEntities(stripHtmlTags(anchorText))
                    val unwrappedHref = unwrapPixivJumpUrl(href)
                    withLink(
                        LinkAnnotation.Url(
                            url = unwrappedHref,
                            styles = linkStyles,
                            linkInteractionListener = { annotation ->
                                val target = (annotation as? LinkAnnotation.Url)?.url ?: unwrappedHref
                                onLinkClick(target)
                            }
                        )
                    ) {
                        append(cleanAnchorText)
                    }
                }
                isBold && boldText != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(decodeHtmlEntities(stripHtmlTags(boldText)))
                    }
                }
                isItalic && italicText != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(decodeHtmlEntities(stripHtmlTags(italicText)))
                    }
                }
                isUnderline && underlineText != null -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(decodeHtmlEntities(stripHtmlTags(underlineText)))
                    }
                }
                isStrike && strikeText != null -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(decodeHtmlEntities(stripHtmlTags(strikeText)))
                    }
                }
            }

            lastIndex = range.last + 1
        }

        // 剩余末尾文本
        if (lastIndex < preprocessed.length) {
            val remaining = preprocessed.substring(lastIndex)
            appendPlainChunkWithAutoLinks(
                chunk = remaining,
                linkStyles = linkStyles,
                onLinkClick = onLinkClick,
            )
        }
    }
}

private val AUTO_LINK_PATTERN = Regex(
    // 1. 标准 HTTP/HTTPS URL
    """(https?://[^\s<>"'`()]+)""" +
    // 2. 常见无协议域名 URL (如 twitter.com/user, fanbox.cc, pixiv.me/user, www.pixiv.net)
    """|((?:www\.|pixiv\.me/|(?:[a-zA-Z0-9-]+\.)+(?:com|net|org|jp|cc|pm|tv|io|moe|me|art)\b)(?:/[^\s<>"'`()]*)?)""" +
    // 3. Pixiv 作品 ID 缩写 (illust_id=123, illust_id: 123, illust: 123)
    """|(?:illust_id|illust)\s*[:=]\s*(\d+)""" +
    // 4. Pixiv 用户 ID 缩写 (user_id=123, uid: 123, user: 123)
    """|(?:user_id|uid|user)\s*[:=]\s*(\d+)""" +
    // 5. Pixiv 系列 ID 缩写 (series_id=123, series: 123)
    """|(?:series_id|series)\s*[:=]\s*(\d+)""" +
    // 6. Pixiv 小说 ID 缩写 (novel_id=123, novel: 123)
    """|(?:novel_id|novel)\s*[:=]\s*(\d+)""",
    RegexOption.IGNORE_CASE
)

/**
 * 清除字符串中的 HTML 标签。
 */
fun stripHtmlTags(input: String): String {
    return input.replace(Regex("""<[^>]*>"""), "")
}

/**
 * 去除 URL 末尾由于标点符号贴附而多匹配的符号（例如句子末尾句号、逗号等）。
 */
fun trimTrailingUrlPunctuation(url: String): Pair<String, String> {
    var endIndex = url.length
    while (endIndex > 0) {
        val lastChar = url[endIndex - 1]
        if (lastChar in ".,;:!?)]\"'") {
            if (lastChar == ')' && url.take(endIndex).count { it == '(' } >= url.take(endIndex).count { it == ')' }) {
                break
            }
            if (lastChar == ']' && url.take(endIndex).count { it == '[' } >= url.take(endIndex).count { it == ']' }) {
                break
            }
            endIndex--
        } else {
            break
        }
    }
    return url.substring(0, endIndex) to url.substring(endIndex)
}

/**
 * 将普通文本片段中的裸写 URL 及 Pixiv ID 自动识别为超链接并追加至 [AnnotatedString.Builder]。
 */
private fun AnnotatedString.Builder.appendPlainChunkWithAutoLinks(
    chunk: String,
    linkStyles: TextLinkStyles,
    onLinkClick: (String) -> Unit,
) {
    val decoded = decodeHtmlEntities(chunk)
    var lastIndex = 0

    AUTO_LINK_PATTERN.findAll(decoded).forEach { matchResult ->
        val range = matchResult.range
        if (range.first > lastIndex) {
            append(decoded.substring(lastIndex, range.first))
        }

        val httpUrl = matchResult.groups[1]?.value
        val domainUrl = matchResult.groups[2]?.value
        val illustId = matchResult.groups[3]?.value
        val userId = matchResult.groups[4]?.value
        val seriesId = matchResult.groups[5]?.value
        val novelId = matchResult.groups[6]?.value

        when {
            httpUrl != null -> {
                val (cleanUrl, trailing) = trimTrailingUrlPunctuation(httpUrl)
                withLink(
                    LinkAnnotation.Url(
                        url = cleanUrl,
                        styles = linkStyles,
                        linkInteractionListener = { annotation ->
                            val target = (annotation as? LinkAnnotation.Url)?.url ?: cleanUrl
                            onLinkClick(target)
                        }
                    )
                ) {
                    append(cleanUrl)
                }
                if (trailing.isNotEmpty()) {
                    append(trailing)
                }
            }
            domainUrl != null -> {
                val (cleanUrl, trailing) = trimTrailingUrlPunctuation(domainUrl)
                val target = "https://$cleanUrl"
                withLink(
                    LinkAnnotation.Url(
                        url = target,
                        styles = linkStyles,
                        linkInteractionListener = { annotation ->
                            val finalTarget = (annotation as? LinkAnnotation.Url)?.url ?: target
                            onLinkClick(finalTarget)
                        }
                    )
                ) {
                    append(cleanUrl)
                }
                if (trailing.isNotEmpty()) {
                    append(trailing)
                }
            }
            illustId != null -> {
                val target = "pixiv://illusts/$illustId"
                withLink(
                    LinkAnnotation.Url(
                        url = target,
                        styles = linkStyles,
                        linkInteractionListener = { onLinkClick(target) }
                    )
                ) {
                    append(matchResult.value)
                }
            }
            userId != null -> {
                val target = "pixiv://users/$userId"
                withLink(
                    LinkAnnotation.Url(
                        url = target,
                        styles = linkStyles,
                        linkInteractionListener = { onLinkClick(target) }
                    )
                ) {
                    append(matchResult.value)
                }
            }
            seriesId != null -> {
                val target = "pixiv://series/$seriesId"
                withLink(
                    LinkAnnotation.Url(
                        url = target,
                        styles = linkStyles,
                        linkInteractionListener = { onLinkClick(target) }
                    )
                ) {
                    append(matchResult.value)
                }
            }
            novelId != null -> {
                val target = "pixiv://novels/$novelId"
                withLink(
                    LinkAnnotation.Url(
                        url = target,
                        styles = linkStyles,
                        linkInteractionListener = { onLinkClick(target) }
                    )
                ) {
                    append(matchResult.value)
                }
            }
            else -> {
                append(matchResult.value)
            }
        }

        lastIndex = range.last + 1
    }

    if (lastIndex < decoded.length) {
        append(decoded.substring(lastIndex))
    }
}

/**
 * 快速解码常见命名与数字（十进制与十六进制）类型的 HTML 实体转义字符。
 */
fun decodeHtmlEntities(input: String): String {
    if (!input.contains('&')) return input

    return input
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace("&copy;", "©")
        .replace("&reg;", "®")
        .replace("&trade;", "™")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&bull;", "•")
        .replace("&yen;", "¥")
        .replace("&euro;", "€")
        .replace("&pound;", "£")
        .replace("&cent;", "¢")
        .replace(Regex("""&#(\d+);""")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 0..0x10FFFF) {
                try {
                    codePointToString(code)
                } catch (_: Throwable) {
                    match.value
                }
            } else match.value
        }
        .replace(Regex("""&#x([0-9a-fA-F]+);""")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 0..0x10FFFF) {
                try {
                    codePointToString(code)
                } catch (_: Throwable) {
                    match.value
                }
            } else match.value
        }
}

/**
 * 将 Unicode CodePoint 转换为 String（支持 BMP 与高平面 Supplementary 平面字符）。
 */
private fun codePointToString(codePoint: Int): String {
    return if (codePoint <= 0xFFFF) {
        codePoint.toChar().toString()
    } else {
        val high = ((codePoint - 0x10000) ushr 10) + 0xD800
        val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
        charArrayOf(high.toChar(), low.toChar()).concatToString()
    }
}

