package com.perol.pixez.shared.platform

/** iOS 不提供应用内更新安装，下载入口统一走 Releases 页面。 */
internal actual suspend fun downloadUpdateFile(
    downloadUrl: String,
    fileName: String,
    onProgress: (downloaded: Long, total: Long) -> Unit,
): Result<String> = Result.failure(UnsupportedOperationException("iOS 暂不支持应用内更新下载"))
