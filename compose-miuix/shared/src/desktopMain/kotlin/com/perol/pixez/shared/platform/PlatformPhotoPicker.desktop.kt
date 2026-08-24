package com.perol.pixez.shared.platform

actual class PlatformPhotoPicker {
    actual fun pickPhoto(onResult: (byteArray: ByteArray?, fileName: String?) -> Unit) {}
}
