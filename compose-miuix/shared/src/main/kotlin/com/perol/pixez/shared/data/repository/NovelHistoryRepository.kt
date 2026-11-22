package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import com.perol.pixez.shared.data.local.novelpersist.Novelpersist
import com.perol.pixez.shared.data.model.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说浏览历史仓库：封装对旧 Flutter `Novelpersist.db` 的记录、查询、清理与导入导出。
 *
 * 表结构与旧版 `novelpersist` 保持一致；阅读器打开小说详情后写入浏览历史
 * （同小说去重并前置），历史页按时间降序展示，导入导出沿用旧版语义。
 */
class NovelHistoryRepository(
    driver: SqlDriver,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val queries = NovelPersistDatabase(driver).novelPersistQueries

    /**
     * 写入一条小说浏览历史。
     * 若该小说已存在历史记录，则更新其时间并前置到最新位置。
     */
    suspend fun insert(novel: Novel) = withContext(Dispatchers.IO) {
        val now = nowMillis()
        queries.transaction {
            queries.deleteByNovelId(novel.id)
            queries.insertHistory(
                novel_id = novel.id,
                user_id = novel.user.id,
                picture_url = novel.imageUrls.squareMedium,
                title = novel.title,
                user_name = novel.user.name,
                time = now,
            )
        }
    }

    /**
     * 查询小说浏览历史，按时间降序排列（最新浏览排在最前）。
     *
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     *
     * @param limit 最大返回条数，防止一次性加载过多历史记录导致内存/卡顿问题。
     */
    suspend fun getAllDesc(limit: Long = HISTORY_QUERY_LIMIT): List<NovelHistoryItem> = withContext(Dispatchers.IO) {
        queries.selectAllDesc(limit).executeAsList().map { it.toNovelHistoryItem() }
    }

    /**
     * 删除指定主键的历史记录。
     *
     * 使用自增主键 [id] 而非 [Novel.id]，避免同小说多次浏览时误删多条记录。
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     */
    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        queries.deleteById(id)
    }

    /**
     * 清空全部浏览历史。
     *
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }

    /**
     * 查询小说浏览历史（导出用），按时间升序排列。
     *
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     *
     * @param limit 最大返回条数，与其余历史表的查询上限同源。
     */
    suspend fun getAll(limit: Long = HISTORY_QUERY_LIMIT): List<NovelHistoryItem> = withContext(Dispatchers.IO) {
        queries.selectAll(limit).executeAsList().map { it.toNovelHistoryItem() }
    }

    /**
     * 导入时批量替换小说浏览历史。
     *
     * 先清空旧记录，再按导入数据重新写入，避免重复记录与主键冲突。
     * 导入项带有效 id 时原样保留；不带 id 时由数据库自增生成，不会彼此折叠。
     * 操作在 [Dispatchers.IO] 中执行并在事务内完成。
     */
    suspend fun replaceAll(items: List<NovelHistoryItem>) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.deleteAll()
            items.forEach { item ->
                val importedId = item.id
                if (importedId == null || importedId <= 0L) {
                    // 无 id 的导入交给 AUTOINCREMENT 生成主键：写成 id = 0 会让多条无 id 记录
                    // 在 INSERT OR REPLACE 下互相覆盖，最终只剩最后一条。
                    queries.insertHistory(
                        novel_id = item.novelId,
                        user_id = item.userId,
                        picture_url = item.pictureUrl,
                        title = item.title,
                        user_name = item.userName,
                        time = item.time,
                    )
                } else {
                    queries.insertOrReplace(
                        id = importedId,
                        novel_id = item.novelId,
                        user_id = item.userId,
                        picture_url = item.pictureUrl,
                        title = item.title,
                        user_name = item.userName,
                        time = item.time,
                    )
                }
            }
        }
    }

    /**
     * 保存某本小说的阅读进度（条目索引 + 条目内偏移）。
     *
     * 同一小说只保留一条记录，重复保存覆盖旧位置，因此阅读器可以按需高频调用。
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     */
    suspend fun saveReadingProgress(novelId: Long, blockIndex: Int, blockOffset: Int) = withContext(Dispatchers.IO) {
        queries.insertReadingProgress(
            novel_id = novelId,
            block_index = blockIndex.toLong(),
            block_offset = blockOffset.toLong(),
            updated_at = nowMillis(),
        )
    }

    /**
     * 读取某本小说的阅读进度，没有记录时返回 null。
     *
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     */
    suspend fun getReadingProgress(novelId: Long): NovelReadingProgress? = withContext(Dispatchers.IO) {
        queries.selectReadingProgress(novelId).executeAsOneOrNull()?.let { row ->
            NovelReadingProgress(
                novelId = row.novel_id,
                blockIndex = row.block_index.toInt(),
                blockOffset = row.block_offset.toInt(),
                updatedAt = row.updated_at,
            )
        }
    }

    /**
     * 清除某本小说的阅读进度。
     *
     * 数据库操作在 [Dispatchers.IO] 中执行，调用方无需自行切换调度器。
     */
    suspend fun clearReadingProgress(novelId: Long) = withContext(Dispatchers.IO) {
        queries.deleteReadingProgress(novelId)
    }

    /**
     * 将数据库行转换为对外模型。
     */
    private fun Novelpersist.toNovelHistoryItem(): NovelHistoryItem = NovelHistoryItem(
        id = id,
        novelId = novel_id,
        userId = user_id,
        pictureUrl = picture_url,
        title = title,
        userName = user_name,
        time = time,
    )
}

/**
 * 小说浏览历史对外模型，字段名与旧 Flutter `NovelPersist` 的 JSON 序列化键保持一致。
 */
/**
 * 小说阅读进度模型：记录阅读器最后停留的条目索引与条目内滚动偏移。
 *
 * 值为 null 表示该小说没有已保存的进度，阅读器从顶部开始展示。
 */
data class NovelReadingProgress(
    val novelId: Long,
    val blockIndex: Int,
    val blockOffset: Int,
    val updatedAt: Long,
)

@Serializable
data class NovelHistoryItem(
    @SerialName("id") val id: Long? = null,
    @SerialName("novel_id") val novelId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("picture_url") val pictureUrl: String,
    @SerialName("title") val title: String,
    @SerialName("user_name") val userName: String,
    @SerialName("time") val time: Long,
)
