package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustDetailResponse
import com.perol.pixez.shared.data.model.Ranking
import com.perol.pixez.shared.data.model.Recommend
import com.perol.pixez.shared.data.model.Walkthrough
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 插画作品仓库：推荐、排行榜、作品详情等业务接口。
 */
class IllustRepository(
    private val apiClient: HttpClient,
) {
    /**
     * 获取首页推荐插画。
     */
    suspend fun getRecommended(): List<Illust> = networkCall("获取推荐插画失败") {
        val response: Recommend = apiClient.get("/v1/illust/recommended") {
            parameter("filter", "for_ios")
            parameter("include_ranking_label", "true")
        }.body()
        response.illusts
    }

    /**
     * 获取未登录 walkthrough 匿名推荐插画。
     */
    suspend fun getWalkthroughIllusts(): List<Illust> = networkCall("获取匿名推荐插画失败") {
        val response: Walkthrough = apiClient.get("/v1/walkthrough/illusts").body()
        response.illusts
    }

    /**
     * 获取排行榜插画。
     *
     * @param mode 排行榜模式，如 day、week、month、day_male、day_female 等。
     * @param date 日期，格式 yyyy-MM-dd；为空则取最新。
     */
    suspend fun getRanking(mode: String, date: String? = null): List<Illust> =
        networkCall("获取排行榜失败 mode=$mode") {
            val response: Ranking = apiClient.get("/v1/illust/ranking") {
                parameter("filter", "for_android")
                parameter("mode", mode)
                if (!date.isNullOrBlank()) {
                    parameter("date", date)
                }
            }.body()
            response.illusts
        }

    /**
     * 获取最新插画（Follow 页也复用 /v1/illust/follow 之外的接口，这里使用 walkthrough 作为占位）。
     *
     * M4 先复用推荐接口作为“最新”标签内容，后续根据原应用需求替换。
     */
    suspend fun getNew(): List<Illust> {
        // 原 Flutter 的 "New" 标签实际展示关注作品（/v2/illust/follow）。
        // M4 为保持各标签都有内容，先返回推荐数据；M5 接入关注系统后再替换。
        return getRecommended()
    }

    /**
     * 获取作品详情。
     */
    suspend fun getIllustDetail(illustId: Int): Illust =
        networkCall("获取作品详情失败 illustId=$illustId") {
            val response: IllustDetailResponse = apiClient.get("/v1/illust/detail") {
                parameter("filter", "for_android")
                parameter("illust_id", illustId)
            }.body()
            response.illust
        }
}
