package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.isR18
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 屏蔽仓库：封装对旧 banillustid.db、banuserid.db 与 bantag.db 的读写。
 *
 * 复用旧 Flutter 遗留的表结构：
 * - banillustid：id（自增主键）、illust_id（被屏蔽作品 ID，文本存储）、name（作品标题）
 * - banuserid：id（自增主键）、user_id（被屏蔽画师 ID，文本存储）、name（画师名称）
 * - bantag：id（自增主键）、translate_name（标签翻译名）、name（标签名或正则表达式）
 *
 * 所有公开方法均为 suspend，内部切到 [Dispatchers.Default] 执行，避免阻塞 UI 线程。
 */
class BanRepository(
    banIllustIdDriver: SqlDriver,
    banUserIdDriver: SqlDriver,
    banTagDriver: SqlDriver,
) {
    private val illustQueries = BanIllustIdDatabase(banIllustIdDriver).banIllustIdQueries
    private val userQueries = BanUserIdDatabase(banUserIdDriver).banUserIdQueries
    private val tagQueries = BanTagDatabase(banTagDriver).banTagQueries

    private val cacheMutex = Mutex()
    private var cachedBannedIllustIds: Set<Int>? = null
    private var cachedBannedUserIds: Set<Int>? = null
    private var cachedBanTags: List<BanTag>? = null

    private fun invalidateCache() {
        cachedBannedIllustIds = null
        cachedBannedUserIds = null
        cachedBanTags = null
    }

    // region 屏蔽作品

    /**
     * 查询全部被屏蔽的作品。
     */
    suspend fun getAllBanIllusts(): List<BanIllust> = withContext(Dispatchers.Default) {
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
    suspend fun isBanIllust(illustId: Int): Boolean = withContext(Dispatchers.Default) {
        val cached = cachedBannedIllustIds
        if (cached != null) return@withContext illustId in cached
        illustQueries.selectByIllustId(illustId.toString()).executeAsOneOrNull() != null
    }

    /**
     * 查询全部被屏蔽的作品 ID 集合，用于列表页快速过滤（带内存缓存）。
     */
    suspend fun getBannedIllustIds(): Set<Int> = withContext(Dispatchers.Default) {
        cachedBannedIllustIds ?: cacheMutex.withLock {
            cachedBannedIllustIds ?: illustQueries.selectAll().executeAsList().mapNotNull {
                it.illust_id.toIntOrNull()
            }.toSet().also { cachedBannedIllustIds = it }
        }
    }

    /**
     * 将作品加入屏蔽列表。
     *
     * 若该作品已存在，先删除旧记录再插入，对齐旧 Flutter 的
     * [ConflictAlgorithm.replace] 行为，避免重复记录。
     */
    suspend fun insertBanIllust(illustId: Int, name: String) = withContext(Dispatchers.Default) {
        val existing = illustQueries.selectByIllustId(illustId.toString()).executeAsOneOrNull()
        if (existing != null) {
            illustQueries.delete(existing.id)
        }
        illustQueries.insert(illustId.toString(), name)
        invalidateCache()
    }

    /**
     * 按主键删除屏蔽作品记录。
     */
    suspend fun deleteBanIllust(id: Long) = withContext(Dispatchers.Default) {
        illustQueries.delete(id)
        invalidateCache()
    }

    /**
     * 清空全部屏蔽作品记录。
     */
    suspend fun clearAllBanIllusts() = withContext(Dispatchers.Default) {
        illustQueries.deleteAll()
        invalidateCache()
    }

    /**
     * 批量导入屏蔽作品记录，事务内逐条写入并替换已存在记录。
     */
    suspend fun insertAllBanIllusts(items: List<BanIllust>) = withContext(Dispatchers.Default) {
        illustQueries.transaction {
            items.forEach { item ->
                illustQueries.insert(item.illustId, item.name)
            }
        }
        invalidateCache()
    }

    /**
     * 原子替换全部屏蔽作品记录：在单个事务内先清空再插入。
     * 若插入失败，旧数据不会被清空，避免导入中途丢失数据。
     */
    suspend fun replaceAllBanIllusts(items: List<BanIllust>) = withContext(Dispatchers.Default) {
        illustQueries.transaction {
            illustQueries.deleteAll()
            items.forEach { item ->
                illustQueries.insert(item.illustId, item.name)
            }
        }
        invalidateCache()
    }

    // endregion

    // region 屏蔽画师

    /**
     * 查询全部被屏蔽的画师。
     */
    suspend fun getAllBanUsers(): List<BanUser> = withContext(Dispatchers.Default) {
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
    /**
     * 查询指定画师是否已被屏蔽。
     */
    suspend fun isBanUser(userId: Int): Boolean = withContext(Dispatchers.Default) {
        val cached = cachedBannedUserIds
        if (cached != null) return@withContext userId in cached
        userQueries.selectByUserId(userId.toString()).executeAsOneOrNull() != null
    }

    /**
     * 查询全部被屏蔽的画师 ID 集合，用于列表页快速过滤（带内存缓存）。
     */
    suspend fun getBannedUserIds(): Set<Int> = withContext(Dispatchers.Default) {
        cachedBannedUserIds ?: cacheMutex.withLock {
            cachedBannedUserIds ?: userQueries.selectAll().executeAsList().mapNotNull {
                it.user_id.toIntOrNull()
            }.toSet().also { cachedBannedUserIds = it }
        }
    }

    /**
     * 将画师加入屏蔽列表。
     *
     * 若该画师已存在，先删除旧记录再插入，对齐旧 Flutter 的
     * [ConflictAlgorithm.replace] 行为，避免重复记录。
     */
    suspend fun insertBanUser(userId: Int, name: String) = withContext(Dispatchers.Default) {
        val existing = userQueries.selectByUserId(userId.toString()).executeAsOneOrNull()
        if (existing != null) {
            userQueries.delete(existing.id)
        }
        userQueries.insert(userId.toString(), name)
        invalidateCache()
    }

    /**
     * 按主键删除屏蔽画师记录。
     */
    suspend fun deleteBanUser(id: Long) = withContext(Dispatchers.Default) {
        userQueries.delete(id)
        invalidateCache()
    }

    /**
     * 清空全部屏蔽画师记录。
     */
    suspend fun clearAllBanUsers() = withContext(Dispatchers.Default) {
        userQueries.deleteAll()
        invalidateCache()
    }

    /**
     * 批量导入屏蔽画师记录，事务内逐条写入并替换已存在记录。
     */
    suspend fun insertAllBanUsers(items: List<BanUser>) = withContext(Dispatchers.Default) {
        userQueries.transaction {
            items.forEach { item ->
                userQueries.insert(item.userId, item.name)
            }
        }
        invalidateCache()
    }

    /**
     * 原子替换全部屏蔽画师记录：在单个事务内先清空再插入。
     * 若插入失败，旧数据不会被清空，避免导入中途丢失数据。
     */
    suspend fun replaceAllBanUsers(items: List<BanUser>) = withContext(Dispatchers.Default) {
        userQueries.transaction {
            userQueries.deleteAll()
            items.forEach { item ->
                userQueries.insert(item.userId, item.name)
            }
        }
        invalidateCache()
    }

    // endregion

    // region 屏蔽标签

    /**
     * 查询全部被屏蔽的标签（带内存缓存）。
     */
    suspend fun getAllBanTags(): List<BanTag> = withContext(Dispatchers.Default) {
        cachedBanTags ?: cacheMutex.withLock {
            cachedBanTags ?: tagQueries.selectAll().executeAsList().map {
                BanTag(
                    id = it.id,
                    name = it.name,
                    translateName = it.translate_name,
                )
            }.also { cachedBanTags = it }
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
    suspend fun insertBanTag(name: String, translateName: String) = withContext(Dispatchers.Default) {
        val existing = tagQueries.selectByName(name).executeAsOneOrNull()
        if (existing != null) {
            tagQueries.delete(existing.id)
        }
        tagQueries.insert(translateName, name)
        invalidateCache()
    }

    /**
     * 按主键删除屏蔽标签记录。
     */
    suspend fun deleteBanTag(id: Long) = withContext(Dispatchers.Default) {
        tagQueries.delete(id)
        invalidateCache()
    }

    /**
     * 清空全部屏蔽标签记录。
     */
    suspend fun clearAllBanTags() = withContext(Dispatchers.Default) {
        tagQueries.deleteAll()
        invalidateCache()
    }

    /**
     * 批量导入屏蔽标签记录，事务内逐条写入并替换已存在记录。
     */
    suspend fun insertAllBanTags(items: List<BanTag>) = withContext(Dispatchers.Default) {
        tagQueries.transaction {
            items.forEach { item ->
                tagQueries.insert(item.translateName, item.name)
            }
        }
        invalidateCache()
    }

    /**
     * 原子替换全部屏蔽标签记录：在单个事务内先清空再插入。
     * 若插入失败，旧数据不会被清空，避免导入中途丢失数据。
     */
    suspend fun replaceAllBanTags(items: List<BanTag>) = withContext(Dispatchers.Default) {
        tagQueries.transaction {
            tagQueries.deleteAll()
            items.forEach { item ->
                tagQueries.insert(item.translateName, item.name)
            }
        }
        invalidateCache()
    }

    /**
     * 统一插画作品屏蔽过滤：
     * 结合被屏蔽作品 ID、画师 ID、屏蔽标签及偏好（AI 生成过滤、R-18 过滤）执行一站式高效过滤。
     * 利用内存缓存避免触底翻页与下拉刷新时重复触发多次 SQLite 磁盘读取。
     */
    suspend fun filterIllusts(
        rawIllusts: List<Illust>,
        banAIIllust: Boolean = false,
        hideR18: Boolean = false,
    ): List<Illust> {
        if (rawIllusts.isEmpty()) return emptyList()
        val bannedIds = getBannedIllustIds()
        val bannedUserIds = getBannedUserIds()
        val banTags = getAllBanTags()
        return rawIllusts.filter { illust ->
            illust.id !in bannedIds &&
                illust.user.id !in bannedUserIds &&
                (!banAIIllust || illust.illustAIType != 2) &&
                (!hideR18 || !illust.isR18()) &&
                !isBannedByTags(
                    banTags,
                    illust.tags.flatMap { tag -> listOfNotNull(tag.name, tag.translatedName) }
                )
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
         * 使用 lazy 缓存避免在遍历作品列表时重复编译相同正则表达式。
         * 正则编译失败时返回 null，避免非法正则导致崩溃。
         */
        val regex: Regex? by lazy {
            if (!isRegexMatcher) {
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
        }

        private companion object {
            private const val REGEX_PREFIX = "r'"
            private const val REGEX_SUFFIX = "'"
        }
    }
}
