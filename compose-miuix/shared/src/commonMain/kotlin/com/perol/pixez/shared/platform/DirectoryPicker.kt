package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable

/**
 * 跨平台系统目录/文件夹选择器。
 *
 * Android 端直接调用系统 SAF 文件选择器（OpenDocumentTree），自动获取并持久化目录授权；
 * Desktop 端调用系统原生文件夹选择对话框；
 * 其他平台提供平滑降级。
 *
 * @param onResult 选定路径后的回调，参数为解析后的绝对路径字符串（或 Uri 字符串），取消或失败时为 null。
 * @return 触发选择器的无参函数。
 */
@Composable
expect fun rememberDirectoryPicker(onResult: (String?) -> Unit): () -> Unit
