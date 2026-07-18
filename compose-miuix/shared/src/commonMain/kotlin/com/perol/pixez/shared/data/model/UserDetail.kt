package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户详情（对应旧版 user_detail.dart）。
 */
@Serializable
data class UserDetail(
    val user: IllustUser,
    val profile: Profile,
    @SerialName("profile_publicity") val profilePublicity: ProfilePublicity,
    val workspace: Workspace,
)

@Serializable
data class Profile(
    val webpage: String? = null,
    val gender: String? = null,
    val birth: String? = null,
    @SerialName("birth_day") val birthDay: String? = null,
    @SerialName("birth_year") val birthYear: Int? = null,
    val region: String? = null,
    @SerialName("address_id") val addressId: Int? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val job: String? = null,
    @SerialName("job_id") val jobId: Int? = null,
    @SerialName("total_follow_users") val totalFollowUsers: Int,
    @SerialName("total_mypixiv_users") val totalMypixivUsers: Int,
    @SerialName("total_illusts") val totalIllusts: Int,
    @SerialName("total_manga") val totalManga: Int,
    @SerialName("total_novels") val totalNovels: Int,
    @SerialName("total_illust_bookmarks_public") val totalIllustBookmarksPublic: Int,
    @SerialName("total_illust_series") val totalIllustSeries: Int,
    @SerialName("total_novel_series") val totalNovelSeries: Int,
    @SerialName("background_image_url") val backgroundImageUrl: String? = null,
    @SerialName("twitter_account") val twitterAccount: String? = null,
    @SerialName("twitter_url") val twitterUrl: String? = null,
    @SerialName("pawoo_url") val pawooUrl: String? = null,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("is_using_custom_profile_image") val isUsingCustomProfileImage: Boolean,
)

@Serializable
data class ProfilePublicity(
    val gender: String,
    val region: String,
    @SerialName("birth_day") val birthDay: String,
    @SerialName("birth_year") val birthYear: String,
    val job: String,
    val pawoo: Boolean,
)

@Serializable
data class Workspace(
    val pc: String,
    val monitor: String,
    val tool: String,
    val scanner: String,
    val tablet: String,
    val mouse: String,
    val printer: String,
    val desktop: String,
    val music: String,
    val desk: String,
    val chair: String,
    val comment: String,
    @SerialName("workspace_image_url") val workspaceImageUrl: String? = null,
)
