package com.perol.pixez.shared.platform

/**
 * 跨平台保存插画图片字节到本地存储。
 *
 * 各平台实现负责选择正确的保存路径与权限处理：
 * - Android：保存到公共 Pictures/PixEz 目录并刷新 MediaStore。
 * - Desktop：保存到用户主目录下的 Pictures/PixEz。
 * - iOS/macOS：保存到应用沙盒或相册（当前为占位实现）。
 */
expect class IllustSaver() {
    /**
     * 将图片字节保存为指定文件名的文件。
     *
     * @param fileName 文件名，含扩展名，如 "title_p0.jpg"。
     * @param bytes 图片二进制数据。
     * @return 保存后的可读路径或标识，失败时抛出异常。
     */
    suspend fun save(fileName: String, bytes: ByteArray): String
}
