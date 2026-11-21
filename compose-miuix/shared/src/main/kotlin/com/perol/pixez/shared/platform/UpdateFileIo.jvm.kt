package com.perol.pixez.shared.platform

import com.perol.pixez.shared.network.sha256File
import java.io.File

/** 流式读取文件计算 SHA-256，避免整包载入内存；读取失败返回 null。 */
fun sha256HexOfFile(path: String): String? = runCatching {
    sha256File(File(path))
}.getOrNull()

/** 删除下载结果文件；返回是否删除成功（失败不影响状态复位）。 */
fun deleteUpdateFile(path: String): Boolean = runCatching {
    File(path).delete()
}.getOrDefault(false)
