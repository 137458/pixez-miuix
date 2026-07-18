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
data class BanCommentPersist(
    @SerialName("comment_id") val commentId: String,
    val name: String,
    val id: Int? = null,
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
data class GlanceIllustPersist(
    val id: Int? = null,
    @SerialName("illust_id") val illustId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("large_url") val largeUrl: String? = null,
    @SerialName("user_name") val userName: String? = null,
    val title: String? = null,
    val type: String,
    val time: Int,
)

@Serializable
data class IllustPersist(
    val id: Int? = null,
    @SerialName("illust_id") val illustId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("user_name") val userName: String? = null,
    val title: String? = null,
    val time: Int,
)

@Serializable
data class KVPair(
    val key: String,
    val value: String,
    @SerialName("expire_time") val expireTime: Int,
    @SerialName("date_time") val dateTime: Int,
)

@Serializable
data class NovelPersist(
    val id: Int? = null,
    @SerialName("novel_id") val novelId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    val time: Int,
    val title: String,
    @SerialName("user_name") val userName: String,
)

@Serializable
data class NovelViewerPersist(
    val id: Int? = null,
    @SerialName("novel_id") val novelId: Int,
    val offset: Double,
)

@Serializable
data class TaskPersist(
    val id: Int? = null,
    @SerialName("user_name") val userName: String,
    @SerialName("file_name") val fileName: String,
    val title: String,
    val url: String,
    val medium: String? = null,
    @SerialName("user_id") val userId: Int,
    @SerialName("illust_id") val illustId: Int,
    @SerialName("sanity_level") val sanityLevel: Int,
    val status: Int,
)

@Serializable
data class TagExportData(
    @SerialName("tagHisotry") val tagHistory: List<TagPersist>? = null,
    val bookTags: List<String>? = null,
)
