package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountEditResponse(
    val error: Boolean,
    val message: String,
    val body: AccountEditBody,
)

@Serializable
data class AccountEditBody(
    @SerialName("is_succeed") val isSucceed: Boolean,
    @SerialName("validation_errors") val validationErrors: ValidationErrors,
)

@Serializable
object ValidationErrors
