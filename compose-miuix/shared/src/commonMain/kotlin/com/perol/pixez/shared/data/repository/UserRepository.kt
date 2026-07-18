package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.model.UserIllusts
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 用户仓库：用户资料与作品列表。
 */
class UserRepository(
    private val apiClient: HttpClient,
) {
    /**
     * 获取用户详情。
     */
    suspend fun getUserDetail(userId: Int): UserDetail =
        networkCall("获取用户详情失败 userId=$userId") {
            apiClient.get("/v1/user/detail") {
                parameter("filter", "for_android")
                parameter("user_id", userId)
            }.body()
        }

    /**
     * 获取用户作品列表。
     *
     * @param userId 用户 ID。
     * @param type 作品类型：illust、manga、novel。
     */
    suspend fun getUserIllusts(
        userId: Int,
        type: String = "illust",
    ): List<Illust> = networkCall("获取用户作品失败 userId=$userId type=$type") {
        val response: UserIllusts = apiClient.get("/v1/user/illusts") {
            parameter("filter", "for_android")
            parameter("user_id", userId)
            parameter("type", type)
        }.body()
        response.illusts
    }
}
