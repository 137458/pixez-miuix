package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import pixez_miuix.shared.generated.resources.*

/**
 * Pixiv 官方评论表情定义与文本解析器。
 */
data class PixivEmoji(
    val code: String,
    val resource: DrawableResource,
)

object PixivEmojis {
    /**
     * 38 种官方表情列表，用于表情选择面板展示。
     */
    val allEmojis: List<PixivEmoji> = listOf(
        PixivEmoji("(normal)", Res.drawable.emoji_101),
        PixivEmoji("(surprise)", Res.drawable.emoji_102),
        PixivEmoji("(serious)", Res.drawable.emoji_103),
        PixivEmoji("(heaven)", Res.drawable.emoji_104),
        PixivEmoji("(happy)", Res.drawable.emoji_105),
        PixivEmoji("(excited)", Res.drawable.emoji_106),
        PixivEmoji("(sing)", Res.drawable.emoji_107),
        PixivEmoji("(cry)", Res.drawable.emoji_108),

        PixivEmoji("(normal2)", Res.drawable.emoji_201),
        PixivEmoji("(shame2)", Res.drawable.emoji_202),
        PixivEmoji("(love2)", Res.drawable.emoji_203),
        PixivEmoji("(interesting2)", Res.drawable.emoji_204),
        PixivEmoji("(blush2)", Res.drawable.emoji_205),
        PixivEmoji("(fire2)", Res.drawable.emoji_206),
        PixivEmoji("(angry2)", Res.drawable.emoji_207),
        PixivEmoji("(shine2)", Res.drawable.emoji_208),
        PixivEmoji("(panic2)", Res.drawable.emoji_209),

        PixivEmoji("(normal3)", Res.drawable.emoji_301),
        PixivEmoji("(satisfaction3)", Res.drawable.emoji_302),
        PixivEmoji("(surprise3)", Res.drawable.emoji_303),
        PixivEmoji("(smile3)", Res.drawable.emoji_304),
        PixivEmoji("(shock3)", Res.drawable.emoji_305),
        PixivEmoji("(gaze3)", Res.drawable.emoji_306),
        PixivEmoji("(wink3)", Res.drawable.emoji_307),
        PixivEmoji("(happy3)", Res.drawable.emoji_308),
        PixivEmoji("(excited3)", Res.drawable.emoji_309),
        PixivEmoji("(love3)", Res.drawable.emoji_310),

        PixivEmoji("(normal4)", Res.drawable.emoji_401),
        PixivEmoji("(surprise4)", Res.drawable.emoji_402),
        PixivEmoji("(serious4)", Res.drawable.emoji_403),
        PixivEmoji("(love4)", Res.drawable.emoji_404),
        PixivEmoji("(shine4)", Res.drawable.emoji_405),
        PixivEmoji("(sweat4)", Res.drawable.emoji_406),
        PixivEmoji("(shame4)", Res.drawable.emoji_407),
        PixivEmoji("(sleep4)", Res.drawable.emoji_408),

        PixivEmoji("(heart)", Res.drawable.emoji_501),
        PixivEmoji("(teardrop)", Res.drawable.emoji_502),
        PixivEmoji("(star)", Res.drawable.emoji_503),
    )

    /**
     * 包含标准名称与数字别名（如 (101)）的完整映射表。
     */
    val emojiMap: Map<String, DrawableResource> = buildMap {
        allEmojis.forEach { emoji ->
            put(emoji.code, emoji.resource)
        }
        // 数字别名兼容支持
        put("(101)", Res.drawable.emoji_101)
        put("(102)", Res.drawable.emoji_102)
        put("(103)", Res.drawable.emoji_103)
        put("(104)", Res.drawable.emoji_104)
        put("(105)", Res.drawable.emoji_105)
        put("(106)", Res.drawable.emoji_106)
        put("(107)", Res.drawable.emoji_107)
        put("(108)", Res.drawable.emoji_108)

        put("(201)", Res.drawable.emoji_201)
        put("(202)", Res.drawable.emoji_202)
        put("(203)", Res.drawable.emoji_203)
        put("(204)", Res.drawable.emoji_204)
        put("(205)", Res.drawable.emoji_205)
        put("(206)", Res.drawable.emoji_206)
        put("(207)", Res.drawable.emoji_207)
        put("(208)", Res.drawable.emoji_208)
        put("(209)", Res.drawable.emoji_209)

        put("(301)", Res.drawable.emoji_301)
        put("(302)", Res.drawable.emoji_302)
        put("(303)", Res.drawable.emoji_303)
        put("(304)", Res.drawable.emoji_304)
        put("(305)", Res.drawable.emoji_305)
        put("(306)", Res.drawable.emoji_306)
        put("(307)", Res.drawable.emoji_307)
        put("(308)", Res.drawable.emoji_308)
        put("(309)", Res.drawable.emoji_309)
        put("(310)", Res.drawable.emoji_310)

        put("(401)", Res.drawable.emoji_401)
        put("(402)", Res.drawable.emoji_402)
        put("(403)", Res.drawable.emoji_403)
        put("(404)", Res.drawable.emoji_404)
        put("(405)", Res.drawable.emoji_405)
        put("(406)", Res.drawable.emoji_406)
        put("(407)", Res.drawable.emoji_407)
        put("(408)", Res.drawable.emoji_408)

        put("(501)", Res.drawable.emoji_501)
        put("(502)", Res.drawable.emoji_502)
        put("(503)", Res.drawable.emoji_503)
    }

    /**
     * 将包含表情标识的纯文本转换为 Compose AnnotatedString 与 InlineTextContent Map。
     */
    fun parseEmojiAnnotatedString(
        text: String,
        inlineSize: TextUnit = 20.sp,
    ): Pair<AnnotatedString, Map<String, InlineTextContent>> {
        val inlineContentMap = mutableMapOf<String, InlineTextContent>()
        val annotatedString = buildAnnotatedString {
            var template = StringBuilder()
            var emojiText = StringBuilder()
            var emojiCollecting = false

            for (ch in text) {
                if (ch == '(') {
                    if (template.isNotEmpty()) {
                        append(template.toString())
                        template.clear()
                    }
                    emojiCollecting = true
                    emojiText.clear()
                } else if (ch == ')') {
                    if (emojiCollecting && emojiText.isNotEmpty()) {
                        val key = "($emojiText)"
                        val resource = emojiMap[key]
                        if (resource != null) {
                            val inlineId = key
                            if (!inlineContentMap.containsKey(inlineId)) {
                                inlineContentMap[inlineId] = InlineTextContent(
                                    Placeholder(
                                        width = inlineSize,
                                        height = inlineSize,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                    ),
                                ) {
                                    Image(
                                        painter = painterResource(resource),
                                        contentDescription = key,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            appendInlineContent(inlineId, key)
                        } else {
                            append("($emojiText)")
                        }
                    } else if (emojiCollecting) {
                        append("()")
                    } else {
                        template.append(')')
                    }
                    emojiCollecting = false
                    emojiText.clear()
                } else {
                    if (emojiCollecting) {
                        emojiText.append(ch)
                    } else {
                        template.append(ch)
                    }
                }
            }

            if (emojiCollecting && emojiText.isNotEmpty()) {
                append("($emojiText")
            } else if (template.isNotEmpty()) {
                append(template.toString())
            }
        }

        return annotatedString to inlineContentMap
    }
}
