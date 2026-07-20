package com.perol.pixez.shared

import app.cash.sqldelight.db.SqlDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.repository.AccountRepository
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.BookmarkRepository
import com.perol.pixez.shared.data.repository.DownloadHistoryRepository
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.PixivHttpClient
import com.perol.pixez.shared.platform.IllustSaver

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
        PixivHttpClient(tokenStorage, enableLogging = false)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(httpClient.oAuthClient, tokenStorage)
    }

    val illustRepository: IllustRepository by lazy {
        IllustRepository(httpClient.apiClient)
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(httpClient.apiClient)
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
     * 屏蔽仓库，封装对旧 banillustid.db 与 banuserid.db 的读写。
     */
    val banRepository: BanRepository by lazy {
        BanRepository(banDriver, banUserDriver)
    }

    /**
     * 释放数据库与网络资源，应用在退出时调用。
     */
    fun close() {
        runCatching { httpClient.close() }
        runCatching { driverFactory.closeDriver(accountDriver) }
        runCatching { driverFactory.closeDriver(taskDriver) }
        runCatching { driverFactory.closeDriver(banDriver) }
        runCatching { driverFactory.closeDriver(banUserDriver) }
    }
}
