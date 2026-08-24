package com.perol.pixez.shared.platform

actual class AppInstaller actual constructor() {
    actual fun install(filePath: String): Boolean = false
    actual fun getUpdateSaveDir(): String = ""
}
