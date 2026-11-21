package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户作品列表 API 响应（/v1/user/illusts）。
 */
@Serializable
data class UserIllusts(
    val illusts: List<Illust>,
    @SerialName("next_url") val nextUrl: String? = null,
)
