package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchlistMangaModel(
    val series: List<WatchlistMangaSeries>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class WatchlistMangaSeries(
    @SerialName("mask_text") val maskText: String? = null,
    @SerialName("latest_content_id") val latestContentId: Int,
    val id: Int,
    val user: WatchlistMangaSeriesUser? = null,
    val title: String,
    @SerialName("last_published_content_datetime") val lastPublishedContentDatetime: String? = null,
    @SerialName("published_content_count") val publishedContentCount: Int,
    val url: String? = null,
)

@Serializable
data class WatchlistMangaSeriesUser(
    val id: Int,
    val account: String? = null,
    val name: String? = null,
    val profileImageUrls: WatchlistMangaSeriesProfileImageUrls? = null,
)

@Serializable
data class WatchlistMangaSeriesProfileImageUrls(
    val medium: String? = null,
)
