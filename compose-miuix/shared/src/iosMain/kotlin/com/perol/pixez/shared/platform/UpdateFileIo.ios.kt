package com.perol.pixez.shared.platform

/** iOS 没有应用内下载，摘要与删除都无文件可操作。 */
actual fun sha256HexOfFile(path: String): String? = null

actual fun deleteUpdateFile(path: String): Boolean = false
