package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NovelWebResponse(
    val id: String,
    val title: String,
    val seriesId: JsonElement? = null,
    val seriesTitle: JsonElement? = null,
    val seriesIsWatched: JsonElement? = null,
    val userId: String,
    val coverUrl: String,
    val tags: List<String>,
    val caption: String,
    val cdate: String,
    val rating: NovelRating,
    val text: String,
    val marker: JsonElement? = null,
    val seriesNavigation: NovelSeriesNavigation? = null,
    val glossaryItems: List<JsonElement>? = null,
    val replaceableItemIds: List<JsonElement>? = null,
    val images: Map<String, NovelWebImage>? = null,
    val illusts: Map<String, NovelWebIllusts?>? = null,
    val aiType: Int? = null,
    val isOriginal: Boolean? = null,
)

@Serializable
data class NovelWebIllusts(
    val illust: NovelWebIllust,
)

@Serializable
data class NovelWebIllust(
    val images: NovelWebIllustImages,
)

@Serializable
data class NovelWebIllustImages(
    val small: String? = null,
    val medium: String? = null,
    val original: String? = null,
)

@Serializable
data class NovelRating(
    val like: Int,
    val bookmark: Int,
    val view: Int,
)

@Serializable
data class NovelSeriesNavigation(
    val nextNovel: NovelPrevNext? = null,
    val prevNovel: NovelPrevNext? = null,
)

@Serializable
data class NovelPrevNext(
    val id: Int,
    val viewable: Boolean,
    val contentOrder: String,
    val title: String? = null,
    val coverUrl: String? = null,
)

@Serializable
data class NovelWebImage(
    val novelImageId: String? = null,
    val sl: String,
    val urls: NovelImageUrlsWeb,
)

@Serializable
data class NovelImageUrlsWeb(
    // TODO: 联调确认真实 key，可能是 @SerialName("240mw") 等
    val the240Mw: String? = null,
    val the480Mw: String? = null,
    val the1200X1200: String? = null,
    val the128X128: String? = null,
    val original: String? = null,
)
