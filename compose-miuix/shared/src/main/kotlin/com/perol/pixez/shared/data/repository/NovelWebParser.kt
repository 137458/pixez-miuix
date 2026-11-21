package com.perol.pixez.shared.data.repository

/**
 * /webview/v2/novel HTML 响应解析器（纯函数，参照旧 Flutter 版
 * parseNovelJsonFromHtml / _readBalancedJsonObject：在 script 文本里定位 "novel:"，
 * 从其后第一个 "{" 起按大括号配平提取 JSON，处理字符串内大括号与转义引号。
 */
object NovelWebParser {

    private const val NOVEL_KEY = "novel:"

    /**
     * 从 webview HTML 中提取内嵌 novel JSON 字符串；找不到 "novel:" 或无法配平时返回 null。
     *
     * 与旧版的差异：仅当 "novel:"（跳过空白）后紧跟 "{" 才按配平提取，
     * 否则视为误匹配继续向后查找，避免 "novel:null,marker:{...}" 提到错误对象。
     */
    fun extractNovelJsonFromHtml(html: String): String? {
        var searchFrom = 0
        while (true) {
            val novelIndex = html.indexOf(NOVEL_KEY, searchFrom)
            if (novelIndex == -1) return null

            var cursor = novelIndex + NOVEL_KEY.length
            while (cursor < html.length && html[cursor].isWhitespace()) cursor++
            if (cursor >= html.length || html[cursor] != '{') {
                searchFrom = novelIndex + 1
                continue
            }

            val extracted = readBalancedJsonObject(html, cursor)
            if (extracted != null) return extracted
            searchFrom = cursor + 1
        }
    }

    /**
     * 从 [start]（必须指向 "{"）起按大括号配平读取完整 JSON 对象；
     * 字符串内的大括号、转义引号与转义反斜杠不参与配平；未配平返回 null。
     */
    private fun readBalancedJsonObject(source: String, start: Int): String? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until source.length) {
            val char = source[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        return null
    }
}
