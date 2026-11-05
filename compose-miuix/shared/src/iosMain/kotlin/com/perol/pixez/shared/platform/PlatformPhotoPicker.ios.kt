package com.perol.pixez.shared.platform

actual class PlatformPhotoPicker {
    actual fun pickPhoto(title: String, onResult: (byteArray: ByteArray?, fileName: String?) -> Unit) {}
}
