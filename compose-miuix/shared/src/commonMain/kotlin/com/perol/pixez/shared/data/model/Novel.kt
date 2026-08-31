package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelRecomResponse(
    val novels: List<Novel>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class Novel(
    val id: Int,
    val title: String,
    val caption: String,
    val restrict: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_original") val isOriginal: Boolean,
    @SerialName("image_urls") val imageUrls: NovelImageUrls,
    @SerialName("create_date") val createDate: String,
    val tags: List<NovelTag>,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_length") val textLength: Int,
    val user: NovelUser,
    // 旧版 novel_recom_response 中 Series 的 id/title 均可空，说明 novel 不一定属于 series。
    val series: NovelSeries? = null,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("total_view") val totalView: Int,
    val visible: Boolean,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("is_mypixiv_only") val isMypixivOnly: Boolean,
    @SerialName("is_x_restricted") val isXRestricted: Boolean,
    @SerialName("novel_ai_type") val novelAIType: Int,
)

@Serializable
data class NovelImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
)

@Serializable
data class NovelSeries(
    val id: Int? = null,
    val title: String? = null,
)

@Serializable
data class NovelTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
    @SerialName("added_by_uploaded_user") val addedByUploadedUser: Boolean,
)

@Serializable
data class NovelUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: NovelProfileImageUrls,
    @SerialName("is_followed") val isFollowed: Boolean,
)

@Serializable
data class NovelProfileImageUrls(
    val medium: String,
)

@Serializable
data class NovelSeriesSeries(
    val id: Int,
    val title: String,
)

@Serializable
data class NovelSeriesNovelTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
    @SerialName("added_by_uploaded_user") val addedByUploadedUser: Boolean,
)

@Serializable
data class NovelSeriesNovel(
    val id: Int,
    val title: String,
    val caption: String? = null,
    val restrict: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_original") val isOriginal: Boolean? = null,
    @SerialName("image_urls") val imageUrls: NovelSeriesImageUrls,
    @SerialName("create_date") val createDate: String,
    val tags: List<NovelSeriesNovelTag>,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_length") val textLength: Int,
    val user: NovelSeriesUser,
    val series: NovelSeriesSeries,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("total_view") val totalView: Int,
    val visible: Boolean,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("is_mypixiv_only") val isMypixivOnly: Boolean,
    @SerialName("is_x_restricted") val isXRestricted: Boolean,
    @SerialName("novel_ai_type") val novelAIType: Int,
)

@Serializable
 data class NovelSeriesDetail(
    val id: Int,
    val title: String,
    val caption: String? = null,
    @SerialName("is_original") val isOriginal: Boolean,
    @SerialName("is_concluded") val isConcluded: Boolean,
    @SerialName("content_count") val contentCount: Int,
    @SerialName("total_character_count") val totalCharacterCount: Int,
    val user: NovelSeriesUser,
    @SerialName("display_text") val displayText: String,
    @SerialName("novel_ai_type") val novelAIType: Int,
    @SerialName("watchlist_added") val watchlistAdded: Boolean? = null,
)

@Serializable
 data class NovelSeriesUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: NovelSeriesProfileImageUrls,
    @SerialName("is_followed") val isFollowed: Boolean,
    @SerialName("is_access_blocking_user") val isAccessBlockingUser: Boolean,
)

@Serializable
 data class NovelSeriesProfileImageUrls(
    val medium: String,
)

@Serializable
 data class NovelSeriesFirstNovel(
    val id: Int,
    val title: String,
    val caption: String,
    val restrict: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_original") val isOriginal: Boolean,
    @SerialName("image_urls") val imageUrls: NovelSeriesImageUrls,
    @SerialName("create_date") val createDate: String,
    val tags: List<NovelSeriesNovelTag>,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_length") val textLength: Int,
    val user: NovelSeriesUser,
    val series: NovelSeriesSeries,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("total_view") val totalView: Int,
    val visible: Boolean,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("is_muted") val isMuted: Boolean? = null,
    @SerialName("is_my_pixiv_only") val isMypixivOnly: Boolean? = null,
    @SerialName("is_X_restricted") val isXRestricted: Boolean? = null,
    @SerialName("novel_ai_type") val novelAIType: Int,
)

@Serializable
data class NovelSeriesImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
)

@Serializable
data class NovelSeriesResponse(
    @SerialName("novel_series_detail") val novelSeriesDetail: NovelSeriesDetail,
    @SerialName("novel_series_first_novel") val novelSeriesFirstNovel: NovelSeriesFirstNovel,
    @SerialName("novel_series_latest_novel") val novelSeriesLatestNovel: NovelSeriesFirstNovel? = null,
    val novels: List<Novel>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class NovelTextResponse(
    @SerialName("novel_marker") val novelMarker: NovelMarker,
    @SerialName("novel_text") val novelText: String,
    @SerialName("series_prev") val seriesPrev: TextNovel? = null,
    @SerialName("series_next") val seriesNext: TextNovel? = null,
)

@Serializable
data class NovelMarker(
    val page: Int? = null,
)

@Serializable
data class TextNovel(
    val id: Int? = null,
    val title: String? = null,
)

@Serializable
data class NovelWatchListModel(
    val series: List<NovelWatchListSeries>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class NovelWatchListSeries(
    val id: Int,
    val title: String,
    val url: String? = null,
    @SerialName("mask_text") val maskText: String? = null,
    @SerialName("published_content_count") val publishedContentCount: Int,
    @SerialName("last_published_content_datetime") val lastPublishedContentDatetime: String,
    @SerialName("latest_content_id") val latestContentId: Int,
    val user: NovelWatchListSeriesUser? = null,
)

@Serializable
data class NovelWatchListSeriesUser(
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: NovelWatchListSeriesProfileImageUrls? = null,
    @SerialName("is_accept_request") val isAcceptRequest: Boolean,
)

@Serializable
data class NovelWatchListSeriesProfileImageUrls(
    val medium: String? = null,
)

/**
 * 判断小说作品是否为 R-18 / 18+ 敏感内容。
 *
 * 判定依据：
 * 1. xRestrict > 0 或 isXRestricted 为 true
 * 2. 标签原名或翻译包含 "R-18", "R18", "18禁", "18+", "R-18G", "R18G"（忽略大小写）
 */
fun Novel.isR18(): Boolean {
    if (xRestrict > 0 || isXRestricted) return true
    return tags.any { isSensitiveTag(it.name, it.translatedName) }
}


