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
import com.perol.pixez.shared.data.model.UgoiraMetadataResponse
import com.perol.pixez.shared.data.model.Walkthrough
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/**
 * 插画作品仓库：推荐、排行榜、作品详情等 app-api 业务接口。
 * Pixivision 特辑抓取/解析已拆分至 [PixivisionRepository]（架构审查 中-7）。
 */
class IllustRepository(
    private val apiClient: HttpClient,
    private val downloadClient: HttpClient = HttpClient(),
    private val settingsRepository: SettingsRepository,
    private val maxUgoiraZipBytes: Long = DownloadRepository.DEFAULT_MAX_DOWNLOAD_BYTES,
) {
    @Volatile
    private var cachedRecommendedResponse: Recommend? = null

    @Volatile
    private var cachedWalkthroughResponse: Walkthrough? = null

    private val illustsCache = ThreadSafeLruCache<Long, Illust>(500)

    /**
     * 清空全部内存缓存（高-5 缓存清理缺失修复）：推荐/匿名推荐响应与插画 LRU 一并失效；
     * Spotlight 列表与详情 LRU 随中-7 拆分迁至 [PixivisionRepository.clearMemoryCache]，
     * 跨页恢复快照自本版本起由 UI 层恢复缓存承载（ui/state IllustFeedRestore / RankingFeedRestore），
     * 不再归仓库管理。生产接入点为账号身份变化（AccountRepository.logout / switchAccount）。
     */
    suspend fun clearMemoryCache() {
        cachedRecommendedResponse = null
        cachedWalkthroughResponse = null
        illustsCache.clear()
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
     * 获取小说评论响应（含 nextUrl）。响应结构与插画评论同构，复用 [CommentResponse]。
     */
    suspend fun getNovelCommentsResponse(
        novelId: Long,
        nextUrl: String? = null,
    ): CommentResponse = networkCall("获取小说评论失败 novelId=$novelId") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v3/novel/comments") {
                parameter("novel_id", novelId)
            }.body()
        }
    }

    /**
     * 获取小说评论回复（楼中楼）响应（含 nextUrl）。
     */
    suspend fun getNovelCommentRepliesResponse(
        commentId: Long,
        nextUrl: String? = null,
    ): CommentResponse = networkCall("获取小说评论回复失败 commentId=$commentId") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
        } else {
            apiClient.get("/v2/novel/comment/replies") {
                parameter("comment_id", commentId)
            }.body()
        }
    }

    /**
     * 发表小说评论或回复指定评论。
     *
     * @param novelId 小说 ID。
     * @param comment 评论内容。
     * @param parentCommentId 被回复的评论 ID；为空时发表普通评论。
     */
    suspend fun postNovelComment(
        novelId: Long,
        comment: String,
        parentCommentId: Long? = null,
    ): Unit = networkCall("发表小说评论失败 novelId=$novelId") {
        apiClient.post("/v1/novel/comment/add") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("novel_id", novelId.toString())
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
     *
     * 整包读入内存，仅保留给小负载与既有测试使用；播放/保存链路应使用
     * [downloadUgoiraZipToFile] 流式落盘（架构审查 中-15）。
     */
    suspend fun downloadUgoiraZip(zipUrl: String): ByteArray =
        networkCall("下载动图 Zip 失败 url=$zipUrl") {
            downloadClient.get(resolveUgoiraZipUrl(zipUrl)) {
                header("Referer", PixivHosts.PIXIV_APP_API)
            }.body()
        }

    /**
     * 下载动图 Zip 包并流式落盘至 [targetPath]（自动应用用户配置的图源镜像 Host）。
     *
     * 采用 prepareGet + 64KB 缓冲写盘（与 [DownloadRepository.downloadToTempFile] 同一模式），
     * zip 不再整体驻留堆内存（架构审查 中-15）。
     * 大小防护三重校验（上限 [maxUgoiraZipBytes]，默认与 [DownloadRepository.DEFAULT_MAX_DOWNLOAD_BYTES] 对齐）：
     * 写盘前按声明 Content-Length 预检、流中按累计字节上限拦截、结束后核验实收字节与声明长度一致；
     * 下载或写盘失败时清理半成品文件后向上抛出。
     *
     * @return [targetPath] 本身。
     */
    suspend fun downloadUgoiraZipToFile(zipUrl: String, targetPath: Path): Path =
        networkCall("下载动图 Zip 失败 url=$zipUrl") {
            val verifiedUrl = resolveUgoiraZipUrl(zipUrl)
            try {
                downloadClient.prepareGet(verifiedUrl) {
                    header("Referer", PixivHosts.PIXIV_APP_API)
                }.execute { response ->
                    // 写盘前先按声明长度预检，恶意/异常的超大响应不落盘。
                    val declaredLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    require(declaredLength == null || declaredLength <= maxUgoiraZipBytes) {
                        "下载内容声明大小超过上限: $declaredLength"
                    }
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    // okio 落盘为阻塞 I/O，切至 IO 调度器，避免占用调用方线程。
                    withContext(Dispatchers.IO) {
                        FileSystem.SYSTEM.write(targetPath) {
                            val buffer = ByteArray(64 * 1024)
                            var totalBytes = 0L
                            while (!channel.isClosedForRead) {
                                val read = channel.readAvailable(buffer, 0, buffer.size)
                                if (read <= 0) break
                                totalBytes += read
                                // 写入前校验累计字节，超限立即中止，防止无限流撑爆磁盘。
                                require(totalBytes <= maxUgoiraZipBytes) {
                                    "下载内容超过大小上限: $totalBytes"
                                }
                                write(buffer, 0, read)
                            }
                            // 声明长度已知时核验实收字节，拒绝被截断的损坏响应。
                            require(declaredLength == null || totalBytes == declaredLength) {
                                "下载内容不完整: 实收 $totalBytes, 声明 $declaredLength"
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                // 失败路径清理半成品文件，避免残留被后续解压当作有效 zip 消费。
                withContext(NonCancellable + Dispatchers.IO) {
                    runCatching { if (FileSystem.SYSTEM.exists(targetPath)) FileSystem.SYSTEM.delete(targetPath) }
                }
                throw e
            }
            targetPath
        }

    /** 解析动图 Zip 的受信任下载地址：先套用用户配置的镜像 Host，再过图片域名白名单。 */
    private fun resolveUgoiraZipUrl(zipUrl: String): String {
        val mirrorHost = settingsRepository.pictureSource
        val mappedUrl = zipUrl.mapToPictureSource(mirrorHost)
        return TrustedUrlPolicy.imageUrl(mappedUrl, mirrorHost)
    }
}

