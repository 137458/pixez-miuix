package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 屏蔽仓库：封装对旧 banillustid.db 与 banuserid.db 的读写。
 *
 * 复用旧 Flutter 遗留的表结构：
 * - banillustid：id（自增主键）、illust_id（被屏蔽作品 ID，文本存储）、name（作品标题）
 * - banuserid：id（自增主键）、user_id（被屏蔽画师 ID，文本存储）、name（画师名称）
 *
 * 所有公开方法均为 suspend，内部切到 [Dispatchers.IO] 执行，避免阻塞 UI 线程。
 */
class BanRepository(
    banIllustIdDriver: SqlDriver,
    banUserIdDriver: SqlDriver,
) {
    private val illustQueries = BanIllustIdDatabase(banIllustIdDriver).banIllustIdQueries
    private val userQueries = BanUserIdDatabase(banUserIdDriver).banUserIdQueries

    // region 屏蔽作品

    /**
     * 查询全部被屏蔽的作品。
     */
    suspend fun getAllBanIllusts(): List<BanIllust> = withContext(Dispatchers.IO) {
        illustQueries.selectAll().executeAsList().map {
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
        illustQueries.selectByIllustId(illustId.toString()).executeAsOneOrNull() != null
    }

    /**
     * 查询全部被屏蔽的作品 ID 集合，用于列表页快速过滤。
     */
    suspend fun getBannedIllustIds(): Set<Int> = withContext(Dispatchers.IO) {
        illustQueries.selectAll().executeAsList().mapNotNull {
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
        val existing = illustQueries.selectByIllustId(illustId.toString()).executeAsOneOrNull()
        if (existing != null) {
            illustQueries.delete(existing.id)
        }
        illustQueries.insert(illustId.toString(), name)
    }

    /**
     * 按主键删除屏蔽作品记录。
     */
    suspend fun deleteBanIllust(id: Long) = withContext(Dispatchers.IO) {
        illustQueries.delete(id)
    }

    /**
     * 清空全部屏蔽作品记录。
     */
    suspend fun clearAllBanIllusts() = withContext(Dispatchers.IO) {
        illustQueries.deleteAll()
    }

    // endregion

    // region 屏蔽画师

    /**
     * 查询全部被屏蔽的画师。
     */
    suspend fun getAllBanUsers(): List<BanUser> = withContext(Dispatchers.IO) {
        userQueries.selectAll().executeAsList().map {
            BanUser(
                id = it.id,
                userId = it.user_id,
                name = it.name,
            )
        }
    }

    /**
     * 查询指定画师是否已被屏蔽。
     */
    suspend fun isBanUser(userId: Int): Boolean = withContext(Dispatchers.IO) {
        userQueries.selectByUserId(userId.toString()).executeAsOneOrNull() != null
    }

    /**
     * 查询全部被屏蔽的画师 ID 集合，用于列表页快速过滤。
     */
    suspend fun getBannedUserIds(): Set<Int> = withContext(Dispatchers.IO) {
        userQueries.selectAll().executeAsList().mapNotNull {
            it.user_id.toIntOrNull()
        }.toSet()
    }

    /**
     * 将画师加入屏蔽列表。
     *
     * 若该画师已存在，先删除旧记录再插入，对齐旧 Flutter 的
     * [ConflictAlgorithm.replace] 行为，避免重复记录。
     */
    suspend fun insertBanUser(userId: Int, name: String) = withContext(Dispatchers.IO) {
        val existing = userQueries.selectByUserId(userId.toString()).executeAsOneOrNull()
        if (existing != null) {
            userQueries.delete(existing.id)
        }
        userQueries.insert(userId.toString(), name)
    }

    /**
     * 按主键删除屏蔽画师记录。
     */
    suspend fun deleteBanUser(id: Long) = withContext(Dispatchers.IO) {
        userQueries.delete(id)
    }

    /**
     * 清空全部屏蔽画师记录。
     */
    suspend fun clearAllBanUsers() = withContext(Dispatchers.IO) {
        userQueries.deleteAll()
    }

    // endregion

    /**
     * 屏蔽作品对外模型。
     */
    data class BanIllust(
        val id: Long,
        val illustId: String,
        val name: String,
    )

    /**
     * 屏蔽画师对外模型。
     */
    data class BanUser(
        val id: Long,
        val userId: String,
        val name: String,
    )
}
