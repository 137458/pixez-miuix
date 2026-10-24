package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 漫画系列追更列表响应，字段对齐旧 Flutter `watchlist_manga_model.dart` 头部注释样例。
 */
@Serializable
data class WatchlistMangaResponse(
    val series: List<MangaSeries> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

/**
 * 追更列表中的单个漫画系列条目；容错字段给默认值，防单条脏数据毒化整页。
 */
@Serializable
data class MangaSeries(
    @SerialName("mask_text") val maskText: String? = null,
    @SerialName("latest_content_id") val latestContentId: Long,
    val id: Long,
    val user: MangaSeriesUser? = null,
    val title: String,
    @SerialName("last_published_content_datetime") val lastPublishedContentDatetime: String? = null,
    @SerialName("published_content_count") val publishedContentCount: Int,
    // 系列封面地址（旧版模型字段名即 url）。
    val url: String? = null,
)

@Serializable
data class MangaSeriesUser(
    val id: Long,
    val account: String? = null,
    val name: String? = null,
    @SerialName("profile_image_urls") val profileImageUrls: MangaSeriesProfileImageUrls? = null,
)

@Serializable
data class MangaSeriesProfileImageUrls(
    val medium: String? = null,
)

/**
 * 基于系列 ID 幂等合并列表，过滤重复系列卡片，避免刷新与触底加载竞态出现重复项。
 */
fun List<MangaSeries>.appendDistinct(newItems: List<MangaSeries>): List<MangaSeries> {
    if (newItems.isEmpty()) return this
    if (this.isEmpty()) return newItems
    val existingIds = mapTo(HashSet(size + newItems.size)) { it.id }
    val uniqueNew = newItems.filter { existingIds.add(it.id) }
    return this + uniqueNew
}
