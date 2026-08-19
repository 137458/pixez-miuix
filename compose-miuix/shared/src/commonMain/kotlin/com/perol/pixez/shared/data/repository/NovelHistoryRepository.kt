package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import com.perol.pixez.shared.data.local.novelpersist.Novelpersist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说浏览历史仓库：封装对旧 Flutter `Novelpersist.db` 的查询与导入导出。
 *
 * 表结构与旧版 `novelpersist` 保持一致，仅提供查询与批量替换能力，
 * 不新增小说阅读埋点。
 */
class NovelHistoryRepository(
    driver: SqlDriver,
) {
    private val queries = NovelPersistDatabase(driver).novelPersistQueries

    /**
     * 查询全部小说浏览历史，按时间升序排列。
     */
    fun getAll(): List<NovelHistoryItem> {
        return queries.selectAll().executeAsList().map { it.toNovelHistoryItem() }
    }

    /**
     * 导入时批量替换小说浏览历史。
     *
     * 先清空旧记录，再按导入数据重新写入，避免重复记录与主键冲突。
     * 操作在 [Dispatchers.Default] 中执行并在事务内完成。
     */
    suspend fun replaceAll(items: List<NovelHistoryItem>) = withContext(Dispatchers.Default) {
        queries.transaction {
            queries.deleteAll()
            items.forEach { item ->
                queries.insertOrReplace(
                    id = item.id ?: 0L,
                    novel_id = item.novelId.toLong(),
                    user_id = item.userId.toLong(),
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
    private fun Novelpersist.toNovelHistoryItem(): NovelHistoryItem = NovelHistoryItem(
        id = id,
        novelId = novel_id.toInt(),
        userId = user_id.toInt(),
        pictureUrl = picture_url,
        title = title,
        userName = user_name,
        time = time,
    )
}

/**
 * 小说浏览历史对外模型，字段名与旧 Flutter `NovelPersist` 的 JSON 序列化键保持一致。
 */
@Serializable
data class NovelHistoryItem(
    @SerialName("id") val id: Long? = null,
    @SerialName("novel_id") val novelId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("title") val title: String,
    @SerialName("user_name") val userName: String,
    @SerialName("time") val time: Long,
)
