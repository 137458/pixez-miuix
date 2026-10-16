package com.perol.pixez.shared.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * SauceNAO 接口响应解析器。
 * 支持结构化 JSON 响应及 HTML 降级兜底解析。
 */
object SauceNaoParser {

    private val PIXIV_URL_REGEX = Regex("""(?:pixiv\.net/(?:artworks/|member_illust\.php\?[^"'\s]*illust_id=))(\d+)""")
    private val HTML_RESULT_REGEX = Regex("""<div class="result">([\s\S]*?)</div>\s*</div>""")
    private val HTML_IMG_REGEX = Regex("""<img[^>]+src=["']([^"']+)["']""")
    private val HTML_SIMILARITY_REGEX = Regex("""<div class="resultsimilarityinfo">([^<]+)</div>""")
    private val HTML_TITLE_REGEX = Regex("""<div class="resulttitle">([^<]+)</div>""")
    private val HTML_MEMBER_REGEX = Regex("""(?:member\.php\?id=|\/users\/)[^"'>]+">([^<]+)</a>""")

    fun parse(rawResponse: String): List<SauceNaoResult> {
        val trimmed = rawResponse.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 优先尝试 JSON 结构化解析
        val jsonResults = parseJson(trimmed)
        if (jsonResults.isNotEmpty()) {
            return jsonResults
        }

        // 降级兜底：HTML 网页解析
        return parseHtml(trimmed)
    }

    private fun parseJson(raw: String): List<SauceNaoResult> {
        return try {
            val element = Json.parseToJsonElement(raw)
            if (element !is JsonObject) return emptyList()

            val resultsArray = element["results"] as? JsonArray ?: return emptyList()
            val seen = mutableSetOf<Long>()
            val parsedList = mutableListOf<SauceNaoResult>()

            for (item in resultsArray) {
                if (item !is JsonObject) continue
                val header = item["header"]?.jsonObject
                val data = item["data"]?.jsonObject ?: continue

                val illustId = extractPixivId(data) ?: continue
                if (!seen.add(illustId)) continue

                val similarity = header?.get("similarity")?.jsonPrimitive?.content
                val thumbnail = header?.get("thumbnail")?.jsonPrimitive?.content
                val title = data["title"]?.jsonPrimitive?.content
                val author = data["member_name"]?.jsonPrimitive?.content
                    ?: data["author_name"]?.jsonPrimitive?.content
                    ?: data["author"]?.jsonPrimitive?.content

                parsedList.add(
                    SauceNaoResult(
                        illustId = illustId,
                        title = title,
                        author = author,
                        similarity = similarity,
                        thumbnail = thumbnail,
                    )
                )
            }
            parsedList
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractPixivId(data: JsonObject): Long? {
        val pixivIdElement = data["pixiv_id"]
        val directId = pixivIdElement?.jsonPrimitive?.content?.toLongOrNull()
        if (directId != null && directId > 0) return directId

        val extUrls = data["ext_urls"] as? JsonArray ?: return null
        for (urlItem in extUrls) {
            val url = urlItem.jsonPrimitive.content
            val match = PIXIV_URL_REGEX.find(url)
            val extractedId = match?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (extractedId != null && extractedId > 0) {
                return extractedId
            }
        }
        return null
    }

    private fun parseHtml(html: String): List<SauceNaoResult> {
        val seen = mutableSetOf<Long>()
        val parsedList = mutableListOf<SauceNaoResult>()

        val matches = HTML_RESULT_REGEX.findAll(html).toList()
        if (matches.isNotEmpty()) {
            for (match in matches) {
                val block = match.groupValues.getOrNull(1) ?: continue
                val idMatch = PIXIV_URL_REGEX.find(block)
                val illustId = idMatch?.groupValues?.getOrNull(1)?.toLongOrNull() ?: continue
                if (!seen.add(illustId)) continue

                val thumbnail = HTML_IMG_REGEX.find(block)?.groupValues?.getOrNull(1)
                val similarity = HTML_SIMILARITY_REGEX.find(block)?.groupValues?.getOrNull(1)?.trim()
                val title = HTML_TITLE_REGEX.find(block)?.groupValues?.getOrNull(1)?.trim()
                val author = HTML_MEMBER_REGEX.find(block)?.groupValues?.getOrNull(1)?.trim()

                parsedList.add(
                    SauceNaoResult(
                        illustId = illustId,
                        title = title,
                        author = author,
                        similarity = similarity,
                        thumbnail = thumbnail,
                    )
                )
            }
            if (parsedList.isNotEmpty()) return parsedList
        }

        // 终极平铺正则兜底
        for (urlMatch in PIXIV_URL_REGEX.findAll(html)) {
            val illustId = urlMatch.groupValues.getOrNull(1)?.toLongOrNull() ?: continue
            if (seen.add(illustId)) {
                parsedList.add(
                    SauceNaoResult(
                        illustId = illustId,
                        title = null,
                        author = null,
                        similarity = null,
                        thumbnail = null,
                    )
                )
            }
        }

        return parsedList
    }
}
