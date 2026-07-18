package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreviewsResponse(
    @SerialName("user_previews") val userPreviews: List<UserPreview>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class UserPreview(
    val user: IllustUser,
    val illusts: List<Illust>,
    val novels: List<UserPreviewNovel>,
    @SerialName("is_muted") val isMuted: Boolean,
)

@Serializable
data class UserPreviewNovel(
    val id: Int,
    val title: String,
    val caption: String? = null,
    @SerialName("image_urls") val imageUrls: ImageUrls,
)
