package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookmarkResponse(
    @SerialName("bookmark_detail") val bookmarkDetail: BookmarkDetailBody,
)

@Serializable
data class BookmarkDetailBody(
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    val tags: List<BookmarkTagEntry>,
    val restrict: String,
)

@Serializable
data class BookmarkTagEntry(
    val name: String,
    @SerialName("is_registered") val isRegistered: Boolean,
)

@Serializable
data class BookmarkDetailResponse(
    @SerialName("bookmark_detail") val bookmarkDetail: BookmarkDetail,
)

@Serializable
data class BookmarkDetail(
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    val tags: List<BookmarkDetailTag>,
    val restrict: String,
)

@Serializable
data class BookmarkDetailTag(
    val name: String,
    @SerialName("is_registered") val isRegistered: Boolean,
)
