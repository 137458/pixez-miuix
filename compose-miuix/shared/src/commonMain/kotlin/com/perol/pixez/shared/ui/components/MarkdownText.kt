package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 针对更新日志与说明文档轻量级 Markdown 渲染器。
 * 支持标题（# ~ ####）、无序列表（- / *）、有序列表、加粗（**text**）、行内代码（`code`）及段落间距。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseFontSize: Int = 13,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val linkColor = MiuixTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val isFirst = index == 0
                    val cleanText = block.text.trim()
                    val categoryKind = detectChangelogCategory(cleanText)

                    if (!isFirst) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (categoryKind != null) {
                        // 更新分类标题（如 新增 / 优化 / 修复），渲染为醒目的 HyperOS 胶囊徽章
                        ChangelogCategoryBadge(
                            title = cleanText,
                            kind = categoryKind,
                            baseFontSize = baseFontSize,
                        )
                    } else {
                        // 常规标题（如版本号或普通章节），搭配主题色指示条与层级样式
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = (baseFontSize + 2).dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(MiuixTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = buildAnnotatedContent(block.text, linkColor),
                                modifier = Modifier.weight(1f),
                                style = when (block.level) {
                                    1 -> MiuixTheme.textStyles.title3.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (baseFontSize + 4).sp,
                                    )
                                    2 -> MiuixTheme.textStyles.title4.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (baseFontSize + 2).sp,
                                    )
                                    else -> MiuixTheme.textStyles.body1.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (baseFontSize + 1).sp,
                                    )
                                },
                                color = MiuixTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.5.dp, end = 8.dp)
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        )
                        Text(
                            text = buildAnnotatedContent(block.text, linkColor),
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6.5).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "${block.number}. ",
                            style = MiuixTheme.textStyles.body2.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6.5).sp,
                            ),
                            color = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            text = buildAnnotatedContent(block.text, linkColor),
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6.5).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedContent(block.text, linkColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = baseFontSize.sp,
                            lineHeight = (baseFontSize + 6.5).sp,
                        ),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * 更新日志分类类别（新增、优化、修复）。
 */
internal enum class CategoryKind {
    NEW,
    OPTIMIZATION,
    FIX,
}

/**
 * 检测标题是否属于标准更新日志分类。
 */
internal fun detectChangelogCategory(text: String): CategoryKind? {
    val lower = text.lowercase()
    return when {
        lower.contains("新增") || lower.contains("新特性") || lower.contains("feature") || lower == "new" -> CategoryKind.NEW
        lower.contains("优化") || lower.contains("改进") || lower.contains("improvement") || lower.contains("optimize") -> CategoryKind.OPTIMIZATION
        lower.contains("修复") || lower.contains("fix") || lower.contains("bug") -> CategoryKind.FIX
        else -> null
    }
}

/**
 * HyperOS 规范更新日志分类胶囊小标。
 */
@Composable
private fun ChangelogCategoryBadge(
    title: String,
    kind: CategoryKind,
    baseFontSize: Int,
) {
    val (badgeBg, badgeText) = when (kind) {
        CategoryKind.NEW -> {
            MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) to MiuixTheme.colorScheme.onPrimaryContainer
        }
        CategoryKind.OPTIMIZATION -> {
            MiuixTheme.colorScheme.surfaceContainerHigh to MiuixTheme.colorScheme.primary
        }
        CategoryKind.FIX -> {
            MiuixTheme.colorScheme.surfaceContainerHigh to MiuixTheme.colorScheme.onSurface
        }
    }

    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .squircleClip(6.dp)
            .background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 2.5.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body2.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (baseFontSize - 1).sp,
            ),
            color = badgeText,
        )
    }
}

internal sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    object Divider : MarkdownBlock
}

internal fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()

    for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue

        when {
            line.startsWith("---") || line.startsWith("***") || line.startsWith("___") -> {
                blocks.add(MarkdownBlock.Divider)
            }
            line.startsWith("#### ") -> {
                blocks.add(MarkdownBlock.Heading(level = 4, text = line.removePrefix("#### ").trim()))
            }
            line.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(level = 3, text = line.removePrefix("### ").trim()))
            }
            line.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(level = 2, text = line.removePrefix("## ").trim()))
            }
            line.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(level = 1, text = line.removePrefix("# ").trim()))
            }
            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                blocks.add(MarkdownBlock.BulletItem(text = line.substring(2).trim()))
            }
            line.matches(Regex("""^\d+\.\s+.*""")) -> {
                val num = line.substringBefore('.')
                val content = line.substringAfter('.').trim()
                blocks.add(MarkdownBlock.NumberedItem(number = num, text = content))
            }
            else -> {
                blocks.add(MarkdownBlock.Paragraph(text = line))
            }
        }
    }

    return blocks
}

internal fun buildAnnotatedContent(rawText: String, linkColor: Color = Color.Unspecified): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = rawText.length

        while (i < len) {
            // **加粗**
            if (rawText.startsWith("**", i)) {
                val end = rawText.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(rawText.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // `代码`
            if (rawText.startsWith("`", i)) {
                val end = rawText.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append(rawText.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            // [链接文字](url)
            if (rawText[i] == '[') {
                val linkTextEnd = rawText.indexOf("](", i + 1)
                if (linkTextEnd != -1) {
                    val urlEnd = rawText.indexOf(")", linkTextEnd + 2)
                    if (urlEnd != -1) {
                        val label = rawText.substring(i + 1, linkTextEnd)
                        withStyle(
                            SpanStyle(
                                color = if (linkColor != Color.Unspecified) linkColor else Color(0xFF2979FF),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium,
                            )
                        ) {
                            append(label)
                        }
                        i = urlEnd + 1
                        continue
                    }
                }
            }

            append(rawText[i])
            i++
        }
    }
}
