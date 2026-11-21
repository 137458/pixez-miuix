package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

/**
 * 作品详情 API 响应（/v1/illust/detail）。
 */
@Serializable
data class IllustDetailResponse(
    val illust: Illust,
)
