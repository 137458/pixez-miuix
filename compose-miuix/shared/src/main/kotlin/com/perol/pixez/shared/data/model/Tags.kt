package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AutoWords(
    val tags: List<SearchTag>,
)

@Serializable
data class SearchTag(
    val name: String,
    @SerialName("translated_name") val translatedName: String? = null,
)

@Serializable
data class TagPersist(
    @SerialName("_id") val id: Int? = null,
    val name: String,
    @SerialName("translated_name") val translatedName: String,
    val type: Int? = 0,
)
