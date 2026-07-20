package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.task.Task
import com.perol.pixez.shared.data.local.task.TaskDatabase
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTask
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.data.model.Illust

/**
 * 下载任务历史仓库：封装对旧 task.db 的读写，支持记录、查询与清理下载历史。
 *
 * 复用旧 Flutter 遗留的 `task` 表结构，status 字段映射如下：
 * - Pending = 0
 * - Downloading = 1
 * - Success = 2
 * - Failed = 3
 */
class DownloadHistoryRepository(
    driver: SqlDriver,
) {
    private val queries = TaskDatabase(driver).taskQueries

    /**
     * 保存或更新一条下载任务历史。
     *
     * 新任务传入 id <= 0 时会在事务中插入并取 [last_insert_rowid][lastInsertRowId]；
     * 已有任务传入 id 时会覆盖整行。
     *
     * 不依赖 [remoteUrl] 反查 ID，因此允许同一 URL 产生多条历史记录（重复下载）。
     */
    fun saveTask(task: DownloadTaskHistory): DownloadTaskHistory {
        return if (task.id > 0) {
            queries.insertOrReplace(
                id = task.id,
                title = task.title,
                user_name = task.userName,
                url = task.remoteUrl,
                sanity_level = task.sanityLevel?.toLong(),
                illust_id = task.illustId.toLong(),
                user_id = task.userId.toLong(),
                status = task.status.toDbValue(),
                file_name = task.fileName,
                medium = task.medium,
            )
            task
        } else {
            var newId = 0L
            queries.transaction {
                queries.insertOrReplace(
                    id = null,
                    title = task.title,
                    user_name = task.userName,
                    url = task.remoteUrl,
                    sanity_level = task.sanityLevel?.toLong(),
                    illust_id = task.illustId.toLong(),
                    user_id = task.userId.toLong(),
                    status = task.status.toDbValue(),
                    file_name = task.fileName,
                    medium = task.medium,
                )
                newId = queries.lastInsertRowId().executeAsOne()
            }
            task.copy(id = newId)
        }
    }

    /**
     * 将 [DownloadTask] 与作品信息转换为历史记录并保存或更新。
     *
     * 用于下载流程中直接持久化临时任务对象。
     *
     * @param id 已有历史记录 ID；大于 0 时覆盖对应行，否则新增。
     */
    fun saveTask(
        task: DownloadTask,
        illust: Illust,
        id: Long = 0L,
    ): DownloadTaskHistory {
        val history = DownloadTaskHistory(
            id = id,
            illustId = task.illustId,
            pageIndex = task.pageIndex,
            title = illust.title,
            userName = illust.user.name,
            remoteUrl = task.remoteUrl,
            fileName = task.fileName,
            status = task.status,
            sanityLevel = illust.sanityLevel,
            userId = illust.user.id,
            medium = illust.imageUrls.medium,
        )
        return saveTask(history)
    }

    /**
     * 查询全部历史记录，按时间倒序排列。
     */
    fun getAllTasks(): List<DownloadTaskHistory> {
        return queries.selectAllPagedDesc(
            value_ = Long.MAX_VALUE,
            value__ = 0L,
        ).executeAsList().map { it.toHistory() }
    }

    /**
     * 按状态查询历史记录，按时间倒序排列。
     */
    fun getTasksByStatus(status: DownloadStatus): List<DownloadTaskHistory> {
        return queries.selectByStatusPagedDesc(
            status = status.toDbValue(),
            value_ = Long.MAX_VALUE,
            value__ = 0L,
        ).executeAsList().map { it.toHistory() }
    }

    /**
     * 删除指定 ID 的历史记录。
     */
    fun deleteTask(id: Long) {
        queries.deleteById(id)
    }

    /**
     * 清空全部下载历史。
     */
    fun clearAll() {
        queries.deleteAll()
    }

    /**
     * 将数据库 [Task] 行转换为对外模型。
     */
    private fun Task.toHistory(): DownloadTaskHistory = DownloadTaskHistory(
        id = id,
        illustId = illust_id.toInt(),
        pageIndex = parsePageIndexFromFileName(file_name),
        title = title,
        userName = user_name,
        remoteUrl = url,
        fileName = file_name,
        status = status.toDownloadStatus(),
        sanityLevel = sanity_level?.toInt(),
        userId = user_id.toInt(),
        medium = medium,
    )

    /**
     * 从保存文件名中提取页码。
     *
     * 文件名格式由 [DownloadRepository.buildFileName] 生成：`{title}_p{index}.{ext}`。
     */
    private fun parsePageIndexFromFileName(fileName: String): Int {
        return fileName
            .substringAfterLast("_p", "0")
            .substringBefore(".", "0")
            .toIntOrNull() ?: 0
    }

    /**
     * 将 [DownloadStatus] 映射为数据库存储数值。
     */
    private fun DownloadStatus.toDbValue(): Long = when (this) {
        DownloadStatus.Pending -> 0L
        DownloadStatus.Downloading -> 1L
        DownloadStatus.Success -> 2L
        DownloadStatus.Failed -> 3L
    }

    /**
     * 将数据库数值映射为 [DownloadStatus]。
     */
    private fun Long.toDownloadStatus(): DownloadStatus = when (this) {
        0L -> DownloadStatus.Pending
        1L -> DownloadStatus.Downloading
        2L -> DownloadStatus.Success
        3L -> DownloadStatus.Failed
        else -> DownloadStatus.Failed
    }
}
