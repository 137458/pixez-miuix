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
 * 1. 完整解析 Pixiv 返回的 HTML 格式简介（包括 `<a href="...">`、`<br>`、`<p>`、`<b>`、`<i>` 以及 HTML 实体）。
 * 2. 自动识别未包裹在标签中的裸写 URL 并赋予可点击链接能力。
 * 3. 采用 Compose Multiplatform 现代 [LinkAnnotation.Url] 机制，支持视觉下划线与高亮反馈。
 * 4. 智能路由分发：
 *    - `pixiv://users/{id}` 或 `pixiv.net/users/{id}` -> 跳转画师主页 [onUserClick]
 *    - `pixiv://illusts/{id}` 或 `pixiv.net/artworks/{id}` -> 跳转画作详情 [onIllustClick]
 *    - 外部 Web URL -> 调用 [onLinkClick]（默认 [openBrowser]）
 * 5. 支持长文案折叠/展开与文本框长按选取复制（[SelectionContainer]）。
 */
@Composable
fun HtmlCaptionText(
    html: String,
    modifier: Modifier = Modifier,
    onUserClick: ((Int) -> Unit)? = null,
    onIllustClick: ((Int) -> Unit)? = null,
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

    val linkClickListener = remember(onUserClick, onIllustClick, onLinkClick) {
        { rawUrl: String ->
            handleCaptionLink(
                url = rawUrl,
                onUserClick = onUserClick,
                onIllustClick = onIllustClick,
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
 * 链接路由分发：优先将 Pixiv 站内深层链接重定向为 App 内部页面跳转，外部链接唤起浏览器。
 */
fun handleCaptionLink(
    url: String,
    onUserClick: ((Int) -> Unit)?,
    onIllustClick: ((Int) -> Unit)?,
    onLinkClick: (String) -> Unit,
) {
    val cleanUrl = url.trim()

    // 1. Pixiv User 协议或 Web 用户主页链接
    val userMatch = Regex("""(?:pixiv://users/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?users/)(\d+)""")
        .find(cleanUrl)
    if (userMatch != null) {
        val userId = userMatch.groupValues[1].toIntOrNull()
        if (userId != null && onUserClick != null) {
            onUserClick(userId)
            return
        }
    }

    // 2. Pixiv Illust 协议或 Web 画作详情链接
    val illustMatch = Regex("""(?:pixiv://illusts/|https?://(?:www\.)?pixiv\.net/(?:[a-zA-Z_-]+/)?artworks/)(\d+)""")
        .find(cleanUrl)
    if (illustMatch != null) {
        val illustId = illustMatch.groupValues[1].toIntOrNull()
        if (illustId != null && onIllustClick != null) {
            onIllustClick(illustId)
            return
        }
    }

    // 3. 通用外部链接
    onLinkClick(cleanUrl)
}

/**
 * 解析 Pixiv HTML 简介文案为带有 [LinkAnnotation.Url] 的 [AnnotatedString]。
 */
fun parseHtmlCaption(
    html: String,
    linkColor: Color,
    onLinkClick: (String) -> Unit,
): AnnotatedString {
    // 1. 规范化换行标签与段落
    val preprocessed = html
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""<p[^>]*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()

    // 2. 构造分词正则：匹配 <a href="...">...</a>、<b>...</b>、<i>...</i> 及普通文本
    val tagPattern = Regex(
        """<a\s+[^>]*href=["']([^"']*)["'][^>]*>(.*?)</a>|<(b|strong)>(.*?)</\3>|<(i|em)>(.*?)</\5>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium,
        )
    )

    return buildAnnotatedString {
        var lastIndex = 0
        tagPattern.findAll(preprocessed).forEach { matchResult ->
            val range = matchResult.range

            // 匹配标签前的普通文本（可能包含裸写 URL）
            if (range.first > lastIndex) {
                val plainChunk = preprocessed.substring(lastIndex, range.first)
                appendPlainChunkWithAutoLinks(
                    chunk = plainChunk,
                    linkStyles = linkStyles,
                    onLinkClick = onLinkClick,
                )
            }

            val href = matchResult.groups[1]?.value
            val anchorText = matchResult.groups[2]?.value
            val isBold = matchResult.groups[3] != null
            val boldText = matchResult.groups[4]?.value
            val isItalic = matchResult.groups[5] != null
            val italicText = matchResult.groups[6]?.value

            when {
                href != null && anchorText != null -> {
                    val decodedText = decodeHtmlEntities(anchorText)
                    withLink(
                        LinkAnnotation.Url(
                            url = href,
                            styles = linkStyles,
                            linkInteractionListener = { annotation ->
                                val target = (annotation as? LinkAnnotation.Url)?.url ?: href
                                onLinkClick(target)
                            }
                        )
                    ) {
                        append(decodedText)
                    }
                }
                isBold && boldText != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(decodeHtmlEntities(boldText))
                    }
                }
                isItalic && italicText != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(decodeHtmlEntities(italicText))
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

/**
 * 将普通文本片段中的裸写 URL 自动识别为超链接并追加至 [AnnotatedString.Builder]。
 */
private fun AnnotatedString.Builder.appendPlainChunkWithAutoLinks(
    chunk: String,
    linkStyles: TextLinkStyles,
    onLinkClick: (String) -> Unit,
) {
    val decoded = decodeHtmlEntities(chunk)
    val urlPattern = Regex("""https?://[^\s<>"'`()]+""")

    var lastIndex = 0
    urlPattern.findAll(decoded).forEach { matchResult ->
        val range = matchResult.range
        if (range.first > lastIndex) {
            append(decoded.substring(lastIndex, range.first))
        }

        val url = matchResult.value
        withLink(
            LinkAnnotation.Url(
                url = url,
                styles = linkStyles,
                linkInteractionListener = { annotation ->
                    val target = (annotation as? LinkAnnotation.Url)?.url ?: url
                    onLinkClick(target)
                }
            )
        ) {
            append(url)
        }

        lastIndex = range.last + 1
    }

    if (lastIndex < decoded.length) {
        append(decoded.substring(lastIndex))
    }
}

/**
 * 快速解码常见与数字类型的 HTML 实体转义字符。
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
        .replace(Regex("""&#(\d+);""")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null) {
                try {
                    code.toChar().toString()
                } catch (_: Throwable) {
                    match.value
                }
            } else match.value
        }
        .replace(Regex("""&#x([0-9a-fA-F]+);""")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null) {
                try {
                    code.toChar().toString()
                } catch (_: Throwable) {
                    match.value
                }
            } else match.value
        }
}
