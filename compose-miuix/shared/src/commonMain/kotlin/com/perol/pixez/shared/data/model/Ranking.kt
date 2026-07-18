package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 排行榜 API 响应（/v1/illust/ranking）。
 */
@Serializable
data class Ranking(
    val illusts: List<Illust>,
    @SerialName("next_url") val nextUrl: String? = null,
)
