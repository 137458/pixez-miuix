package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.BoardInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * 公告板仓库：从原 Flutter 项目托管在 GitHub Raw 上的 JSON 拉取公告列表。
 *
 * 该数据源无需 Pixiv 认证，因此使用独立的轻量级 HttpClient。
 * [client] 由调用方注入并管理生命周期，仓库自身不再创建或持有客户端，
 * 以便在 [com.perol.pixez.shared.AppDependencies.close] 中统一释放资源。
 */
class BoardRepository(
    private val client: HttpClient,
) {

    /**
     * 从远端加载公告列表。
     *
     * 依赖 [client] 已安装的 ContentNegotiation 插件直接反序列化为 [BoardInfo] 列表；
     * 解析失败或网络异常时抛出异常。
     */
    suspend fun loadBoardList(): List<BoardInfo> = networkCall("加载公告板失败") {
        client.get(BOARD_URL).body<List<BoardInfo>>()
    }

    companion object {
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
