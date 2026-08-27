package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.Search
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.model.TrendingTag
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.UserPreviewsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 搜索仓库：热门标签与插画搜索。
 */
class SearchRepository(
    private val apiClient: HttpClient,
    private val illustRepository: IllustRepository? = null,
) {
    /**
     * 获取热门标签列表。
     */
    suspend fun getTrendTags(): List<TrendTag> = networkCall("获取热门标签失败") {
        val response: TrendingTag = apiClient.get("/v1/trending-tags/illust") {
            parameter("filter", "for_android")
        }.body()
        response.trendTags
    }

    /**
     * 按关键词搜索插画（返回包含 nextUrl 的响应）。
     */
    suspend fun searchIllustResponse(
        word: String,
        sort: String = "date_desc",
        searchTarget: String = "partial_match_for_tags",
        searchAiType: Int = 0,
        startDate: String? = null,
        endDate: String? = null,
        nextUrl: String? = null,
    ): Search = networkCall("搜索插画失败 word=$word") {
        val response: Search = if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(nextUrl).body()
        } else {
            apiClient.get("/v1/search/illust") {
                parameter("filter", "for_android")
                parameter("merge_plain_keyword_results", "true")
                parameter("sort", sort)
                parameter("search_target", searchTarget)
                parameter("search_ai_type", searchAiType)
                parameter("word", word)
                startDate?.let { parameter("start_date", it.toPixivDateFormat()) }
                endDate?.let { parameter("end_date", it.toPixivDateFormat()) }
            }.body()
        }
        illustRepository?.cacheIllusts(response.illusts)
        response
    }

    /**
     * 按关键词搜索插画（仅返回第一页列表，兼容旧调用）。
     */
    suspend fun searchIllust(
        word: String,
        sort: String = "date_desc",
        searchTarget: String = "partial_match_for_tags",
        searchAiType: Int = 0,
        startDate: String? = null,
        endDate: String? = null,
    ): List<Illust> = searchIllustResponse(
        word = word,
        sort = sort,
        searchTarget = searchTarget,
        searchAiType = searchAiType,
        startDate = startDate,
        endDate = endDate,
    ).illusts

    /**
     * 按关键词搜索画师（返回包含 nextUrl 的响应）。
     */
    suspend fun searchUserResponse(
        word: String,
        nextUrl: String? = null,
    ): UserPreviewsResponse = networkCall("搜索画师失败 word=$word") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(nextUrl).body()
        } else {
            apiClient.get("/v1/search/user") {
                parameter("filter", "for_android")
                parameter("word", word)
            }.body()
        }
    }

    /**
     * 按关键词搜索画师（仅返回第一页列表，兼容旧调用）。
     */
    suspend fun searchUser(
        word: String,
    ): List<UserPreview> = searchUserResponse(word).userPreviews
}


/**
 * 将 YYYY-MM-DD 格式日期转换为 Pixiv API 期望的 YYYY-M-D 格式。
 * 输入非法时原样返回，让后端决定行为。
 */
private fun String.toPixivDateFormat(): String {
    val parts = split("-")
    if (parts.size != 3) return this
    val year = parts[0].toIntOrNull() ?: return this
    val month = parts[1].toIntOrNull() ?: return this
    val day = parts[2].toIntOrNull() ?: return this
    return "$year-$month-$day"
}
