package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    @SerialName("total_comments") val totalComments: Int? = null,
    val comments: List<Comment>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class Comment(
    val id: Int? = null,
    val comment: String? = null,
    val date: String? = null,
    val user: CommentUser? = null,
    @SerialName("parent_comment") val parentComment: Comment? = null,
    @SerialName("has_replies") val hasReplies: Boolean? = null,
    val stamp: CommentStamp? = null,
)

@Serializable
data class CommentUser(
    val id: Int? = null,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: CommentProfileImageUrls,
)

@Serializable
data class CommentProfileImageUrls(
    val medium: String,
)

@Serializable
data class CommentStamp(
    @SerialName("stamp_id") val stampId: Int? = null,
    @SerialName("stamp_url") val stampUrl: String? = null,
)

/**
 * 基于评论 ID 幂等合并列表，过滤重复评论，避免刷新与触底加载竞态出现重复项。
 * id 为 null 的异常数据不参与去重，直接保留。
 */
fun List<Comment>.appendDistinct(newItems: List<Comment>): List<Comment> {
    if (newItems.isEmpty()) return this
    if (this.isEmpty()) return newItems
    val existingIds = mapNotNullTo(HashSet(size + newItems.size)) { it.id }
    val uniqueNew = newItems.filter { it.id == null || existingIds.add(it.id) }
    return this + uniqueNew
}
