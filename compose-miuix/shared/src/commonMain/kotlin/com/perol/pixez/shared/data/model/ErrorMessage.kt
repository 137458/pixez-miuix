package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorMessage(
    val error: ApiError,
)

@Serializable
data class ApiError(
    @SerialName("user_message") val userMessage: String? = null,
    val message: String? = null,
    val reason: String? = null,
    @SerialName("user_message_details") val userMessageDetails: JsonElement? = null,
)
