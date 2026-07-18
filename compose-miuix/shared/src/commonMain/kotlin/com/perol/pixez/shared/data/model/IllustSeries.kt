package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustSeriesDetailResponse(
    @SerialName("illust_series_context") val illustSeriesContext: IllustSeriesContext? = null,
    @SerialName("illust_series_detail") val illustSeriesDetail: IllustSeriesDetail? = null,
)

@Serializable
data class IllustSeriesContext(
    @SerialName("content_order") val contentOrder: Int? = null,
    val next: Illust? = null,
    val prev: Illust? = null,
)

@Serializable
data class IllustSeriesDetail(
    val height: Int,
    @SerialName("series_work_count") val seriesWorkCount: Int,
    val id: Int,
    @SerialName("create_date") val createDate: String,
    val title: String,
    val width: Int,
    @SerialName("cover_image_urls") val coverImageUrls: CoverImageUrls,
    @SerialName("watchlist_added") val watchlistAdded: Boolean,
    val caption: String,
    val user: IllustSeriesUser? = null,
)

@Serializable
data class IllustSeriesUser(
    val id: Int,
    val account: String,
    val name: String,
    @SerialName("profile_image_urls") val profileImageUrls: IllustSeriesProfileImageUrls? = null,
    @SerialName("is_followed") val isFollowed: Boolean,
)

@Serializable
data class IllustSeriesProfileImageUrls(
    val medium: String? = null,
)

@Serializable
data class CoverImageUrls(
    val medium: String? = null,
)

@Serializable
data class IllustSeriesWithIdModel(
    @SerialName("illust_series_detail") val illustSeriesDetail: IllustSeriesDetail? = null,
    @SerialName("illust_series_first_illust") val illustSeriesFirstIllust: Illust? = null,
    val illusts: List<Illust>? = null,
    @SerialName("next_url") val nextUrl: String? = null,
)
