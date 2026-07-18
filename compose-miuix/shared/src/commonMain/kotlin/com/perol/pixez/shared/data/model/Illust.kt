package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 插画作品详情（对应旧版 illust.dart 中的 Illusts）。
 */
@Serializable
data class Illust(
    val id: Int,
    val title: String,
    val type: String,
    @SerialName("image_urls") val imageUrls: ImageUrls,
    val caption: String,
    val restrict: Int,
    val user: IllustUser,
    val tags: List<IllustTag>,
    val tools: List<String>,
    @SerialName("create_date") val createDate: String,
    @SerialName("page_count") val pageCount: Int,
    val width: Int,
    val height: Int,
    @SerialName("sanity_level") val sanityLevel: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("meta_single_page") val metaSinglePage: MetaSinglePage? = null,
    @SerialName("meta_pages") val metaPages: List<MetaPage>,
    @SerialName("total_view") val totalView: Int,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    val visible: Boolean,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("illust_ai_type") val illustAIType: Int,
    val series: IllustSeries? = null,
    @SerialName("illust_book_style") val illustBookStyle: Int? = null,
    @SerialName("total_comments") val totalComments: Int? = null,
)

@Serializable
data class ImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
)

@Serializable
data class IllustUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: IllustProfileImageUrls,
    val comment: String? = null,
    @SerialName("is_followed") val isFollowed: Boolean? = null,
)

@Serializable
data class IllustProfileImageUrls(
    val medium: String,
)

@Serializable
data class IllustTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
)

@Serializable
data class MetaSinglePage(
    @SerialName("original_image_url") val originalImageUrl: String? = null,
)

@Serializable
data class MetaPage(
    @SerialName("image_urls") val imageUrls: MetaPageImageUrls? = null,
)

@Serializable
data class MetaPageImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
    val original: String,
)

@Serializable
data class IllustSeries(
    val id: Int,
    val title: String? = null,
)
