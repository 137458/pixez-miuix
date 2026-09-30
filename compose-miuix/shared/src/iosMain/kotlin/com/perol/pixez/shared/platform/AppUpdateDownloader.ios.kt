package com.perol.pixez.shared.platform

actual class AppUpdateDownloader actual constructor() {
    actual suspend fun download(
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
        sha256Digest: String?,
    ): Result<String> = Result.failure(UnsupportedOperationException("iOS 暂不支持应用内更新下载"))
}
