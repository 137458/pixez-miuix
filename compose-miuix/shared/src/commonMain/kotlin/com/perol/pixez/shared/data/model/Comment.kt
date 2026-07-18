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
