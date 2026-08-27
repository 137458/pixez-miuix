package com.perol.pixez.shared.platform

/**
 * 跨平台剪贴板工具：将文本写入系统剪贴板。
 *
 * 各平台实现负责选择正确的系统 API：
 * - Android：使用 `ClipboardManager.setPrimaryClip()`。
 * - Desktop：使用 `java.awt.datatransfer`。
 * - iOS：使用 `UIPasteboard.general.string`。
 * - macOS：使用 `NSPasteboard`。
 */
expect class IllustClipboard() {
    /**
     * 将 [text] 复制到系统剪贴板。
     *
     * 失败时抛出异常，由调用方处理并提示用户。
     */
    fun copy(text: String)

    /**
     * 读取系统剪贴板中的纯文本内容，剪贴板为空或不支持时返回 null。
     */
    fun getText(): String?
}
