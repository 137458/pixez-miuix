package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnezeroResponse(
    @SerialName("Answer") val answer: List<OnezeroAnswer>,
)

@Serializable
data class OnezeroAnswer(
    val name: String,
    val type: Int,
    val data: String,
    @SerialName("TTL") val ttl: Int,
)
