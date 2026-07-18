package com.perol.pixez.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AmWork(
    val title: String? = null,
    val user: String? = null,
    val arworkLink: String? = null,
    val userLink: String? = null,
    val userImage: String? = null,
    val showImage: String? = null,
)
