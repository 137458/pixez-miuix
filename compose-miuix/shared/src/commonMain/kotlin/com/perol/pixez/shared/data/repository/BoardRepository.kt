package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.BoardInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 公告板仓库：从原 Flutter 项目托管在 GitHub Raw 上的 JSON 拉取公告列表。
 *
 * 该数据源无需 Pixiv 认证，因此使用独立的轻量级 HttpClient，避免与 Pixiv API
 * 的 Token 刷新逻辑耦合。
 */
class BoardRepository {

    /**
     * 复用的轻量级 HttpClient，配置超时与 JSON 解析。
     */
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

    /**
     * 从远端加载公告列表。
     *
     * 返回按 JSON 顺序排列的 [BoardInfo] 列表；解析失败或网络异常时抛出异常。
     */
    suspend fun loadBoardList(): List<BoardInfo> = networkCall("加载公告板失败") {
        val responseText = client.get(BOARD_URL).bodyAsText()
        jsonParser.decodeFromString<List<BoardInfo>>(responseText)
    }

    companion object {
        /**
         * 复用的 JSON 解析器，避免每次请求都创建新的 Json 实例。
         */
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        /**
         * 公告 JSON 数据源地址。
         *
         * 与原 Flutter 项目调试模式保持一致，统一使用 android.json；
         * 不修改数据源基础 URL 与 JSON 格式。
         */
        private const val BOARD_BASE_URL =
            "https://raw.githubusercontent.com/Notsfsssf/pixez-flutter/refs/heads/master/.github/board"
        private const val BOARD_URL = "$BOARD_BASE_URL/android.json"
    }
}
