package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Comment
import com.perol.pixez.shared.data.model.CommentResponse
import com.perol.pixez.shared.data.model.FollowIllusts
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustDetailResponse
import com.perol.pixez.shared.data.model.IllustSeriesWithIdModel
import com.perol.pixez.shared.data.model.Ranking
import com.perol.pixez.shared.data.model.Recommend
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.SpotlightResponse
import com.perol.pixez.shared.data.model.Walkthrough
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

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
     * 获取关注用户最新插画（/v2/illust/follow）。
     *
     * @param restrict 可见性筛选：all、public、private。
     */
    suspend fun getFollowIllusts(restrict: String = "all"): List<Illust> =
        networkCall("获取关注插画失败 restrict=$restrict") {
            val response: FollowIllusts = apiClient.get("/v2/illust/follow") {
                parameter("restrict", restrict)
            }.body()
            response.illusts
        }

    /**
     * 获取 Spotlight 精选文章列表。
     *
     * @param category 分类，如 all、illust、novel 等。
     */
    suspend fun getSpotlightArticles(category: String = "all"): List<SpotlightArticle> =
        networkCall("获取 Spotlight 失败 category=$category") {
            val response: SpotlightResponse = apiClient.get("/v1/spotlight/articles") {
                parameter("filter", "for_android")
                parameter("category", category)
            }.body()
            response.spotlightArticles
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

    /**
     * 获取作品评论列表。
     */
    suspend fun getIllustComments(illustId: Int): List<Comment> =
        networkCall("获取作品评论失败 illustId=$illustId") {
            val response: CommentResponse = apiClient.get("/v3/illust/comments") {
                parameter("illust_id", illustId)
            }.body()
            response.comments
        }

    /**
     * 发表作品评论或回复指定评论。
     *
     * @param illustId 作品 ID。
     * @param comment 评论内容。
     * @param parentCommentId 被回复的评论 ID；为空时发表普通评论。
     */
    suspend fun postComment(
        illustId: Int,
        comment: String,
        parentCommentId: Int? = null,
    ): Unit = networkCall("发表评论失败 illustId=$illustId") {
        apiClient.post("/v1/illust/comment/add") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("illust_id", illustId.toString())
                        append("comment", comment)
                        if (parentCommentId != null) {
                            append("parent_comment_id", parentCommentId.toString())
                        }
                    },
                ),
            )
        }
    }

    /**
     * 获取相关作品列表。
     */
    suspend fun getIllustRelated(illustId: Int): List<Illust> =
        networkCall("获取相关作品失败 illustId=$illustId") {
            val response: Recommend = apiClient.get("/v2/illust/related") {
                parameter("filter", "for_android")
                parameter("illust_id", illustId)
            }.body()
            response.illusts
        }

    /**
     * 获取插画系列详情与系列内作品列表。
     */
    suspend fun getIllustSeries(seriesId: Int): Pair<String, List<Illust>> =
        networkCall("获取系列详情失败 seriesId=$seriesId") {
            val response: IllustSeriesWithIdModel = apiClient.get("/v1/illust/series") {
                parameter("illust_series_id", seriesId)
            }.body()
            val title = response.illustSeriesDetail?.title ?: "系列"
            title to (response.illusts.orEmpty())
        }
}
