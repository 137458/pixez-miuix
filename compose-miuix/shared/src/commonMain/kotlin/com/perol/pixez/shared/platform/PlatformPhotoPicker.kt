package com.perol.pixez.shared.platform

/**
 * 跨平台照片选择器抽象。
 *
 * Android 端接入系统零权限 Photo Picker（支持 Android 16 Embedded Photo Picker 原生内嵌与零存储权限安全沙盒）。
 */
expect class PlatformPhotoPicker() {
    /**
     * 选择一张图片并读取字节。
     *
     * @param title 系统对话框标题（调用方传本地化文案）；空白时用 [PlatformDialogDefaults.PICK_PHOTO_TITLE]。
     *   标题作为参数而非在 actual 内读 ui 单例，避免 platform 层反向依赖 ui（架构审查 DN15）。
     * @param onResult 选中结果回调；取消或失败时为 (null, null)。
     */
    fun pickPhoto(
        title: String = PlatformDialogDefaults.PICK_PHOTO_TITLE,
        onResult: (byteArray: ByteArray?, fileName: String?) -> Unit,
    )
}
