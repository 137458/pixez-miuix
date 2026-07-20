package com.perol.pixez.shared.platform

/**
 * 跨平台原生分享工具：将文本通过系统分享面板发送给其他应用。
 *
 * 各平台实现负责选择正确的系统 API：
 * - Android：使用 `Intent.ACTION_SEND` 启动系统分享选择器。
 * - Desktop：系统分享面板不可用，回退到 `IllustClipboard.copy()` 并提示用户。
 * - iOS：使用 `UIActivityViewController` 展示系统分享面板。
 * - macOS：使用 `NSSharingServicePicker` 展示系统分享面板。
 */
expect class IllustShare() {
    /**
     * 分享 [text]。
     *
     * [subject] 作为分享的标题/主题，部分平台（如邮件）会展示。
     * 失败时抛出异常，由调用方处理并提示用户。
     */
    fun share(text: String, subject: String? = null)
}
