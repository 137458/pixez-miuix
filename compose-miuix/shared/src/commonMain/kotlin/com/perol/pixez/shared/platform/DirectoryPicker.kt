package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable

/**
 * 跨平台系统目录/文件夹选择器。
 *
 * Android 端直接调用系统 SAF 文件选择器（OpenDocumentTree），自动获取并持久化目录授权；
 * Desktop 端调用系统原生文件夹选择对话框；
 * 其他平台提供平滑降级。
 *
 * 初始目录与标题由调用方传入：platform 层不读 ui 的 CompositionLocal（分层单向，架构审查 DN15）。
 * 未传时桌面端回退到默认图片目录与 [PlatformDialogDefaults.PICK_DIRECTORY_TITLE]。
 *
 * @param initialDirectory 对话框初始目录；为 null/空白时用平台默认图片目录。
 * @param dialogTitle 对话框标题（调用方传本地化文案）；为 null/空白时用中性缺省标题。
 * @param onResult 选定路径后的回调，参数为解析后的绝对路径字符串（或 Uri 字符串），取消或失败时为 null。
 * @return 触发选择器的无参函数。
 */
@Composable
expect fun rememberDirectoryPicker(
    initialDirectory: String? = null,
    dialogTitle: String? = null,
    onResult: (String?) -> Unit,
): () -> Unit
