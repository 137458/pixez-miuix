package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 屏蔽仓库：封装对旧 banillustid.db、banuserid.db 与 bantag.db 的读写。
 *
 * 复用旧 Flutter 遗留的表结构：
 * - banillustid：id（自增主键）、illust_id（被屏蔽作品 ID，文本存储）、name（作品标题）
 * - banuserid：id（自增主键）、user_id（被屏蔽画师 ID，文本存储）、name（画师名称）
 * - bantag：id（自增主键）、translate_name（标签翻译名）、name（标签名或正则表达式）
 *
 * 所有公开方法均为 suspend，内部切到 [Dispatchers.IO] 执行，避免阻塞 UI 线程。
 */
class BanRepository(
    banIllustIdDriver: SqlDriver,
    banUserIdDriver: SqlDriver,
    banTagDriver: SqlDriver,
) {
    private val illustQueries = BanIllustIdDatabase(banIllustIdDriver).banIllustIdQueries
    private val userQueries = BanUserIdDatabase(banUserIdDriver).banUserIdQueries
    private val tagQueries = BanTagDatabase(banTagDriver).banTagQueries

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

    /**
     * 批量导入屏蔽作品记录，事务内逐条写入并替换已存在记录。
     */
    suspend fun insertAllBanIllusts(items: List<BanIllust>) = withContext(Dispatchers.IO) {
        illustQueries.transaction {
            items.forEach { item ->
                illustQueries.insert(item.illustId, item.name)
            }
        }
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

    /**
     * 批量导入屏蔽画师记录，事务内逐条写入并替换已存在记录。
     */
    suspend fun insertAllBanUsers(items: List<BanUser>) = withContext(Dispatchers.IO) {
        userQueries.transaction {
            items.forEach { item ->
                userQueries.insert(item.userId, item.name)
            }
        }
    }

    // endregion

    // region 屏蔽标签

    /**
     * 查询全部被屏蔽的标签。
     */
    suspend fun getAllBanTags(): List<BanTag> = withContext(Dispatchers.IO) {
        tagQueries.selectAll().executeAsList().map {
            BanTag(
                id = it.id,
                name = it.name,
                translateName = it.translate_name,
            )
        }
    }

    /**
     * 判断给定标签列表是否命中任意屏蔽规则。
     *
     * 屏蔽规则分两种：
     * - 普通标签：当 [BanTag.name] 或 [BanTag.translateName] 与输入标签精确匹配时命中。
     * - 正则标签：当 [BanTag.name] 以 `r'` 开头且以 `'` 结尾时，中间内容作为正则表达式
     *   在输入标签中搜索匹配（对应原 Flutter RegExp.hasMatch）；正则编译失败时安全降级为不匹配。
     *
     * 该方法为非挂起函数，用于列表页在一次性查询屏蔽标签后批量过滤。
     */
    fun isBannedByTags(banTags: List<BanTag>, tags: List<String>): Boolean {
        for (banTag in banTags) {
            if (banTag.isRegexMatcher) {
                val regex = banTag.regex ?: continue
                for (tag in tags) {
                    if (regex.containsMatchIn(tag)) return true
                }
            } else {
                for (tag in tags) {
                    if (banTag.name == tag || banTag.translateName == tag) return true
                }
            }
        }
        return false
    }

    /**
     * 将标签加入屏蔽列表。
     *
     * 若该标签已存在，先删除旧记录再插入，对齐旧 Flutter 的
     * [ConflictAlgorithm.replace] 行为，避免重复记录。
     */
    suspend fun insertBanTag(name: String, translateName: String) = withContext(Dispatchers.IO) {
        val existing = tagQueries.selectByName(name).executeAsOneOrNull()
        if (existing != null) {
            tagQueries.delete(existing.id)
        }
        tagQueries.insert(translateName, name)
    }

    /**
     * 按主键删除屏蔽标签记录。
     */
    suspend fun deleteBanTag(id: Long) = withContext(Dispatchers.IO) {
        tagQueries.delete(id)
    }

    /**
     * 清空全部屏蔽标签记录。
     */
    suspend fun clearAllBanTags() = withContext(Dispatchers.IO) {
        tagQueries.deleteAll()
    }

    /**
     * 批量导入屏蔽标签记录，事务内逐条写入并替换已存在记录。
     */
    suspend fun insertAllBanTags(items: List<BanTag>) = withContext(Dispatchers.IO) {
        tagQueries.transaction {
            items.forEach { item ->
                tagQueries.insert(item.translateName, item.name)
            }
        }
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

    /**
     * 屏蔽标签对外模型。
     */
    data class BanTag(
        val id: Long,
        val name: String,
        val translateName: String,
    ) {
        /**
         * 当前标签是否作为正则表达式匹配器使用。
         */
        val isRegexMatcher: Boolean
            get() = name.startsWith(REGEX_PREFIX) && name.endsWith(REGEX_SUFFIX)

        /**
         * 当 [isRegexMatcher] 为 true 时返回编译后的正则，否则返回 null。
         * 正则编译失败时返回 null，避免非法正则导致崩溃。
         */
        val regex: Regex?
            get() = if (!isRegexMatcher) {
                null
            } else {
                runCatching {
                    Regex(
                        name.substring(
                            REGEX_PREFIX.length,
                            name.length - REGEX_SUFFIX.length,
                        )
                    )
                }.getOrNull()
            }

        private companion object {
            private const val REGEX_PREFIX = "r'"
            private const val REGEX_SUFFIX = "'"
        }
    }
}
