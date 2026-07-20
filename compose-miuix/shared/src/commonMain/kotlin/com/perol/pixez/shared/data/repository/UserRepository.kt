package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.model.UserIllusts
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.UserPreviewsResponse
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

    /**
     * 获取用户收藏的插画列表。
     *
     * @param userId 用户 ID。
     * @param restrict 可见性：`public` 或 `private`，默认 `public`。
     */
    suspend fun getUserBookmarks(
        userId: Int,
        restrict: String = "public",
    ): List<Illust> = networkCall("获取用户收藏失败 userId=$userId restrict=$restrict") {
        val response: UserIllusts = apiClient.get("/v1/user/bookmarks/illust") {
            parameter("filter", "for_android")
            parameter("user_id", userId)
            parameter("restrict", restrict)
        }.body()
        response.illusts
    }

    /**
     * 获取用户关注列表。
     *
     * @param userId 用户 ID。
     * @param restrict 可见性：`public` 或 `private`，默认 `public`。
     */
    suspend fun getUserFollowing(
        userId: Int,
        restrict: String = "public",
    ): List<UserPreview> = networkCall("获取用户关注列表失败 userId=$userId restrict=$restrict") {
        val response: UserPreviewsResponse = apiClient.get("/v1/user/following") {
            parameter("filter", "for_android")
            parameter("user_id", userId)
            parameter("restrict", restrict)
        }.body()
        response.userPreviews
    }

    /**
     * 获取用户粉丝列表。
     *
     * @param userId 用户 ID。
     * @param restrict 可见性：`public` 或 `private`，默认 `public`。
     */
    suspend fun getUserFollowers(
        userId: Int,
        restrict: String = "public",
    ): List<UserPreview> = networkCall("获取用户粉丝列表失败 userId=$userId restrict=$restrict") {
        val response: UserPreviewsResponse = apiClient.get("/v1/user/follower") {
            parameter("filter", "for_android")
            parameter("user_id", userId)
            parameter("restrict", restrict)
        }.body()
        response.userPreviews
    }

    /**
     * 获取推荐用户列表。
     */
    suspend fun getRecommendedUsers(): List<UserPreview> = networkCall("获取推荐用户失败") {
        val response: UserPreviewsResponse = apiClient.get("/v1/user/recommended") {
            parameter("filter", "for_android")
        }.body()
        response.userPreviews
    }
}
