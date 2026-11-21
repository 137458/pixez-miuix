package com.perol.pixez.shared.utils

import io.ktor.http.decodeURLQueryComponent

/**
 * 统一的 HTML 与 URL 解码/清洗工具类，收敛全模块重复的实体解析与标签剥离逻辑。
 */
object HtmlUtils {

    private val HTML_TAG_REGEX = Regex("""<[^>]*>""")
    private val NUMERIC_ENTITY_REGEX = Regex("""&#([0-9]+);|&#x([0-9a-fA-F]+);""")

    /**
     * 剥离 HTML 标签，返回纯文本内容。
     */
    fun stripHtmlTags(input: String): String {
        if (!input.contains('<')) return input
        return input.replace(HTML_TAG_REGEX, "")
    }

    /**
     * 解码 HTML 命名实体与十进制/十六进制数值实体（包含 BMP 与高平面 Supplementary 字符）。
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
            .replace("&#039;", "'")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
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
            .replace(NUMERIC_ENTITY_REGEX) { match ->
                val decCode = match.groups[1]?.value?.toIntOrNull()
                val hexCode = match.groups[2]?.value?.toIntOrNull(16)
                val code = decCode ?: hexCode
                if (code != null && code in 0..0x10FFFF) {
                    try {
                        codePointToString(code)
                    } catch (_: Throwable) {
                        match.value
                    }
                } else {
                    match.value
                }
            }
    }

    /**
     * 基于 Ktor 成熟的标准 API 解码 URL 百分号编码（如 %2F -> /，+ -> 空格）。
     */
    fun decodeUrlPercent(input: String): String {
        if (!input.contains('%') && !input.contains('+')) return input
        return runCatching {
            input.decodeURLQueryComponent(plusIsSpace = true)
        }.getOrDefault(input)
    }

    private fun codePointToString(codePoint: Int): String {
        return if (codePoint <= 0xFFFF) {
            codePoint.toChar().toString()
        } else {
            val high = ((codePoint - 0x10000) ushr 10) + 0xD800
            val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
            charArrayOf(high.toChar(), low.toChar()).concatToString()
        }
    }
}
