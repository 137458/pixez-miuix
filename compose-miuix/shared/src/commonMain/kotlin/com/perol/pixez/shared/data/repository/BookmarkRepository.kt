package com.perol.pixez.shared.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

/**
 * 收藏与关注仓库：封装作品收藏、用户关注等需要登录的写操作。
 */
class BookmarkRepository(
    private val apiClient: HttpClient,
) {
    /**
     * 收藏作品。
     *
     * @param illustId 作品 ID。
     * @param isPrivate 是否私密收藏（true 为非公开收藏）。
     * @param tags 可选收藏标签，逗号分隔；为空时不传。
     */
    suspend fun addBookmark(
        illustId: Int,
        isPrivate: Boolean = false,
        tags: String? = null,
    ) = networkCall("收藏作品失败 illustId=$illustId") {
        apiClient.post("/v2/illust/bookmark/add") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("illust_id", illustId.toString())
                        append("restrict", if (isPrivate) "private" else "public")
                        if (!tags.isNullOrBlank()) {
                            append("tags", tags)
                        }
                    },
                ),
            )
        }
    }

    /**
     * 取消收藏作品。
     */
    suspend fun deleteBookmark(illustId: Int) = networkCall("取消收藏失败 illustId=$illustId") {
        apiClient.post("/v1/illust/bookmark/delete") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("illust_id", illustId.toString())
                    },
                ),
            )
        }
    }

    /**
     * 关注用户。
     *
     * @param userId 用户 ID。
     * @param isPrivate 是否私密关注。
     */
    suspend fun followUser(
        userId: Int,
        isPrivate: Boolean = false,
    ) = networkCall("关注用户失败 userId=$userId") {
        apiClient.post("/v1/user/follow/add") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("user_id", userId.toString())
                        append("restrict", if (isPrivate) "private" else "public")
                    },
                ),
            )
        }
    }

    /**
     * 取消关注用户。
     */
    suspend fun unfollowUser(userId: Int) = networkCall("取消关注失败 userId=$userId") {
        apiClient.post("/v1/user/follow/delete") {
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("user_id", userId.toString())
                    },
                ),
            )
        }
    }
}
