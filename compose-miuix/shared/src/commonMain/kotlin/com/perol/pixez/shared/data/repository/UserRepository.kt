package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.ShowAIResponse
import com.perol.pixez.shared.data.model.UserDetail
import com.perol.pixez.shared.data.model.UserIllusts
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.UserPreviewsResponse
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
     * 获取用户插画/漫画作品响应（含 nextUrl）。

     *
     * @param userId 用户 ID。
     * @param type 作品类型：illust、manga、novel。
     * @param nextUrl 分页请求 URL。
     */
    suspend fun getUserIllustsResponse(
        userId: Int,
        type: String = "illust",
        nextUrl: String? = null,
    ): UserIllusts = networkCall("获取用户作品失败 userId=$userId type=$type") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(nextUrl).body()
        } else {
            apiClient.get("/v1/user/illusts") {
                parameter("filter", "for_android")
                parameter("user_id", userId)
                parameter("type", type)
            }.body()
        }
    }

    /**
     * 获取用户插画/漫画作品列表（兼容旧调用）。
     */
    suspend fun getUserIllusts(
        userId: Int,
        type: String = "illust",
    ): List<Illust> = getUserIllustsResponse(userId, type).illusts

    /**
     * 获取用户收藏的插画响应（含 nextUrl）。
     *
     * @param userId 用户 ID。
     * @param restrict 可见性：`public` 或 `private`，默认 `public`。
     * @param nextUrl 分页请求 URL。
     */
    suspend fun getUserBookmarksResponse(
        userId: Int,
        restrict: String = "public",
        nextUrl: String? = null,
    ): UserIllusts = networkCall("获取用户收藏失败 userId=$userId restrict=$restrict") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(nextUrl).body()
        } else {
            apiClient.get("/v1/user/bookmarks/illust") {
                parameter("filter", "for_android")
                parameter("user_id", userId)
                parameter("restrict", restrict)
            }.body()
        }
    }

    /**
     * 获取用户收藏的插画列表（兼容旧调用）。
     */
    suspend fun getUserBookmarks(
        userId: Int,
        restrict: String = "public",
    ): List<Illust> = getUserBookmarksResponse(userId, restrict).illusts


    /**
     * 获取用户关注列表响应（含 nextUrl）。
     *
     * @param userId 用户 ID。
     * @param restrict 可见性：`public` 或 `private`，默认 `public`。
     * @param nextUrl 分页请求 URL。
     */
    suspend fun getUserFollowingResponse(
        userId: Int,
        restrict: String = "public",
        nextUrl: String? = null,
    ): UserPreviewsResponse = networkCall("获取用户关注列表失败 userId=$userId restrict=$restrict") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(nextUrl).body()
        } else {
            apiClient.get("/v1/user/following") {
                parameter("filter", "for_android")
                parameter("user_id", userId)
                parameter("restrict", restrict)
            }.body()
        }
    }

    /**
     * 获取用户关注列表（兼容旧调用）。
     */
    suspend fun getUserFollowing(
        userId: Int,
        restrict: String = "public",
    ): List<UserPreview> = getUserFollowingResponse(userId, restrict).userPreviews

    /**
     * 获取用户粉丝列表响应（含 nextUrl）。
     *
     * @param userId 用户 ID。
     * @param restrict 可见性：`public` 或 `private`，默认 `public`。
     * @param nextUrl 分页请求 URL。
     */
    suspend fun getUserFollowersResponse(
        userId: Int,
        restrict: String = "public",
        nextUrl: String? = null,
    ): UserPreviewsResponse = networkCall("获取用户粉丝列表失败 userId=$userId restrict=$restrict") {
        if (nextUrl != null && nextUrl.isNotBlank()) {
            apiClient.get(nextUrl).body()
        } else {
            apiClient.get("/v1/user/follower") {
                parameter("filter", "for_android")
                parameter("user_id", userId)
                parameter("restrict", restrict)
            }.body()
        }
    }

    /**
     * 获取用户粉丝列表（兼容旧调用）。
     */
    suspend fun getUserFollowers(
        userId: Int,
        restrict: String = "public",
    ): List<UserPreview> = getUserFollowersResponse(userId, restrict).userPreviews


    /**
     * 获取推荐用户列表。
     */
    suspend fun getRecommendedUsers(): UserPreviewsResponse = networkCall("获取推荐用户失败") {
        apiClient.get("/v1/user/recommended") {
            parameter("filter", "for_android")
        }.body()
    }

    /**
     * 通过 Pixiv 返回的 next_url 加载下一页推荐用户。
     *
     * @param nextUrl 上一页响应中的 `next_url`。
     */
    suspend fun getRecommendedUsers(nextUrl: String): UserPreviewsResponse = networkCall("加载更多推荐用户失败") {
        apiClient.get(nextUrl).body()
    }

    /**
     * 获取当前账号的 AI 作品显示设置。
     */
    suspend fun getUserAISettings(): ShowAIResponse = networkCall("获取 AI 显示设置失败") {
        apiClient.get("/v1/user/ai-show-settings").body()
    }

    /**
     * 更新当前账号的 AI 作品显示设置。
     *
     * @param showAI 是否显示 AI 作品。
     */
    suspend fun updateUserAISettings(showAI: Boolean): ShowAIResponse = networkCall("更新 AI 显示设置失败") {
        apiClient.post("/v1/user/ai-show-settings/edit") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("show_ai", showAI.toString())
                    },
                ),
            )
        }.body()
    }
}
