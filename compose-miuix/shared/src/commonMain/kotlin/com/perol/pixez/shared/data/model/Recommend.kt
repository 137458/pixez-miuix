package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recommend(
    val illusts: List<Illust>,
    @SerialName("ranking_illusts") val rankingIllusts: List<Illust>? = null,
    @SerialName("contest_exists") val contestExists: Boolean? = null,
    @SerialName("privacy_policy") val privacyPolicy: PrivacyPolicy? = null,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
object PrivacyPolicy
