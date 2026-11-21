package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustTag
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.settings.SettingsRepository
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
 * 三张表的读写流程完全同构，差异只在 SQL 调用与业务主键取法：由内置的 BanTable 描述这些差异后，
 * 事务、互斥锁与缓存失效统一收敛为一份泛型实现，公开方法只保留类型映射。
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

    private val cacheMutex = Mutex()

    // 双检锁快路径无锁读，须 @Volatile 保证写侧（Mutex 内）对读侧的可见性（写法同 AuthTokenStorage）。
    @kotlin.concurrent.Volatile
    private var cachedBannedIllustIds: Set<Long>? = null
    @kotlin.concurrent.Volatile
    private var cachedBannedUserIds: Set<Long>? = null
    @kotlin.concurrent.Volatile
    private var cachedBanTags: List<BanTag>? = null

    /**
     * 单张屏蔽表的读写原语。
     *
     * 三张表的 SQL 调用与业务主键取法不同，故以 lambda 注入；事务、互斥锁与缓存失效流程
     * 由下面的泛型方法统一实现。
     *
     * @param selectAllRows 全表查询，同时完成行类型到对外模型的映射
     * @param findRowId 按业务主键（illust_id / user_id / name）查自增主键，未命中返回 null
     * @param insertRow 插入一行；模型里的 id 不参与写入，自增主键由数据库生成
     * @param deleteById 按自增主键删除
     * @param deleteAllRows 清空整表
     * @param runInTransaction 把多条语句包进同一事务，异常时整体回滚
     */
    private class BanTable<T>(
        val selectAllRows: () -> List<T>,
        val findRowId: (key: String) -> Long?,
        val insertRow: (T) -> Unit,
        val deleteById: (Long) -> Unit,
        val deleteAllRows: () -> Unit,
        val runInTransaction: (body: () -> Unit) -> Unit,
    )

    private val illustTable = BanTable(
        selectAllRows = {
            illustQueries.selectAll().executeAsList().map {
                BanIllust(id = it.id, illustId = it.illust_id, name = it.name)
            }
        },
        findRowId = { key -> illustQueries.selectByIllustId(key).executeAsOneOrNull()?.id },
        insertRow = { illust -> illustQueries.insert(illust.illustId, illust.name) },
        deleteById = { id -> illustQueries.delete(id) },
        deleteAllRows = { illustQueries.deleteAll() },
        runInTransaction = { body -> illustQueries.transaction { body() } },
    )

    private val userTable = BanTable(
        selectAllRows = {
            userQueries.selectAll().executeAsList().map {
                BanUser(id = it.id, userId = it.user_id, name = it.name)
            }
        },
        findRowId = { key -> userQueries.selectByUserId(key).executeAsOneOrNull()?.id },
        insertRow = { user -> userQueries.insert(user.userId, user.name) },
        deleteById = { id -> userQueries.delete(id) },
        deleteAllRows = { userQueries.deleteAll() },
        runInTransaction = { body -> userQueries.transaction { body() } },
    )

    private val tagTable = BanTable(
        selectAllRows = {
            tagQueries.selectAll().executeAsList().map {
                BanTag(id = it.id, name = it.name, translateName = it.translate_name)
            }
        },
        findRowId = { key -> tagQueries.selectByName(key).executeAsOneOrNull()?.id },
        insertRow = { tag -> tagQueries.insert(tag.translateName, tag.name) },
        deleteById = { id -> tagQueries.delete(id) },
        deleteAllRows = { tagQueries.deleteAll() },
        runInTransaction = { body -> tagQueries.transaction { body() } },
    )

    private fun invalidateCacheLocked() {
        cachedBannedIllustIds = null
        cachedBannedUserIds = null
        cachedBanTags = null
    }

    private fun <T> deduplicateByKey(items: List<T>, keyOf: (T) -> String): List<T> {
        if (items.size <= 1) return items
        return linkedMapOf<String, T>().apply {
            items.forEach { put(keyOf(it), it) }
        }.values.toList()
    }

    /**
     * 带内存缓存的读取：命中直接返回；未命中则在互斥锁内重新读一次缓存后加载并回填。
     * 锁内二次读缓存，避免并发写入方失效缓存后重复全表扫描。
     */
    private suspend fun <T : Any> readCached(
        readCache: () -> T?,
        writeCache: (T) -> Unit,
        load: () -> T,
    ): T = withContext(Dispatchers.IO) {
        readCache() ?: cacheMutex.withLock {
            readCache() ?: load().also(writeCache)
        }
    }

    /** 无缓存读取整表。 */
    private suspend fun <T> readAll(table: BanTable<T>): List<T> = withContext(Dispatchers.IO) {
        table.selectAllRows()
    }

    /** 数值主键表的「是否已屏蔽」查询：ID 集合已缓存时走内存判断，否则按业务主键精确查一行。 */
    private suspend fun <T> isBanned(
        table: BanTable<T>,
        cachedIds: Set<Long>?,
        id: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        val cached = cachedIds
        if (cached != null) return@withContext id in cached
        table.findRowId(id.toString()) != null
    }

    /**
     * 插入一行并对齐旧 Flutter 的 [ConflictAlgorithm.replace] 行为：同业务主键的旧记录先删除。
     *
     * 查删插包在同库事务与互斥锁内，避免并发读回填旧缓存。
     */
    private suspend fun <T> insertRow(table: BanTable<T>, item: T, key: String) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            table.runInTransaction {
                table.findRowId(key)?.let { table.deleteById(it) }
                table.insertRow(item)
            }
            invalidateCacheLocked()
        }
    }

    /** 按自增主键删除一行。 */
    private suspend fun <T> deleteRow(table: BanTable<T>, id: Long) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            table.deleteById(id)
            invalidateCacheLocked()
        }
    }

    /** 清空整表。 */
    private suspend fun <T> clearAllRows(table: BanTable<T>) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            table.deleteAllRows()
            invalidateCacheLocked()
        }
    }

    /** 批量导入：先按业务主键去重批次内记录，再在同一事务里逐条替换库中同名记录。 */
    private suspend fun <T> insertAllRows(table: BanTable<T>, items: List<T>, keyOf: (T) -> String) =
        withContext(Dispatchers.IO) {
            val uniqueItems = deduplicateByKey(items, keyOf)
            cacheMutex.withLock {
                table.runInTransaction {
                    uniqueItems.forEach { item ->
                        table.findRowId(keyOf(item))?.let { table.deleteById(it) }
                        table.insertRow(item)
                    }
                }
                invalidateCacheLocked()
            }
        }

    /**
     * 原子替换整表：在单个事务内先清空再插入。
     * 若插入失败，旧数据不会被清空，避免导入中途丢失数据。
     */
    private suspend fun <T> replaceAllRows(table: BanTable<T>, items: List<T>, keyOf: (T) -> String) =
        withContext(Dispatchers.IO) {
            val uniqueItems = deduplicateByKey(items, keyOf)
            cacheMutex.withLock {
                table.runInTransaction {
                    table.deleteAllRows()
                    uniqueItems.forEach { table.insertRow(it) }
                }
                invalidateCacheLocked()
            }
        }

    // region 屏蔽作品

    /** 查询全部被屏蔽的作品。 */
    suspend fun getAllBanIllusts(): List<BanIllust> = readAll(illustTable)

    /** 查询指定作品是否已被屏蔽。 */
    suspend fun isBanIllust(illustId: Long): Boolean =
        isBanned(illustTable, cachedBannedIllustIds, illustId)

    /** 查询全部被屏蔽的作品 ID 集合，用于列表页快速过滤（带内存缓存）。 */
    suspend fun getBannedIllustIds(): Set<Long> = readCached(
        readCache = { cachedBannedIllustIds },
        writeCache = { cachedBannedIllustIds = it },
    ) {
        illustTable.selectAllRows().mapNotNull { it.illustId.toLongOrNull() }.toSet()
    }

    /** 将作品加入屏蔽列表；已存在同 ID 记录时先删除再插入，避免重复记录。 */
    suspend fun insertBanIllust(illustId: Long, name: String) = insertRow(
        table = illustTable,
        item = BanIllust(id = 0L, illustId = illustId.toString(), name = name),
        key = illustId.toString(),
    )

    /** 按主键删除屏蔽作品记录。 */
    suspend fun deleteBanIllust(id: Long) = deleteRow(illustTable, id)

    /** 清空全部屏蔽作品记录。 */
    suspend fun clearAllBanIllusts() = clearAllRows(illustTable)

    /** 批量导入屏蔽作品记录，去重批次及库中既有同 ID 记录后事务写入。 */
    suspend fun insertAllBanIllusts(items: List<BanIllust>) =
        insertAllRows(illustTable, items) { it.illustId }

    /** 原子替换全部屏蔽作品记录：在单个事务内先清空再插入。 */
    suspend fun replaceAllBanIllusts(items: List<BanIllust>) =
        replaceAllRows(illustTable, items) { it.illustId }

    // endregion

    // region 屏蔽画师

    /** 查询全部被屏蔽的画师。 */
    suspend fun getAllBanUsers(): List<BanUser> = readAll(userTable)

    /** 查询指定画师是否已被屏蔽。 */
    suspend fun isBanUser(userId: Long): Boolean = isBanned(userTable, cachedBannedUserIds, userId)

    /** 查询全部被屏蔽的画师 ID 集合，用于列表页快速过滤（带内存缓存）。 */
    suspend fun getBannedUserIds(): Set<Long> = readCached(
        readCache = { cachedBannedUserIds },
        writeCache = { cachedBannedUserIds = it },
    ) {
        userTable.selectAllRows().mapNotNull { it.userId.toLongOrNull() }.toSet()
    }

    /** 将画师加入屏蔽列表；已存在同 ID 记录时先删除再插入，避免重复记录。 */
    suspend fun insertBanUser(userId: Long, name: String) = insertRow(
        table = userTable,
        item = BanUser(id = 0L, userId = userId.toString(), name = name),
        key = userId.toString(),
    )

    /** 按主键删除屏蔽画师记录。 */
    suspend fun deleteBanUser(id: Long) = deleteRow(userTable, id)

    /** 批量导入屏蔽画师记录，去重批次及库中既有同 ID 记录后事务写入。 */
    suspend fun insertAllBanUsers(items: List<BanUser>) =
        insertAllRows(userTable, items) { it.userId }

    /** 原子替换全部屏蔽画师记录：在单个事务内先清空再插入。 */
    suspend fun replaceAllBanUsers(items: List<BanUser>) =
        replaceAllRows(userTable, items) { it.userId }

    // endregion

    // region 屏蔽标签

    /** 查询全部被屏蔽的标签（带内存缓存）。 */
    suspend fun getAllBanTags(): List<BanTag> = readCached(
        readCache = { cachedBanTags },
        writeCache = { cachedBanTags = it },
    ) {
        tagTable.selectAllRows()
    }

    /**
     * 按屏蔽规则过滤条目列表，保持输入顺序。
     *
     * 搜索结果、热门标签等模型通常包含原标签名与翻译名等多个关联名，
     * 通过 [tagsOf] 取出待匹配的所有候选标签名后交给统一规则表判定，
     * 确保原生名与翻译名任一命中屏蔽词时均能正确过滤。
     */
    fun <T> filterBannedTags(banTags: List<BanTag>, items: List<T>, tagsOf: (T) -> List<String>): List<T> {
        if (banTags.isEmpty() || items.isEmpty()) return items
        val rules = compileBanTagRules(banTags)
        if (rules.isEmpty) return items
        return items.filterNot { rules.matchesAny(tagsOf(it)) }
    }

    /** 将标签加入屏蔽列表；已存在同名记录时先删除再插入，避免重复记录。 */
    suspend fun insertBanTag(name: String, translateName: String) = insertRow(
        table = tagTable,
        item = BanTag(id = 0L, name = name, translateName = translateName),
        key = name,
    )

    /** 按主键删除屏蔽标签记录。 */
    suspend fun deleteBanTag(id: Long) = deleteRow(tagTable, id)

    /** 批量导入屏蔽标签记录，去重批次及库中既有同名记录后事务写入。 */
    suspend fun insertAllBanTags(items: List<BanTag>) =
        insertAllRows(tagTable, items) { it.name }

    /** 原子替换全部屏蔽标签记录：在单个事务内先清空再插入。 */
    suspend fun replaceAllBanTags(items: List<BanTag>) =
        replaceAllRows(tagTable, items) { it.name }

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
        // 屏蔽规则先编译成名称集合与正则表，作品侧直接遍历 tags 数组，避免逐作品
        // 构建候选标签列表并做 banTags×tags 双层遍历。
        val banTagRules = compileBanTagRules(getAllBanTags())
        return rawIllusts.filter { illust ->
            illust.id !in bannedIds &&
                illust.user.id !in bannedUserIds &&
                (!banAIIllust || illust.illustAIType != 2) &&
                (!hideR18 || !illust.isR18()) &&
                !banTagRules.matchesIllustTags(illust.tags)
        }
    }

    /**
     * 把屏蔽标签编译成规则表：普通标签收敛为名称集合，正则标签收敛为已编译正则。
     *
     * 正则标签编译失败时不做任何匹配（原实现即直接跳过），不会回退成按名称精确比较。
     */
    private fun compileBanTagRules(banTags: List<BanTag>): BanTagRules {
        val exactNames = HashSet<String>(banTags.size)
        val regexes = ArrayList<Regex>(banTags.size)
        for (banTag in banTags) {
            val regex = banTag.regex
            when {
                regex != null -> regexes.add(regex)
                banTag.isRegexMatcher -> Unit
                else -> {
                    exactNames.add(banTag.name)
                    exactNames.add(banTag.translateName)
                }
            }
        }
        return BanTagRules(exactNames, regexes)
    }

    // endregion

    /**
     * 编译后的屏蔽标签规则表。
     *
     * 匹配语义与逐条遍历 [BanTag] 等价：
     * - 普通标签按 [BanTag.name] 与 [BanTag.translateName] 精确比较（空串同样入集合，保持原判等行为）；
     * - 正则标签用 [Regex.containsMatchIn] 在候选标签中搜索。
     */
    private class BanTagRules(
        private val exactNames: Set<String>,
        private val regexes: List<Regex>,
    ) {
        /** 规则表为空时任何输入都不命中。 */
        val isEmpty: Boolean get() = exactNames.isEmpty() && regexes.isEmpty()

        /** 判断候选标签名列表是否命中任一规则。 */
        fun matchesAny(tags: List<String>): Boolean {
            for (tag in tags) {
                if (matchesTag(tag)) return true
            }
            return false
        }

        /** 判断作品标签是否命中任一规则：原生名与翻译名任一命中即算命中。 */
        fun matchesIllustTags(tags: List<IllustTag>): Boolean {
            for (tag in tags) {
                if (matchesTag(tag.name)) return true
                val translated = tag.translatedName
                if (translated != null && matchesTag(translated)) return true
            }
            return false
        }

        private fun matchesTag(tag: String): Boolean {
            if (tag in exactNames) return true
            for (regex in regexes) {
                if (regex.containsMatchIn(tag)) return true
            }
            return false
        }
    }

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

/**
 * 按用户屏蔽设置统一过滤插画：读取 [SettingsRepository] 的 AI 过滤与 R-18 隐藏开关后转调 [BanRepository.filterIllusts]。
 * 每次调用实时读取设置项，与列表页随设置变更即时重过滤的口径保持一致。
 */
suspend fun BanRepository.filterIllustsBySettings(
    rawIllusts: List<Illust>,
    settings: SettingsRepository,
): List<Illust> = filterIllusts(
    rawIllusts = rawIllusts,
    banAIIllust = settings.banAIIllust,
    hideR18 = settings.hIsNotAllow,
)