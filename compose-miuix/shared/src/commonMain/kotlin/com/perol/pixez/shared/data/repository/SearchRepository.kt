package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.Search
import com.perol.pixez.shared.data.model.TrendTag
import com.perol.pixez.shared.data.model.TrendingTag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 搜索仓库：热门标签与插画搜索。
 */
class SearchRepository(
    private val apiClient: HttpClient,
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
     * 按关键词搜索插画。
     *
     * @param word 搜索关键词。
     * @param sort 排序：date_desc（默认）、date_asc、popular_desc。
     * @param searchTarget 搜索目标：partial_match_for_tags、exact_match_for_tags、title_and_caption 等。
     */
    suspend fun searchIllust(
        word: String,
        sort: String = "date_desc",
        searchTarget: String = "partial_match_for_tags",
    ): List<Illust> = networkCall("搜索插画失败 word=$word") {
        val response: Search = apiClient.get("/v1/search/illust") {
            parameter("filter", "for_android")
            parameter("merge_plain_keyword_results", "true")
            parameter("sort", sort)
            parameter("search_target", searchTarget)
            parameter("word", word)
        }.body()
        response.illusts
    }
}
