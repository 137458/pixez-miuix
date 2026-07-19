package com.perol.pixez.shared.data.model

/**
 * 插画下载任务状态。
 */
enum class DownloadStatus {
    Pending,
    Downloading,
    Success,
    Failed,
}

/**
 * 插画下载任务。
 *
 * @param illustId 作品 ID
 * @param pageIndex 页码，单页作品为 0
 * @param remoteUrl 远程图片 URL
 * @param fileName 保存文件名（含扩展名）
 * @param status 当前状态
 * @param error 错误信息
 */
data class DownloadTask(
    val illustId: Int,
    val pageIndex: Int,
    val remoteUrl: String,
    val fileName: String,
    val status: DownloadStatus = DownloadStatus.Pending,
    val error: String? = null,
)
