package com.perol.pixez.shared.platform

/**
 * 跨平台应用更新包下载器。
 */
expect class AppUpdateDownloader() {
    /**
     * 下载更新安装包至本地缓存目录。
     *
     * @param downloadUrl 安装包远程下载直链。
     * @param fileName 保存文件名（如 "PixEz-MIUIX-v0.9.108.2-miuix.apk"）。
     * @param onProgress 进度回调 (progress: 0f..1f, downloadedBytes, totalBytes)。
     * @return 成功返回本地绝对路径，失败返回异常。
     */
    suspend fun download(
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<String>
}
