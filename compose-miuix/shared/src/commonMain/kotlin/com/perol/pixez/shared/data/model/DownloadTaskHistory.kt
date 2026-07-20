package com.perol.pixez.shared.data.model

/**
 * 下载任务历史记录，对应本地 task 表的一行。
 *
 * @param id 本地数据库自增 ID
 * @param illustId 作品 ID
 * @param pageIndex 页码，单页作品为 0
 * @param title 作品标题
 * @param userName 画师名称
 * @param remoteUrl 远程图片 URL
 * @param fileName 保存文件名
 * @param status 下载状态
 * @param sanityLevel 作品 sanity level
 * @param userId 画师 ID
 * @param medium 作品 medium 缩略图 URL
 */
data class DownloadTaskHistory(
    val id: Long,
    val illustId: Int,
    val pageIndex: Int,
    val title: String,
    val userName: String,
    val remoteUrl: String,
    val fileName: String,
    val status: DownloadStatus,
    val sanityLevel: Int? = null,
    val userId: Int = 0,
    val medium: String? = null,
)
