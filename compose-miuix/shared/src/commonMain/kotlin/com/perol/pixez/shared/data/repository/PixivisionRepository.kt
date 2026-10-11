package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.SpotlightDetail
import com.perol.pixez.shared.data.model.SpotlightResponse
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.PixivHosts
import com.perol.pixez.shared.network.TrustedUrlPolicy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/**
 * Pixivision 特辑仓库：Spotlight 精选列表与特辑详情的抓取、解析与缓存（架构审查 中-7 拆分）。
 *
 * 缓存语义统一说明：
 * - [getSpotlightArticles]：仅首页响应写入 LRU 缓存（按分类存快照，供分类切换/重进页面恢复）；
 *   首页默认读缓存，[forceRefresh] 强制发起网络请求并覆盖旧快照；翻页原样返回当页响应、
 *   不回写缓存——累积与去重由 UI 层共享列表状态单点负责（架构审查 中-8）。
 * - [getSpotlightArticleDetail]：详情全量缓存（LRU 50），[forceRefresh] 强制重新抓取并覆盖。
 * - [clearMemoryCache]：两类缓存一并失效，账号身份变化（登出/切换账号）时由 AccountRepository 调用。
 *
 * 数据来源分两条链路：all/illust/manga 分类走 app-api（[apiClient]），novel 分类与特辑详情
 * 走 pixivision.net 网页抓取（[webClient]，UA 伪装 + 语言路径拼接）经 [PixivisionParser] 解析。
 */
class PixivisionRepository(
    private val apiClient: HttpClient,
    private val webClient: HttpClient,
    private val settingsRepository: SettingsRepository,
) {
    private val spotlightArticlesCache = ThreadSafeLruCache<String, SpotlightResponse>(20)

    /**
     * 清空全部内存缓存：Spotlight 列表与详情 LRU 一并失效。
     * 生产接入点为账号身份变化（AccountRepository.logout / switchAccount）。
     */
    suspend fun clearMemoryCache() {
        spotlightArticlesCache.clear()
        spotlightDetailCache.clear()
    }

    /**
     * 获取指定分类已缓存的 Spotlight 首页快照，若无缓存返回 null。
     */
    fun getCachedSpotlightArticles(category: String = "all"): SpotlightResponse? = spotlightArticlesCache.get(category)

    /**
     * 获取 Spotlight 精选文章列表，支持按分类与分页加载。
     *
     * 仓库只作当页数据源：首页响应写入 LRU 缓存（仅存最近首页快照，供分类切换/重进页面恢复），
     * 翻页原样返回当页响应、不回写缓存——累积与去重由 UI 层共享列表状态单点负责（架构审查 中-8）。
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

    private fun getPixivisionLanguagePath(): String = when (settingsRepository.languageNum) {
        1 -> "zh"
        2 -> "zh-tw"
        3 -> "ja"
        4 -> "ko"
        else -> "en"
    }
}
