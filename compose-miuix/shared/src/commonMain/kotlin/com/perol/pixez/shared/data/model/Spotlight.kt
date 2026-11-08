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
fun List<SpotlightArticle>.appendDistinct(newItems: List<SpotlightArticle>): List<SpotlightArticle> =
    appendDistinctBy(newItems) { it.id }

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
    /**
     * 合集判定只看结构化字段：抓到了子文章才算合集。
     *
     * 旧实现还会在「无作品且标题包含中文『合集/合辑』」时判为合集，
     * 该启发式在非中文语境与改标题后都会失效，且与解析结果无关，故移除。
     */
    val isCollection: Boolean
        get() = subArticles.isNotEmpty()
}
