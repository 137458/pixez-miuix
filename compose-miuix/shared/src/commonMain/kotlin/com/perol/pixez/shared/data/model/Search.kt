package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 搜索插画 API 响应（/v1/search/illust）。
 */
@Serializable
data class Search(
    val illusts: List<Illust>,
    @SerialName("next_url") val nextUrl: String? = null,
    @SerialName("search_span_limit") val searchSpanLimit: Int? = null,
)
