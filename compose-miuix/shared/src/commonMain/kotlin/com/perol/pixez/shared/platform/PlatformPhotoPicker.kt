package com.perol.pixez.shared.platform

/**
 * 跨平台照片选择器抽象。
 *
 * Android 端接入系统零权限 Photo Picker（支持 Android 16 Embedded Photo Picker 原生内嵌与零存储权限安全沙盒）。
 */
expect class PlatformPhotoPicker() {
    fun pickPhoto(onResult: (byteArray: ByteArray?, fileName: String?) -> Unit)
}
