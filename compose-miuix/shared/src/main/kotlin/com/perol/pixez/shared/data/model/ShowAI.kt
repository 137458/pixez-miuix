package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowAIResponse(
    @SerialName("show_ai") val showAI: Boolean,
)
