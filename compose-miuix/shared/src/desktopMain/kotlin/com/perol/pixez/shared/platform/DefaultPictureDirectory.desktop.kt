package com.perol.pixez.shared.platform

import java.io.File

actual fun getDefaultPictureDirectory(): String {
    val userHome = System.getProperty("user.home")
        ?: throw IllegalStateException("无法获取用户主目录")
    return File(userHome, "Pictures/PixEz").absolutePath
}
