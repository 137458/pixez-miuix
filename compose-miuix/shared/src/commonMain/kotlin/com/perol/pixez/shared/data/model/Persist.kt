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
)

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


