package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.NovelRecomResponse
import com.perol.pixez.shared.data.model.NovelSeriesResponse
import com.perol.pixez.shared.data.model.NovelTextResponse
import com.perol.pixez.shared.network.TrustedUrlPolicy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 小说作品仓库：提供小说推荐、排行榜、系列与正文文本拉取业务接口。
 */
class NovelRepository(
    private val apiClient: HttpClient,
) {
    /**
     * 获取小说正文内容与标记信息。
     */
    suspend fun getNovelText(novelId: Int): NovelTextResponse =
        networkCall("获取小说正文失败 novelId=$novelId") {
            apiClient.get("/v1/novel/text") {
                parameter("novel_id", novelId)
            }.body()
        }

    /**
     * 获取小说系列作品列表（支持分页）。
     */
    suspend fun getNovelSeries(seriesId: Int, nextUrl: String? = null): NovelSeriesResponse =
        networkCall("获取小说系列失败 seriesId=$seriesId") {
            if (!nextUrl.isNullOrBlank()) {
                apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
            } else {
                apiClient.get("/v2/novel/series") {
                    parameter("series_id", seriesId)
                }.body()
            }
        }

    /**
     * 获取推荐小说列表（支持分页）。
     */
    suspend fun getRecommendedNovels(
        includeRanking: Boolean = true,
        nextUrl: String? = null,
    ): NovelRecomResponse = networkCall("获取推荐小说失败") {
        if (!nextUrl.isNullOrBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v1/novel/recommended") {
                parameter("filter", "for_android")
                parameter("include_privacy_policy", "true")
                parameter("include_ranking_novels", includeRanking.toString())
            }.body()
        }
    }

    /**
     * 获取小说排行榜（支持日榜、周榜、月榜等与分页）。
     */
    suspend fun getNovelRanking(
        mode: String = "day",
        nextUrl: String? = null,
    ): NovelRecomResponse = networkCall("获取小说排行榜失败 mode=$mode") {
        if (!nextUrl.isNullOrBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v1/novel/ranking") {
                parameter("filter", "for_android")
                parameter("mode", mode)
            }.body()
        }
    }
}
