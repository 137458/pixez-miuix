package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth 登录响应根对象。
 */
@Serializable
data class Account(
    val response: AccountResponse,
)

/**
 * OAuth token 与登录用户信息。
 */
@Serializable
data class AccountResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    val scope: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: OAuthUser,
)

/**
 * OAuth 返回的用户信息（字段与旧版 account.dart 保持一致）。
 */
@Serializable
data class OAuthUser(
    @SerialName("profile_image_urls") val profileImageUrls: OAuthProfileImageUrls,
    val id: String,
    val name: String,
    val account: String,
    @SerialName("mail_address") val mailAddress: String,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_mail_authorized") val isMailAuthorized: Boolean,
    @SerialName("require_policy_agreement") val requirePolicyAgreement: Boolean? = null,
)

@Serializable
data class OAuthProfileImageUrls(
    @SerialName("px_16x16") val px16x16: String,
    @SerialName("px_50x50") val px50x50: String,
    @SerialName("px_170x170") val px170x170: String,
)
