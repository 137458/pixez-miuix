package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 关注插画 API 响应（/v2/illust/follow）。
 */
@Serializable
data class FollowIllusts(
    val illusts: List<Illust>,
    @SerialName("next_url") val nextUrl: String? = null,
)
