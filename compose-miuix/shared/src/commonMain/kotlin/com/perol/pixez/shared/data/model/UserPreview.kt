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

/**
 * 基于用户 ID 幂等合并列表，过滤重复用户卡片，避免刷新与触底加载竞态出现重复项。
 */
fun List<UserPreview>.appendDistinct(newItems: List<UserPreview>): List<UserPreview> {
    if (newItems.isEmpty()) return this
    if (this.isEmpty()) return newItems
    val existingIds = mapTo(HashSet(size + newItems.size)) { it.user.id }
    val uniqueNew = newItems.filter { existingIds.add(it.user.id) }
    return this + uniqueNew
}
