package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 支持 Pixiv 行内表情图文混排渲染的评论文本组件。
 */
@Composable
fun CommentEmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MiuixTheme.textStyles.body2,
    color: Color = MiuixTheme.colorScheme.onSurface,
    emojiSize: TextUnit = 20.sp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val (annotatedString, inlineContent) = remember(text, emojiSize) {
        PixivEmojis.parseEmojiAnnotatedString(text, emojiSize)
    }

    BasicText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = color),
        inlineContent = inlineContent,
        maxLines = maxLines,
        overflow = overflow,
    )
}
