package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotlightResponse(
    @SerialName("spotlight_articles") val spotlightArticles: List<SpotlightArticle>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class SpotlightArticle(
    val id: Int,
    val title: String,
    @SerialName("pure_title") val pureTitle: String,
    val thumbnail: String,
    @SerialName("article_url") val articleUrl: String,
    @SerialName("publish_date") val publishDate: String,
)

@Serializable
data class SpotlightDetail(
    val title: String,
    val pureTitle: String = "",
    val description: String = "",
    val coverUrl: String? = null,
    val works: List<AmWork> = emptyList(),
    val subArticles: List<SpotlightArticle> = emptyList(),
    val rawUrl: String = "",
) {
    val isCollection: Boolean
        get() = subArticles.isNotEmpty() || (works.isEmpty() && (title.contains("合集") || title.contains("合辑")))
}
