package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.illustpersist.Illustpersist

/**
 * 插画浏览历史仓库：封装对旧 Flutter `illustpersist.db` 的查询与清理。
 *
 * 仅消费已有历史记录，不新增埋点；表结构保持与旧版一致。
 */
class HistoryRepository(
    driver: SqlDriver,
) {
    private val queries = IllustPersistDatabase(driver).illustPersistQueries

    /**
     * 查询全部浏览历史，按时间升序排列（与旧版 `ORDER BY time` 一致）。
     */
    fun getAll(): List<HistoryItem> {
        return queries.selectAll().executeAsList().map { it.toHistoryItem() }
    }

    /**
     * 删除指定主键的历史记录。
     *
     * 使用自增主键 [id] 而非 [illust_id]，避免同作品多次浏览时误删多条记录。
     */
    fun deleteById(id: Long) {
        queries.deleteById(id)
    }

    /**
     * 清空全部浏览历史。
     */
    fun clearAll() {
        queries.deleteAll()
    }

    /**
     * 将数据库行转换为对外模型。
     */
    private fun Illustpersist.toHistoryItem(): HistoryItem = HistoryItem(
        id = id,
        illustId = illust_id.toInt(),
        userId = user_id.toInt(),
        pictureUrl = picture_url,
        title = title,
        userName = user_name,
        time = time,
    )
}

/**
 * 浏览历史展示模型，仅包含 UI 所需的字段。
 */
data class HistoryItem(
    val id: Long,
    val illustId: Int,
    val userId: Int,
    val pictureUrl: String,
    val title: String?,
    val userName: String?,
    val time: Long,
)
