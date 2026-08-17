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
) {
    val illustId: Int?
        get() {
            val link = arworkLink ?: return null
            val match = Regex("""artworks/(\d+)""").find(link)
                ?: Regex("""illust_id=(\d+)""").find(link)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

    val userId: Int?
        get() {
            val link = userLink ?: return null
            val match = Regex("""users/(\d+)""").find(link)
                ?: Regex("""(?:users/|id=)(\d+)""").find(link)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }
}
