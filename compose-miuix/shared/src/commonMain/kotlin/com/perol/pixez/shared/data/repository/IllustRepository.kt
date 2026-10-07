package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.network.TrustedUrlPolicy
import com.perol.pixez.shared.platform.mapToPictureSource

import com.perol.pixez.shared.data.model.CommentResponse
import com.perol.pixez.shared.data.model.FollowIllusts
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustDetailResponse
import com.perol.pixez.shared.data.model.IllustSeriesWithIdModel
import com.perol.pixez.shared.data.model.Ranking
import com.perol.pixez.shared.data.model.Recommend
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.SpotlightDetail
import com.perol.pixez.shared.data.model.SpotlightResponse
import com.perol.pixez.shared.data.model.UgoiraMetadataResponse
import com.perol.pixez.shared.data.model.Walkthrough
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 插画作品仓库：推荐、排行榜、作品详情等业务接口。
 */
class IllustRepository(
    private val apiClient: HttpClient,
    private val webClient: HttpClient = HttpClient(),
    private val downloadClient: HttpClient = webClient,
    private val settingsRepository: SettingsRepository,
) {
    @Volatile
    private var cachedRecommendedResponse: Recommend? = null

    @Volatile
    private var cachedWalkthroughResponse: Walkthrough? = null

    private val illustsCache = ThreadSafeLruCache<Long, Illust>(500)

    /** 跨页恢复的视图数据：UI 过滤后的作品列表与下一页 URL。 */
    private data class ActiveFeed(
        val illusts: List<Illust>,
        val nextUrl: String?,
    )

    private val activeRecommendedFeed = MutableStateFlow<ActiveFeed?>(null)
    private val activeRankingPages = MutableStateFlow<Map<String, ActiveFeed>>(emptyMap())

    /** 排行跨页缓存的 key 容量上限，超出按 LRU 淘汰最久未用 key，防内存无界增长。 */
    private companion object {
        const val RANKING_CACHE_MAX_KEYS = 8
    }

    /** 推荐页跨页恢复列表快照（UI 过滤后），未记录时为 null。 */
    val activeRecommendedIllusts: List<Illust>?
        get() = activeRecommendedFeed.value?.illusts

    /** 推荐页跨页恢复下一页 URL 快照，未记录时为 null。 */
    val activeRecommendedNextUrl: String?
        get() = activeRecommendedFeed.value?.nextUrl

    /**
     * 记录推荐页跨页恢复数据（UI 过滤后的列表与下一页 URL）。
     */
    fun updateRecommendedFeed(filtered: List<Illust>, nextUrl: String?) {
        activeRecommendedFeed.value = ActiveFeed(filtered, nextUrl)
    }

    /**
     * 获取指定 key（mode_date）的排行页跨页恢复快照，未记录时为 null。
     */
    fun getCachedRankingPage(key: String): Pair<List<Illust>, String?>? =
        activeRankingPages.value[key]?.let { it.illusts to it.nextUrl }

    /**
     * 记录指定 key（mode_date）的排行页跨页恢复数据；
     * 同 key 重写视为最近使用，缓存超过 [RANKING_CACHE_MAX_KEYS] 个 key 时淘汰最久未用的 key。
     */
    fun updateRankingPage(key: String, list: List<Illust>, nextUrl: String?) {
        activeRankingPages.value =
            activeRankingPages.value.putRankingEntry(key, ActiveFeed(list, nextUrl))
    }

    /** 以写入序 LRU 语义返回插入后的新快照：重复 key 提升到最新，超限淘汰最旧 key。 */
    private fun Map<String, ActiveFeed>.putRankingEntry(key: String, feed: ActiveFeed): Map<String, ActiveFeed> {
        val next = LinkedHashMap<String, ActiveFeed>(size)
        for ((existingKey, value) in this) {
            if (existingKey != key) {
                next[existingKey] = value
            }
        }
        next[key] = feed
        while (next.size > RANKING_CACHE_MAX_KEYS) {
            next.remove(next.keys.first())
        }
        return next
    }

    fun clearMemoryCache() {
        cachedRecommendedResponse = null
        cachedWalkthroughResponse = null
        activeRecommendedFeed.value = null
        activeRankingPages.value = emptyMap()
    }

    /**
     * 将单个插画作品存入内存缓存（LRU 策略，最大 500 条）。
     */
    suspend fun cacheIllust(illust: Illust) {
        illustsCache.put(illust.id, illust)
    }

    /**
     * 将批量插画作品存入内存缓存。
     */
    suspend fun cacheIllusts(illusts: Iterable<Illust>) {
        illustsCache.putAll(illusts.map { it.id to it })
    }

    /**
     * 根据 ID 获取已在内存缓存中的插画作品，若未缓存则返回 null。
     * 读取不可变快照，保障高并发安全且无锁开销。
     */
    fun getCachedIllust(illustId: Long): Illust? = illustsCache.get(illustId)

    /**
     * 获取首页推荐插画响应（含 nextUrl），默认使用内存缓存，通过 [forceRefresh] 触发强制刷新。
     */
    suspend fun getRecommendedResponse(
        nextUrl: String? = null,
        forceRefresh: Boolean = false,
    ): Recommend = networkCall("获取推荐插画失败") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            val response: Recommend = apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
            cacheIllusts(response.illusts)
            response
        } else {
            val cached = cachedRecommendedResponse
            if (!forceRefresh && cached != null) {
                return@networkCall cached
            }
            val response: Recommend = apiClient.get("/v1/illust/recommended") {
                parameter("filter", "for_ios")
                parameter("include_ranking_label", "true")
            }.body()
            cacheIllusts(response.illusts)
            cachedRecommendedResponse = response
            response
        }
    }

    /**
     * 获取首页推荐插画列表，默认使用内存缓存，通过 [forceRefresh] 触发强制刷新。
     */
    suspend fun getRecommended(forceRefresh: Boolean = false): List<Illust> =
        getRecommendedResponse(nextUrl = null, forceRefresh = forceRefresh).illusts

    /**
     * 获取未登录 walkthrough 匿名推荐插画响应（含 nextUrl），默认使用内存缓存，通过 [forceRefresh] 触发强制刷新。
     */
    suspend fun getWalkthroughResponse(
        nextUrl: String? = null,
        forceRefresh: Boolean = false,
    ): Walkthrough = networkCall("获取匿名推荐插画失败") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            val response: Walkthrough = apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
            cacheIllusts(response.illusts)
            response
        } else {
            val cached = cachedWalkthroughResponse
            if (!forceRefresh && cached != null) {
                return@networkCall cached
            }
            val response: Walkthrough = apiClient.get("/v1/walkthrough/illusts").body()
            cacheIllusts(response.illusts)
            cachedWalkthroughResponse = response
            response
        }
    }

    /**
     * 获取未登录 walkthrough 匿名推荐插画列表，默认使用内存缓存，通过 [forceRefresh] 触发强制刷新。
     */
    suspend fun getWalkthroughIllusts(forceRefresh: Boolean = false): List<Illust> =
        getWalkthroughResponse(nextUrl = null, forceRefresh = forceRefresh).illusts

    /**
     * 获取排行榜插画响应（含 nextUrl）。
     *
     * @param mode 排行榜模式，如 day、week、month、day_male、day_female 等。
     * @param date 日期，格式 yyyy-MM-dd；为空则取最新。
     * @param nextUrl 分页 URL，非空时优先请求下一页。
     */
    suspend fun getRankingResponse(
        mode: String,
        date: String? = null,
        nextUrl: String? = null,
    ): Ranking = networkCall("获取排行榜失败 mode=$mode") {
        val response: Ranking = if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v1/illust/ranking") {
                parameter("filter", "for_android")
                parameter("mode", mode)
                if (!date.isNullOrBlank()) {
                    parameter("date", date)
                }
            }.body()
        }
        cacheIllusts(response.illusts)
        response
    }

    /**
     * 获取排行榜插画列表（兼容旧调用）。
     */
    suspend fun getRanking(mode: String, date: String? = null): List<Illust> =
        getRankingResponse(mode = mode, date = date).illusts

    /**
     * 获取关注用户最新插画响应（含 nextUrl）。
     *
     * @param restrict 可见性筛选：all、public、private。
     * @param nextUrl 分页 URL，非空时优先请求下一页。
     */
    suspend fun getFollowIllustsResponse(
        restrict: String = "all",
        nextUrl: String? = null,
    ): FollowIllusts = networkCall("获取关注插画失败 restrict=$restrict") {
        val response: FollowIllusts = if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v2/illust/follow") {
                parameter("restrict", restrict)
            }.body()
        }
        cacheIllusts(response.illusts)
        response
    }

    /**
     * 获取关注用户最新插画列表（兼容旧调用）。
     */
    suspend fun getFollowIllusts(restrict: String = "all"): List<Illust> =
        getFollowIllustsResponse(restrict = restrict).illusts

    private val spotlightArticlesCache = ThreadSafeLruCache<String, SpotlightResponse>(20)

    /**
     * 获取指定分类已缓存的 Spotlight 列表，若无缓存返回 null。
     */
    fun getCachedSpotlightArticles(category: String = "all"): SpotlightResponse? = spotlightArticlesCache.get(category)

    /**
     * 获取 Spotlight 精选文章列表，支持按分类与分页加载，内置内存级缓存。
     *
     * @param category 分类，如 all、illust、manga、novel 等。
     * @param nextUrl 分页加载下一页时传入的完整 URL；为空时请求首页数据。
     * @param forceRefresh 是否强制发起网络请求刷新。
     */
    suspend fun getSpotlightArticles(
        category: String = "all",
        nextUrl: String? = null,
        forceRefresh: Boolean = false,
    ): SpotlightResponse {
        if (nextUrl == null && !forceRefresh) {
            val cached = spotlightArticlesCache.get(category)
            if (cached != null) {
                return cached
            }
        }
        return networkCall("获取 Spotlight 失败 category=$category") {
            val isNovelCategory = category.equals("novel", ignoreCase = true) || category.equals("novels", ignoreCase = true)
            val response: SpotlightResponse = if (isNovelCategory) {
                if (nextUrl != null && nextUrl.isNotBlank()) {
                    SpotlightResponse(spotlightArticles = emptyList(), nextUrl = null)
                } else {
                    val lang = getPixivisionLanguagePath()
                    val targetUrl = "https://www.pixivision.net/$lang/c/novels"
                    val html: String = webClient.get(TrustedUrlPolicy.spotlightUrl(targetUrl)) {
                        headers {
                            append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            append("Referer", "https://www.pixivision.net/")
                        }
                    }.body()
                    val articles = PixivisionParser.parseArticleList(html)
                    SpotlightResponse(
                        spotlightArticles = articles,
                        nextUrl = null,
                    )
                }
            } else if (nextUrl != null && nextUrl.isNotBlank()) {
                apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
            } else {
                apiClient.get("/v1/spotlight/articles") {
                    parameter("filter", "for_android")
                    parameter("category", category)
                }.body()
            }

            if (nextUrl == null) {
                spotlightArticlesCache.put(category, response)
            } else {
                val cached = spotlightArticlesCache.get(category)
                if (cached != null) {
                    val existingIds = cached.spotlightArticles.map { it.id }.toSet()
                    val merged = cached.spotlightArticles + response.spotlightArticles.filter { it.id !in existingIds }
                    spotlightArticlesCache.put(
                        category,
                        SpotlightResponse(
                            spotlightArticles = merged,
                            nextUrl = response.nextUrl,
                        ),
                    )
                }
            }
            response
        }
    }

    private val spotlightDetailCache = ThreadSafeLruCache<String, SpotlightDetail>(50)

    /**
     * 获取缓存的 Spotlight 特辑详情，若无缓存返回 null。
     */
    fun getCachedSpotlightDetail(articleUrl: String): SpotlightDetail? = spotlightDetailCache.get(articleUrl)

    /**
     * 请求并解析 Pixivision 特辑文章详情（包含正文导语与画作列表，支持内存缓存与强制刷新）。
     *
     * @param articleUrl 特辑文章 URL。
     * @param forceRefresh 是否强制发起网络请求刷新。
     */
    suspend fun getSpotlightArticleDetail(
        articleUrl: String,
        forceRefresh: Boolean = false,
    ): SpotlightDetail {
        if (!forceRefresh) {
            val cached = spotlightDetailCache.get(articleUrl)
            if (cached != null) {
                return cached
            }
        }
        return networkCall("获取 Spotlight 特辑详情失败 url=$articleUrl") {
            val response: String = webClient.get(TrustedUrlPolicy.spotlightUrl(articleUrl)) {
                headers {
                    append("Referer", PixivHosts.REFERER_PIXIVISION_ZH)
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/85.0.564.13")
                    append("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7")
                }
            }.bodyAsText()
            val detail = PixivisionParser.parse(response, articleUrl)
            spotlightDetailCache.put(articleUrl, detail)
            detail
        }
    }

    /**
     * 获取作品详情。
     */
    suspend fun getIllustDetail(illustId: Long): Illust =
        networkCall("获取作品详情失败 illustId=$illustId") {
            val response: IllustDetailResponse = apiClient.get("/v1/illust/detail") {
                parameter("filter", "for_android")
                parameter("illust_id", illustId)
            }.body()
            cacheIllust(response.illust)
            response.illust
        }

    /**
     * 获取动图 (Ugoira) 元数据（含 zip 包地址与各帧延迟时间）。
     */
    suspend fun getUgoiraMetadata(illustId: Long): UgoiraMetadataResponse =
        networkCall("获取动图元数据失败 illustId=$illustId") {
            apiClient.get("/v1/ugoira/metadata") {
                parameter("illust_id", illustId)
            }.body()
        }

    /**
     * 获取作品评论响应（含 nextUrl）。
     */
    suspend fun getIllustCommentsResponse(
        illustId: Long,
        nextUrl: String? = null,
    ): CommentResponse = networkCall("获取作品评论失败 illustId=$illustId") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v3/illust/comments") {
                parameter("illust_id", illustId)
            }.body()
        }
    }

    /**
     * 发表作品评论或回复指定评论。
     *
     * @param illustId 作品 ID。
     * @param comment 评论内容。
     * @param parentCommentId 被回复的评论 ID；为空时发表普通评论。
     */
    suspend fun postComment(
        illustId: Long,
        comment: String,
        parentCommentId: Long? = null,
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
     * 获取相关作品响应（含 nextUrl）。
     */
    suspend fun getIllustRelatedResponse(
        illustId: Long,
        nextUrl: String? = null,
    ): Recommend = networkCall("获取相关作品失败 illustId=$illustId") {
        val response: Recommend = if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v2/illust/related") {
                parameter("filter", "for_android")
                parameter("illust_id", illustId)
            }.body()
        }
        cacheIllusts(response.illusts)
        response
    }

    /**
     * 获取相关作品列表（兼容旧调用）。
     */
    suspend fun getIllustRelated(illustId: Long): List<Illust> =
        getIllustRelatedResponse(illustId).illusts

    /**
     * 获取插画系列响应（含 nextUrl 与系列详情）。
     */
    suspend fun getIllustSeriesResponse(
        seriesId: Long,
        nextUrl: String? = null,
    ): IllustSeriesWithIdModel = networkCall("获取系列详情失败 seriesId=$seriesId") {
        val response: IllustSeriesWithIdModel = if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v1/illust/series") {
                parameter("illust_series_id", seriesId)
            }.body()
        }
        response.illusts?.let { cacheIllusts(it) }
        response
    }

    /**
     * 下载动图 Zip 包字节流（自动应用用户配置的图源镜像 Host）。
     */
    suspend fun downloadUgoiraZip(zipUrl: String): ByteArray =
        networkCall("下载动图 Zip 失败 url=$zipUrl") {
            val mirrorHost = settingsRepository.pictureSource
            val mappedUrl = zipUrl.mapToPictureSource(mirrorHost)
            val verifiedUrl = TrustedUrlPolicy.imageUrl(mappedUrl, mirrorHost)
            downloadClient.get(verifiedUrl) {
                header("Referer", PixivHosts.PIXIV_APP_API)
            }.body()
        }

    private fun getPixivisionLanguagePath(): String = when (settingsRepository.languageNum) {
        1 -> "zh"
        2 -> "zh-tw"
        3 -> "ja"
        4 -> "ko"
        else -> "en"
    }
}

