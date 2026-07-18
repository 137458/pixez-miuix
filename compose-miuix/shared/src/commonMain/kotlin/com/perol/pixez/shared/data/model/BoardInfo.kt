package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BoardInfo(
    val title: String,
    val content: String,
    val startDate: String,
    val endDate: String? = null,
)
