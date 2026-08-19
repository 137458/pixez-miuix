package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.illustpersist.Illustpersist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
     * 查询浏览历史，按时间升序排列（与旧版 `ORDER BY time` 一致）。
     *
     * @param limit 最大返回条数，防止一次性加载过多历史记录导致内存/卡顿问题。
     */
    fun getAll(limit: Long = 1000L): List<HistoryItem> {
        return queries.selectAll(limit).executeAsList().map { it.toHistoryItem() }
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
     * 导入时批量替换浏览历史。
     *
     * 先清空旧记录，再按导入数据重新写入，避免重复记录与主键冲突。
     * 操作在 [Dispatchers.Default] 中执行并在事务内完成。
     */
    suspend fun replaceAll(items: List<HistoryItem>) = withContext(Dispatchers.Default) {
        queries.transaction {
            queries.deleteAll()
            items.forEach { item ->
                queries.insertOrReplace(
                    id = item.id,
                    illust_id = item.illustId,
                    user_id = item.userId,
                    picture_url = item.pictureUrl,
                    title = item.title,
                    user_name = item.userName,
                    time = item.time,
                )
            }
        }
    }

    /**
     * 将数据库行转换为对外模型。
     */
    private fun Illustpersist.toHistoryItem(): HistoryItem = HistoryItem(
        id = id,
        // 数据库中 illust_id/user_id 以 INTEGER 存储，SQLite 最大可存 64 位有符号整数，
        // 保持 Long 避免转 Int 时对大号 ID 产生截断/溢出。
        illustId = illust_id,
        userId = user_id,
        pictureUrl = picture_url,
        title = title,
        userName = user_name,
        time = time,
    )
}

/**
 * 浏览历史展示模型，仅包含 UI 所需的字段。
 * 字段名与旧 Flutter `IllustPersist` 的 JSON 序列化键保持一致。
 */
@Serializable
data class HistoryItem(
    @SerialName("id") val id: Long,
    // illust_id/user_id 在 SQLite 中以 64 位 INTEGER 存储，保持 Long 避免转 Int 时溢出。
    @SerialName("illust_id") val illustId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("title") val title: String?,
    @SerialName("user_name") val userName: String?,
    @SerialName("time") val time: Long,
)
