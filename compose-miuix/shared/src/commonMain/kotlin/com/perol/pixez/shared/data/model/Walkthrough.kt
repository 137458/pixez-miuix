package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

/**
 * 未登录 walkthrough 匿名推荐响应。
 */
@Serializable
data class Walkthrough(
    val illusts: List<Illust>,
)
