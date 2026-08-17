package com.perol.pixez.shared

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.local.illustpersist.IllustPersistDatabase
import com.perol.pixez.shared.data.local.novelpersist.NovelPersistDatabase
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BoardRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.MuteRepository
import com.perol.pixez.shared.data.repository.NovelHistoryRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.PixivHttpClient
import com.perol.pixez.shared.platform.IllustSaver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.perol.pixez.shared.ui.screens.createUpdateCheckClient
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

/**
 * 应用级依赖容器。
 *
 * 在平台入口（Android Activity / Desktop main）中创建，并通过 [App] 传递到 UI 层。
 * 负责集中管理数据库驱动、网络客户端、Repository 等生命周期较长的对象。
 */
class AppDependencies(
    val driverFactory: DriverFactory,
    val settingsFactory: SettingsFactory,
) {
    init {
        runCatching { Napier.base(DebugAntilog()) }
    }
    /**
     * 账号数据库驱动，复用旧 Flutter account.db。
     */
    val accountDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            AccountDatabase.Schema,
            "account.db",
        )
    }

    /**
     * 下载任务数据库驱动，复用旧 Flutter task.db。
     */
    val taskDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            com.perol.pixez.shared.data.local.task.TaskDatabase.Schema,
            "task.db",
        )
    }

    /**
     * 屏蔽作品数据库驱动，复用旧 Flutter banillustid.db。
     */
    val banDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase.Schema,
            "banillustid.db",
        )
    }

    /**
     * 屏蔽画师数据库驱动，复用旧 Flutter banuserid.db。
     */
    val banUserDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase.Schema,
            "banuserid.db",
        )
    }

    /**
     * 屏蔽标签数据库驱动，复用旧 Flutter bantag.db。
     */
    val banTagDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            com.perol.pixez.shared.data.local.bantag.BanTagDatabase.Schema,
            "bantag.db",
        )
    }

    /**
     * 插画浏览历史数据库驱动，复用旧 Flutter illustpersist.db。
     */
    val illustPersistDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            IllustPersistDatabase.Schema,
            "illustpersist.db",
        )
    }

    /**
     * 小说浏览历史数据库驱动，复用旧 Flutter Novelpersist.db。
     */
    val novelPersistDriver: SqlDriver by lazy {
        driverFactory.createDriver(
            NovelPersistDatabase.Schema,
            "Novelpersist.db",
        )
    }

    /**
     * 设置仓库，桥接旧 SharedPreferences / NSUserDefaults。
     */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsFactory.createSettings())
    }

    /**
     * Token 与账号本地存储。
     */
    val tokenStorage: AuthTokenStorage by lazy {
        AuthTokenStorage(accountDriver)
    }

    /**
     * 网络客户端，包含业务 API 与 OAuth 客户端。
     * 默认关闭网络日志，防止在 release 构建中泄露 Authorization token。
     */
    val httpClient: PixivHttpClient by lazy {
        PixivHttpClient(
            tokenStorage = tokenStorage,
            languageProvider = {
                val num = settingsRepository.languageNum
                com.perol.pixez.shared.ui.screens.LANGUAGE_OPTIONS.getOrNull(num)?.code ?: "zh-CN"
            },
            enableLogging = true,
        )
    }

    /**
     * 更新检查专用 HttpClient，生命周期由应用容器统一管理。
     */
    val updateCheckClient: HttpClient by lazy {
        createUpdateCheckClient()
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(
            oAuthClient = httpClient.oAuthClient,
            tokenStorage = tokenStorage,
            accountClient = httpClient.accountClient,
        )
    }

    val illustRepository: IllustRepository by lazy {
        IllustRepository(httpClient.apiClient)
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(httpClient.apiClient)
    }

    /**
     * 公告板专用 JSON 解析器，允许未知键、宽松输入并强制非空默认值。
     */
    private val boardJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 公告板专用 HttpClient，配置 JSON 协商与超时，生命周期由 [AppDependencies] 统一管理。
     */
    private val boardHttpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(boardJson)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
        }
    }

    /**
     * 公告板仓库，从 GitHub Raw 拉取官方公告 JSON。
     */
    val boardRepository: BoardRepository by lazy {
        BoardRepository(boardHttpClient)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(httpClient.apiClient)
    }

    val bookmarkRepository: BookmarkRepository by lazy {
        BookmarkRepository(httpClient.apiClient)
    }

    /**
     * 下载历史仓库，复用旧 Flutter task.db 记录下载任务。
     */
    val downloadHistoryRepository: DownloadHistoryRepository by lazy {
        DownloadHistoryRepository(taskDriver)
    }

    /**
     * 插画浏览历史仓库，复用旧 Flutter illustpersist.db。
     */
    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(illustPersistDriver)
    }

    /**
     * 小说浏览历史仓库，复用旧 Flutter Novelpersist.db。
     */
    val novelHistoryRepository: NovelHistoryRepository by lazy {
        NovelHistoryRepository(novelPersistDriver)
    }

    /**
     * 屏蔽数据仓库，聚合作品、画师、标签屏蔽记录。
     */
    val muteRepository: MuteRepository by lazy {
        MuteRepository(banRepository)
    }

    /**
     * 插画下载仓库，负责下载图片字节并调用平台保存，同时写入下载历史。
     */
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            httpClient = httpClient.downloadClient,
            saver = IllustSaver(),
            historyRepository = downloadHistoryRepository,
        )
    }

    /**
     * 屏蔽仓库，封装对旧 banillustid.db、banuserid.db 与 bantag.db 的读写。
     */
    val banRepository: BanRepository by lazy {
        BanRepository(banDriver, banUserDriver, banTagDriver)
    }

    /**
     * 释放数据库与网络资源，应用在退出时调用。
     */
    fun close() {
        runCatching { httpClient.close() }
        runCatching { updateCheckClient.close() }
        runCatching { boardHttpClient.close() }
        runCatching { driverFactory.closeDriver(accountDriver) }
        runCatching { driverFactory.closeDriver(taskDriver) }
        runCatching { driverFactory.closeDriver(banDriver) }
        runCatching { driverFactory.closeDriver(banUserDriver) }
        runCatching { driverFactory.closeDriver(banTagDriver) }
        runCatching { driverFactory.closeDriver(illustPersistDriver) }
        runCatching { driverFactory.closeDriver(novelPersistDriver) }
    }
}
