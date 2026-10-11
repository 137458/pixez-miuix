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
    val id: Long,
    val title: String,
    @SerialName("pure_title") val pureTitle: String,
    val thumbnail: String,
    @SerialName("article_url") val articleUrl: String,
    @SerialName("publish_date") val publishDate: String,
)

/**
 * 基于特辑文章 ID 幂等合并列表，过滤重复项，避免 Compose LazyLayout 因重复 key 闪退。
 */
fun List<SpotlightArticle>.appendDistinct(newItems: List<SpotlightArticle>): List<SpotlightArticle> {
    if (newItems.isEmpty()) return this
    if (this.isEmpty()) return newItems
    val existingIds = mapTo(HashSet(size + newItems.size)) { it.id }
    val uniqueNew = newItems.filter { existingIds.add(it.id) }
    return this + uniqueNew
}

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
