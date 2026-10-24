package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.WatchlistMangaResponse
import com.perol.pixez.shared.network.TrustedUrlPolicy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

/**
 * 漫画系列追更（Watchlist）仓库：追更列表分页查询与追更/取消追更。
 * 端点与参数对齐旧 Flutter `api_client.dart` 的 watchListManga / watchListMangaAdd / watchListMangaDelete。
 */
class WatchlistRepository(
    private val apiClient: HttpClient,
) {
    /**
     * 获取漫画系列追更列表响应（含 nextUrl）。
     *
     * @param nextUrl 分页 URL，非空时优先请求下一页。
     */
    suspend fun getMangaWatchlist(nextUrl: String? = null): WatchlistMangaResponse =
        networkCall("获取追更列表失败") {
            if (nextUrl != null && nextUrl.isNotBlank()) {
                apiClient.get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
            } else {
                apiClient.get("/v1/watchlist/manga").body()
            }
        }

    /**
     * 追更指定漫画系列。
     */
    suspend fun addMangaWatchlist(seriesId: Long): Unit = networkCall("追更系列失败 seriesId=$seriesId") {
        apiClient.post("/v1/watchlist/manga/add") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("series_id", seriesId.toString())
                    },
                ),
            )
        }
    }

    /**
     * 取消追更指定漫画系列。
     */
    suspend fun deleteMangaWatchlist(seriesId: Long): Unit =
        networkCall("取消追更系列失败 seriesId=$seriesId") {
            apiClient.post("/v1/watchlist/manga/delete") {
                header("Content-Type", "application/x-www-form-urlencoded")
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("series_id", seriesId.toString())
                        },
                    ),
                )
            }
        }
}
