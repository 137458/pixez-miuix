package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FollowDetail(
    @SerialName("is_followed") val isFollowed: Boolean,
    val restrict: String,
)
