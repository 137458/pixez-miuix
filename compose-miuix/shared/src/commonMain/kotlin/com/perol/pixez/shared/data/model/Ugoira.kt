package com.perol.pixez.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UgoiraMetadataResponse(
    @SerialName("ugoira_metadata") val ugoiraMetadata: UgoiraMetadata,
)

@Serializable
data class UgoiraMetadata(
    @SerialName("zip_urls") val zipUrls: UgoiraZipUrls,
    val frames: List<UgoiraFrame>,
)

@Serializable
data class UgoiraFrame(
    val file: String,
    val delay: Int,
)

@Serializable
data class UgoiraZipUrls(
    val medium: String,
)
