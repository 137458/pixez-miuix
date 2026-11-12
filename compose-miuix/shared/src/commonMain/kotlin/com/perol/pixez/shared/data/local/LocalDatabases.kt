package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import com.perol.pixez.shared.data.local.glanceillustpersist.GlanceIllustPersistDatabase
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.kvpair.KVPairDatabase
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import com.perol.pixez.shared.data.local.task.TaskDatabase

/**
 * 本地 SQLite 库登记项：一个库的全部事实收敛到一行。
 *
 * @property fileName 新建库时使用的权威文件名（沿用旧 Flutter 应用的文件名以保证读到老数据）。
 * @property schema SQLDelight 生成的库结构。
 * @property legacyFileNames 该库历史上出现过的其他文件名：过渡版本或旧平台特例写出的文件，
 *   打开时按「已存在优先」探测，仅作读取兼容，不再作为新建文件名。
 * @property legacyRequiredColumns 旧 v1 结构缺少的表 → 列清单（均为可空列），
 *   打开旧库时按 PRAGMA table_info 补 ALTER TABLE。
 */
class LocalDatabase internal constructor(
    val fileName: String,
    val schema: SqlSchema<QueryResult.Value<Unit>>,
    val legacyFileNames: List<String> = emptyList(),
    val legacyRequiredColumns: Map<String, List<String>> = emptyMap(),
    val legacyRequiredIndexSql: List<String> = emptyList(),
) {
    /** 该库在用户设备上可能存在的全部文件名：权威主名优先，历史别名次之。 */
    val candidateFileNames: List<String> = listOf(fileName) + legacyFileNames
}

/**
 * 本地数据库目录：库名、schema 与旧库补列清单的唯一权威。
 *
 * DriverFactory（解析实际文件名与执行补列）、持库仓库（AppDependencies / WidgetRepository）
 * 一律经此取用，禁止再写裸文件名字面量——历史上小组件库因此出现「实开名与迁移清单名不一致」，
 * 导致旧库补列永不生效且读不到旧数据。
 */
object LocalDatabases {

    /** 账号库（Token 与已登录账号）。 */
    val account = LocalDatabase(
        fileName = "account.db",
        schema = AccountDatabase.Schema,
        legacyRequiredIndexSql = listOf(
            "CREATE INDEX IF NOT EXISTS idx_account_user_id ON account(user_id)",
        ),
    )

    /** 下载任务库。 */
    val task = LocalDatabase(
        fileName = "task.db",
        schema = TaskDatabase.Schema,
        legacyRequiredColumns = mapOf("task" to listOf("medium")),
        legacyRequiredIndexSql = listOf(
            "CREATE INDEX IF NOT EXISTS idx_task_status_id ON task(status, id)",
            "CREATE INDEX IF NOT EXISTS idx_task_url ON task(url)",
        ),
    )

    /** 屏蔽作品库。 */
    val banIllustId = LocalDatabase(
        fileName = "banillustid.db",
        schema = BanIllustIdDatabase.Schema,
    )

    /** 屏蔽画师库。 */
    val banUserId = LocalDatabase(
        fileName = "banuserid.db",
        schema = BanUserIdDatabase.Schema,
    )

    /** 屏蔽标签库。 */
    val banTag = LocalDatabase(
        fileName = "bantag.db",
        schema = BanTagDatabase.Schema,
    )

    /** 插画浏览历史库。 */
    val illustPersist = LocalDatabase(
        fileName = "illustpersist.db",
        schema = IllustPersistDatabase.Schema,
        legacyRequiredColumns = mapOf("illustpersist" to listOf("title", "user_name")),
        legacyRequiredIndexSql = listOf(
            "CREATE INDEX IF NOT EXISTS idx_illustpersist_illust_id ON illustpersist(illust_id)",
            "CREATE INDEX IF NOT EXISTS idx_illustpersist_time ON illustpersist(time)",
        ),
    )

    /** 小说浏览历史库（旧 Flutter 文件名首字母大写，保持原样以读到老数据）。 */
    val novelPersist = LocalDatabase(
        fileName = "Novelpersist.db",
        schema = NovelPersistDatabase.Schema,
    )

    /** 小组件推荐缓存库。 */
    val glanceIllustPersist = LocalDatabase(
        fileName = "glanceillustpersist.db",
        schema = GlanceIllustPersistDatabase.Schema,
        // 过渡版本的小组件曾自造下划线命名，该文件里的缓存同样属于用户数据，需继续可读。
        legacyFileNames = listOf("glance_illust_persist.db"),
        legacyRequiredColumns = mapOf("glanceillustpersist" to listOf("original_url", "large_url")),
        legacyRequiredIndexSql = listOf(
            "CREATE INDEX IF NOT EXISTS idx_glance_ctype_ctime ON glanceillustpersist(ctype, ctime DESC)",
            "CREATE INDEX IF NOT EXISTS idx_glance_illust_id ON glanceillustpersist(illust_id)",
        ),
    )

    /** 键值缓存库（旧 Flutter 遗留，暂无生产读取方）。 */
    val kvPair = LocalDatabase(
        fileName = "kvpair.db",
        schema = KVPairDatabase.Schema,
        legacyRequiredIndexSql = listOf(
            "CREATE INDEX IF NOT EXISTS idx_kvpair_key ON kvpair(key)",
        ),
    )

    /** 全部登记库，按主文件名索引。 */
    val all: List<LocalDatabase> = listOf(
        account,
        task,
        banIllustId,
        banUserId,
        banTag,
        illustPersist,
        novelPersist,
        glanceIllustPersist,
        kvPair,
    )

    private val byAnyFileName: Map<String, LocalDatabase> =
        all.flatMap { database -> database.candidateFileNames.map { it to database } }.toMap()

    /**
     * 按文件名（主名或历史别名）定位登记项；未登记的文件名返回 null。
     *
     * 补列迁移按「实际打开的文件名」查询此表，故别名与主名必须解析到同一登记项。
     */
    fun byFileName(fileName: String): LocalDatabase? = byAnyFileName[fileName]
}

/**
 * 按登记项打开库：先解析该设备上实际存在的库文件名（兼容历史别名），再交给平台驱动。
 */
fun DriverFactory.createDriver(database: LocalDatabase) =
    createDriver(database.schema, resolveDatabaseFileName(database))
