package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingTag(
    @SerialName("trend_tags") val trendTags: List<TrendTag>,
)

@Serializable
data class TrendTag(
    val tag: String,
    @SerialName("translated_name") val translatedName: String? = null,
    val illust: TrendTagIllust,
)

@Serializable
data class TrendTagIllust(
    val id: Int,
    @SerialName("image_urls") val imageUrls: ImageUrls,
)
