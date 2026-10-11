package com.perol.pixez.shared

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.SqlDelightAuthTokenStorage
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
import com.perol.pixez.shared.data.repository.NovelRepository
import com.perol.pixez.shared.data.repository.PixivisionRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.HttpClientDefaults
import com.perol.pixez.shared.network.PixivHttpClient
import com.perol.pixez.shared.network.webTimeouts
import com.perol.pixez.shared.platform.APP_LANGUAGE_CODES
import com.perol.pixez.shared.platform.IllustSaver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import com.perol.pixez.shared.network.createUpdateCheckClient
import com.perol.pixez.shared.platform.installErrorLogAntilog
import com.perol.pixez.shared.platform.isDebugBuild
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

/**
 * 应用级依赖容器。
 *
 * 在平台入口（Android Application / Desktop main）中创建，并通过 [App] 传递到 UI 层。
 * Android 端容器随进程常驻，不随 Activity 销毁关闭。
 * 负责集中管理数据库驱动、网络客户端、Repository 等生命周期较长的对象。
 */
class AppDependencies(
    val driverFactory: DriverFactory,
    val settingsFactory: SettingsFactory,
) {
    init {
        currentInstance = this
        runCatching {
            // debug 构建安装全量控制台日志；release 不安装（整体关闭）。
            if (isDebugBuild) Napier.base(DebugAntilog())
            // 错误级落盘（架构审查 中-12，裁决 2026-10-11）：W/E 级经 ErrorLogSanitizer 红线脱敏
            // （不落 Authorization/token/完整 URL/用户标识）后写入平台日志目录滚动文件（5MB×3），
            // 仅落私有目录、不自动外发。debug/release 二选一（实施步骤 2）由 androidMain actual
            // 按 isDebugBuild 执行（debug 仅 DebugAntilog 不落盘）；desktop 由 pixez.debug 属性判定，
            // 默认 release 态发布 EXE 无控制台、落盘为唯一可观测通道，debug 态与 DebugAntilog 双通道。
            installErrorLogAntilog()
        }
    }

    companion object {
        @kotlin.concurrent.Volatile
        private var currentInstance: AppDependencies? = null

        fun orNull(): AppDependencies? = currentInstance
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
        SqlDelightAuthTokenStorage(
            driver = accountDriver,
            getActiveUserId = { settingsRepository.activeUserId },
            setActiveUserId = { settingsRepository.activeUserId = it },
        )
    }

    /**
     * DoH 网络模式提供者：按请求主机是否为 OAuth 域读取对应用户设置。
     * 注入全部 HttpClient 的 DoH 策略，取代 network 层对本容器的静态反查（架构审查高-2）。
     */
    private val dnsModeProvider: (isOauthHost: Boolean) -> String? = { isOauthHost ->
        if (isOauthHost) settingsRepository.oauthNetworkMode else settingsRepository.apiNetworkMode
    }

    /**
     * 网络客户端，包含业务 API 与 OAuth 客户端。
     * 默认关闭网络日志，防止在 release 构建中泄露 Authorization token。
     * Accept-Language 取自 platform 层语言码表（架构审查 中-1），
     * 装配层不再反向引用 ui.i18n。
     */
    val httpClient: PixivHttpClient by lazy {
        PixivHttpClient(
            tokenStorage = tokenStorage,
            languageProvider = {
                val num = settingsRepository.languageNum
                APP_LANGUAGE_CODES.getOrNull(num) ?: "zh-CN"
            },
            enableLogging = false,
            modeProvider = dnsModeProvider,
        )
    }

    /**
     * 更新检查专用 HttpClient，生命周期由应用容器统一管理。
     */
    val updateCheckClient: HttpClient by lazy {
        createUpdateCheckClient(modeProvider = dnsModeProvider)
    }

    /**
     * Pixivision Web 页面抓取专用 HttpClient，生命周期由应用容器统一管理。
     */
    val webClient: HttpClient by lazy {
        com.perol.pixez.shared.network.createPlatformHttpClient(modeProvider = dnsModeProvider) {
            webTimeouts()
        }
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(
            oAuthClient = httpClient.oAuthClient,
            tokenStorage = tokenStorage,
            accountClient = httpClient.accountClient,
            // 高-5：账号身份变化（登出/切换账号）时清理作品内存缓存，防止跨账号读到旧缓存数据。
            illustRepository = illustRepository,
            pixivisionRepository = pixivisionRepository,
        )
    }

    val illustRepository: IllustRepository by lazy {
        IllustRepository(
            apiClient = httpClient.apiClient,
            downloadClient = httpClient.downloadClient,
            settingsRepository = settingsRepository,
        )
    }

    /**
     * Pixivision 特辑仓库：Spotlight 列表与特辑详情（网页抓取/HTML 解析），
     * 自 IllustRepository 拆分（架构审查 中-7）。
     */
    val pixivisionRepository: PixivisionRepository by lazy {
        PixivisionRepository(
            apiClient = httpClient.apiClient,
            webClient = webClient,
            settingsRepository = settingsRepository,
        )
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(httpClient.apiClient, illustRepository)
    }

    val novelRepository: NovelRepository by lazy {
        NovelRepository(httpClient.apiClient)
    }

    /**
     * 公告板专用 HttpClient，配置 JSON 协商与超时，生命周期由 [AppDependencies] 统一管理。
     */
    private val boardHttpClient: HttpClient by lazy {
        com.perol.pixez.shared.network.createPlatformHttpClient(modeProvider = dnsModeProvider) {
            install(ContentNegotiation) {
                json(HttpClientDefaults.commonJson)
            }
            webTimeouts()
        }
    }

    /**
     * 公告板仓库，从 GitHub Raw 拉取官方公告 JSON。
     */
    val boardRepository: BoardRepository by lazy {
        BoardRepository(boardHttpClient)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(httpClient.apiClient, illustRepository)
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
            settingsRepository = settingsRepository,
        )
    }

    /**
     * 屏蔽仓库，封装对旧 banillustid.db、banuserid.db 与 bantag.db 的读写。
     */
    val banRepository: BanRepository by lazy {
        BanRepository(banDriver, banUserDriver, banTagDriver)
    }

    /**
     * 应用启动预热：在后台异步预热常用网络连接与 TLS 握手。
     */
    fun warmupAsync(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            httpClient.warmup()
        }
    }

    /**
     * 释放数据库与网络资源，应用在退出时调用。
     */
    fun close() {
        runCatching { httpClient.close() }
        runCatching { updateCheckClient.close() }
        runCatching { boardHttpClient.close() }
        runCatching { webClient.close() }
        runCatching { driverFactory.closeDriver(accountDriver) }
        runCatching { driverFactory.closeDriver(taskDriver) }
        runCatching { driverFactory.closeDriver(banDriver) }
        runCatching { driverFactory.closeDriver(banUserDriver) }
        runCatching { driverFactory.closeDriver(banTagDriver) }
        runCatching { driverFactory.closeDriver(illustPersistDriver) }
        runCatching { driverFactory.closeDriver(novelPersistDriver) }
        // close 后清理静态单例引用，避免进程存活期间残留悬空容器；
        // 仅当 currentInstance 仍指向本实例时清空，防止旧容器关闭误清新容器的引用。
        if (currentInstance === this) {
            currentInstance = null
        }
    }
}
