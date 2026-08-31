package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.BoardInfo
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * 公告板仓库：从原 Flutter 项目托管在 GitHub Raw / CDN 镜像上的 JSON 拉取公告列表。
 *
 * 该数据源无需 Pixiv 认证，因此使用独立的轻量级 HttpClient。
 * [client] 由调用方注入并管理生命周期，仓库自身不再创建或持有客户端，
 * 以便在 [com.perol.pixez.shared.AppDependencies.close] 中统一释放资源。
 */
class BoardRepository(
    private val client: HttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var memoryCache: List<BoardInfo>? = null

    /**
     * 从远端加载公告列表，支持 GitHub Raw 与 CDN 镜像多源重试与内存缓存兜底。
     */
    suspend fun loadBoardList(): List<BoardInfo> = networkCall("加载公告板失败") {
        val cached = memoryCache
        var lastError: Throwable? = null
        for (url in BOARD_URLS) {
            try {
                val text = client.get(url).bodyAsText()
                val list = json.decodeFromString<List<BoardInfo>>(text)
                memoryCache = list
                return@networkCall list
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("尝试从 $url 加载公告失败: ${e.message}")
                lastError = e
            }
        }
        if (!cached.isNullOrEmpty()) {
            Napier.i("远端加载公告失败，回退使用内存缓存")
            return@networkCall cached
        }
        throw (lastError ?: RuntimeException("加载公告板失败"))
    }

    companion object {
        /**
         * 公告 JSON 多数据源地址（包含 GitHub Raw 与 jsdelivr CDN 镜像），
         * 提高在不同网络环境下的可用性，避免因单一源连接超时导致入口丢失。
         */
        private val BOARD_URLS = listOf(
            "https://raw.githubusercontent.com/Notsfsssf/pixez-flutter/refs/heads/master/.github/board/android.json",
            "https://fastly.jsdelivr.net/gh/Notsfsssf/pixez-flutter@master/.github/board/android.json",
            "https://cdn.jsdelivr.net/gh/Notsfsssf/pixez-flutter@master/.github/board/android.json",
        )
    }
}
