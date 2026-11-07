package com.perol.pixez.shared.platform

import com.perol.pixez.shared.network.sha256File
import java.io.File

/** Android/Desktop 共用：流式读取文件计算 SHA-256，避免整包载入内存。 */
actual fun sha256HexOfFile(path: String): String? = runCatching {
    sha256File(File(path))
}.getOrNull()

actual fun deleteUpdateFile(path: String): Boolean = runCatching {
    File(path).delete()
}.getOrDefault(false)
