package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IllustBookmarkTagsResponse(
    @SerialName("bookmark_tags") val bookmarkTags: List<IllustBookmarkTag>,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class IllustBookmarkTag(
    val name: String,
    val count: Int,
)
