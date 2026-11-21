package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginErrorResponse(
    @SerialName("has_error") val hasError: Boolean,
    val errors: LoginErrors,
)

@Serializable
data class LoginErrors(
    val system: LoginErrorSystem,
)

@Serializable
data class LoginErrorSystem(
    val message: String,
    val code: Int,
)
