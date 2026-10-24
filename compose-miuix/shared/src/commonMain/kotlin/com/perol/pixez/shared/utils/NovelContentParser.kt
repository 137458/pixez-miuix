package com.perol.pixez.shared.utils

/**
 * pixiv 小说正文内嵌标记解析结果（纯数据，无 Compose 依赖）。
 */
sealed class NovelSpan {
    /** 普通正文（含原始换行）。 */
    data class Text(val text: String) : NovelSpan()

    /** [chapter:标题] 章节标题。 */
    data class Chapter(val title: String) : NovelSpan()

    /** [newpage] 分页符。 */
    data object NewPage : NovelSpan()

    /**
     * [pixivimage:ID] / [pixivimage:ID-页码] 内嵌插画。
     *
     * [key] 为标记内原始内容（如 "12345" / "12345-2"），与 webview 映射里的
     * illusts 键（"12345-1" 格式）对应；page 为 0 表示标记未携带页码。
     */
    data class PixivImage(
        val illustId: Long,
        val page: Int,
        val key: String,
    ) : NovelSpan()

    /** [uploadedimage:ID] 用户上传图（images 映射键）。 */
    data class UploadedImage(val imageId: String) : NovelSpan()

    /** [[jumpuri:标题 > URL]] 超链接。 */
    data class JumpUri(
        val title: String,
        val url: String,
    ) : NovelSpan()

    /** [jump:页码] 翻页跳转标记（当前阅读器无分页概念，展示层按原始文本兜底）。 */
    data class Jump(
        val page: Int,
        val raw: String,
    ) : NovelSpan()
}

/**
 * pixiv 小说正文内嵌标记解析器（纯函数，参照旧 Flutter 版 NovelSpansGenerator.buildSpans 的
 * 括号收集循环：遇 "[" 起收集，遇 "]" 收敛单层 "[...]" 或双层 "[[...]]" 标记，
 * 未识别 / 畸形标记一律按原始文本兜底。
 */
object NovelContentParser {

    private const val PIXIV_IMAGE_PREFIX = "[pixivimage:"
    private const val UPLOADED_IMAGE_PREFIX = "[uploadedimage:"
    private const val CHAPTER_PREFIX = "[chapter:"
    private const val JUMP_PREFIX = "[jump:"
    private const val JUMP_URI_PREFIX = "[[jumpuri:"
    private const val NEW_PAGE = "[newpage]"

    /** 与旧版一致的超链接识别正则。 */
    private val linkRegex = Regex("""https?://\S+""")

    fun parse(source: String): List<NovelSpan> {
        if (source.isEmpty()) return emptyList()

        val result = mutableListOf<NovelSpan>()
        val buffer = StringBuilder()
        for (char in source) {
            when {
                char == '[' -> {
                    when {
                        // 空缓冲：开启新的标记收集。
                        buffer.isEmpty() -> buffer.append(char)
                        // 已是单 "["：拼成 "[[" 进入双层标记收集。
                        buffer.toString() == "[" -> buffer.append(char)
                        // 其余：前段正文收敛，重新开启标记收集。
                        else -> {
                            result += parseToken(buffer.toString())
                            buffer.clear()
                            buffer.append(char)
                        }
                    }
                }
                char == ']' -> {
                    if (buffer.startsWith("[[")) {
                        // 双层标记需以 "]" 隔 "]]" 收敛。
                        if (buffer.endsWith("]")) {
                            buffer.append(char)
                            result += parseToken(buffer.toString())
                            buffer.clear()
                        } else {
                            buffer.append(char)
                        }
                    } else {
                        // 单层标记遇 "]" 直接收敛。
                        buffer.append(char)
                        result += parseToken(buffer.toString())
                        buffer.clear()
                    }
                }
                else -> buffer.append(char)
            }
        }
        // 残余未闭合内容按原始文本兜底，不吞正文。
        if (buffer.isNotEmpty()) {
            result += NovelSpan.Text(buffer.toString())
        }
        return result
    }

    /** 单个 "[...]" / "[[...]]" 完整标记分类；未识别返回原始文本。 */
    private fun parseToken(token: String): NovelSpan = when {
        token == NEW_PAGE -> NovelSpan.NewPage
        token.startsWith(CHAPTER_PREFIX) -> NovelSpan.Chapter(
            title = token.removePrefix(CHAPTER_PREFIX).removeSuffix("]"),
        )
        token.startsWith(PIXIV_IMAGE_PREFIX) -> parsePixivImage(token)
        token.startsWith(UPLOADED_IMAGE_PREFIX) -> {
            val id = token.removePrefix(UPLOADED_IMAGE_PREFIX).removeSuffix("]")
            if (id.isEmpty()) NovelSpan.Text(token) else NovelSpan.UploadedImage(id)
        }
        token.startsWith(JUMP_URI_PREFIX) -> parseJumpUri(token)
        token.startsWith(JUMP_PREFIX) -> {
            val page = token.removePrefix(JUMP_PREFIX).removeSuffix("]").toIntOrNull()
            if (page == null) NovelSpan.Text(token) else NovelSpan.Jump(page, token)
        }
        else -> NovelSpan.Text(token)
    }

    /** "[pixivimage:ID]" / "[pixivimage:ID-页码]"；内容缺失或数字非法按原始文本兜底。 */
    private fun parsePixivImage(token: String): NovelSpan {
        val content = token.removePrefix(PIXIV_IMAGE_PREFIX).removeSuffix("]")
        if (content.isEmpty()) return NovelSpan.Text(token)

        val dashIndex = content.indexOf('-')
        return if (dashIndex == -1) {
            val id = content.toLongOrNull() ?: return NovelSpan.Text(token)
            NovelSpan.PixivImage(illustId = id, page = 0, key = content)
        } else {
            val id = content.substring(0, dashIndex).toLongOrNull()
                ?: return NovelSpan.Text(token)
            val page = content.substring(dashIndex + 1).toIntOrNull()
                ?: return NovelSpan.Text(token)
            NovelSpan.PixivImage(illustId = id, page = page, key = content)
        }
    }

    /** "[[jumpuri:标题 > URL]]"；无链接按原始文本兜底（对齐旧版 linkRegex 识别方式）。 */
    private fun parseJumpUri(token: String): NovelSpan {
        val content = token.removePrefix(JUMP_URI_PREFIX).removeSuffix("]]")
        val match = linkRegex.find(content) ?: return NovelSpan.Text(token)
        val title = content.substring(0, match.range.first)
            .trim()
            .trimEnd('>', '＞', ' ')
            .trim()
        return NovelSpan.JumpUri(title = title, url = match.value)
    }
}
