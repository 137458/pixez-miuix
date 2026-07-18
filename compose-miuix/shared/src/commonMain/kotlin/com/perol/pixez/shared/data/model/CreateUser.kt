package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserResponse(
    val error: Boolean,
    val message: String,
    val body: CreateUserBody,
)

@Serializable
data class CreateUserBody(
    @SerialName("user_account") val userAccount: String,
    val password: String,
    @SerialName("device_token") val deviceToken: String,
)
