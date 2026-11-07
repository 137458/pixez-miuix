package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountPersist(
    val id: Int? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("user_image") val userImage: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("device_token") val deviceToken: String,
    val name: String,
    val account: String,
    @SerialName("mail_address") val mailAddress: String,
    @SerialName("password") val passWord: String,
    @SerialName("is_premium") val isPremium: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("is_mail_authorized") val isMailAuthorized: Int,
) {
    /**
     * 是否仍持有可用凭证。解密失败或被判定吊销后账号行会保留但 token 被清空，
     * 登录态判定必须用它而不是「账号行是否存在」。
     */
    val hasCredentials: Boolean
        get() = accessToken.isNotBlank() && refreshToken.isNotBlank()
}

/**
 * 登录态判定入口：账号行存在且仍持有可用凭证。
 * UI 层统一用它判定登录态，不要直接比较账号行是否为 null。
 */
val AccountPersist?.isLoggedIn: Boolean
    get() = this?.hasCredentials == true

@Serializable
data class BanIllustIdPersist(
    @SerialName("illust_id") val illustId: String,
    val name: String,
    val id: Int? = null,
)

@Serializable
data class BanTagPersist(
    val id: Int? = null,
    val name: String,
    @SerialName("translate_name") val translateName: String,
)

@Serializable
data class BanUserIdPersist(
    @SerialName("user_id") val userId: String? = null,
    val id: Int? = null,
    val name: String? = null,
)

@Serializable
data class IllustPersist(
    val id: Int? = null,
    @SerialName("illust_id") val illustId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("user_name") val userName: String? = null,
    val title: String? = null,
    val time: Int,
)

@Serializable
data class NovelPersist(
    val id: Int? = null,
    @SerialName("novel_id") val novelId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("picture_url") val pictureUrl: String,
    val time: Int,
    val title: String,
    @SerialName("user_name") val userName: String,
)


