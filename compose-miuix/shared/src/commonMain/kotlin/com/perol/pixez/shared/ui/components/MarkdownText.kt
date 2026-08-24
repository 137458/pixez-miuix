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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildAnnotatedContent(block.text),
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

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 8.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.primary),
                        )
                        Text(
                            text = buildAnnotatedContent(block.text),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "${block.number}. ",
                            style = MiuixTheme.textStyles.body2.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            text = buildAnnotatedContent(block.text),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedContent(block.text),
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = baseFontSize.sp,
                            lineHeight = (baseFontSize + 6).sp,
                        ),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }

                is MarkdownBlock.Divider -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(0.5.dp)
                            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    object Divider : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
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

private fun buildAnnotatedContent(rawText: String): AnnotatedString {
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

            append(rawText[i])
            i++
        }
    }
}
