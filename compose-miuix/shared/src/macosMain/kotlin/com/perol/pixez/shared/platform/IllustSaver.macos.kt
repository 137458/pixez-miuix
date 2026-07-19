package com.perol.pixez.shared.platform

/**
 * macOS 平台实现占位：将图片保存到应用沙盒 Documents/PixEz。
 *
 * TODO: 后续可接入 Photos 框架或文件选择器保存。
 */
actual class IllustSaver {
    actual suspend fun save(fileName: String, bytes: ByteArray): String {
        // M7 切片 1 先提供占位实现，保证 commonMain 编译通过。
        throw NotImplementedError("macOS 平台保存实现待补充")
    }
}
