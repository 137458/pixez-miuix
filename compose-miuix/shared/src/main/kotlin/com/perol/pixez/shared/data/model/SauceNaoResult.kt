package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

/**
 * SauceNAO 以图搜图检索结果。
 */
@Serializable
data class SauceNaoResult(
    val illustId: Long,
    val title: String? = null,
    val author: String? = null,
    val similarity: String? = null,
    val thumbnail: String? = null,
)
