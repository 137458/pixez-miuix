package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 屏蔽作品仓库：封装对旧 banillustid.db 的读写。
 *
 * 复用旧 Flutter 遗留的 `banillustid` 表结构，字段为：
 * - id：自增主键
 * - illust_id：被屏蔽的作品 ID（文本存储，与旧版一致）
 * - name：作品标题，用于占位页展示
 *
 * 所有公开方法均为 suspend，内部切到 [Dispatchers.IO] 执行，避免阻塞 UI 线程。
 */
class BanRepository(
    driver: SqlDriver,
) {
    private val queries = BanIllustIdDatabase(driver).banIllustIdQueries

    /**
     * 查询全部被屏蔽的作品。
     */
    suspend fun getAllBanIllusts(): List<BanIllust> = withContext(Dispatchers.IO) {
        queries.selectAll().executeAsList().map {
            BanIllust(
                id = it.id,
                illustId = it.illust_id,
                name = it.name,
            )
        }
    }

    /**
     * 查询指定作品是否已被屏蔽。
     */
    suspend fun isBanIllust(illustId: Int): Boolean = withContext(Dispatchers.IO) {
        queries.selectByIllustId(illustId.toString()).executeAsOneOrNull() != null
    }

    /**
     * 查询全部被屏蔽的作品 ID 集合，用于列表页快速过滤。
     */
    suspend fun getBannedIllustIds(): Set<Int> = withContext(Dispatchers.IO) {
        queries.selectAll().executeAsList().mapNotNull {
            it.illust_id.toIntOrNull()
        }.toSet()
    }

    /**
     * 将作品加入屏蔽列表。
     *
     * 若该作品已存在，先删除旧记录再插入，对齐旧 Flutter 的
     * [ConflictAlgorithm.replace] 行为，避免重复记录。
     */
    suspend fun insertBanIllust(illustId: Int, name: String) = withContext(Dispatchers.IO) {
        val existing = queries.selectByIllustId(illustId.toString()).executeAsOneOrNull()
        if (existing != null) {
            queries.delete(existing.id)
        }
        queries.insert(illustId.toString(), name)
    }

    /**
     * 按主键删除屏蔽记录。
     */
    suspend fun deleteBanIllust(id: Long) = withContext(Dispatchers.IO) {
        queries.delete(id)
    }

    /**
     * 清空全部屏蔽记录。
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }

    /**
     * 屏蔽作品对外模型。
     */
    data class BanIllust(
        val id: Long,
        val illustId: String,
        val name: String,
    )
}
