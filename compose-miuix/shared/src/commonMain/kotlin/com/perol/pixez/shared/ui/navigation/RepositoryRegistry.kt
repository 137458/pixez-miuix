package com.perol.pixez.shared.ui.navigation

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
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import io.ktor.client.HttpClient

/**
 * RootContent 链路的仓储聚合容器：将原本 RootContent → RootChildContent → render*
 * 逐级透传的 14 个仓储与更新检查专用 HttpClient 收敛为单一不可变参数。
 *
 * 字段与 [com.perol.pixez.shared.AppDependencies] 中的同名仓储一一对应，由 App 层装配后
 * 一次性传入 [RootContent]，各页面渲染分支按需从本容器取值。
 *
 * [updateCheckClient] 为裸 [HttpClient] 临时入容器，后续以 UpdateRepository 替换后移出。
 */
data class RepositoryRegistry(
    val illustRepository: IllustRepository,
    val searchRepository: SearchRepository,
    val userRepository: UserRepository,
    val accountRepository: AccountRepository,
    val bookmarkRepository: BookmarkRepository,
    val downloadRepository: DownloadRepository,
    val downloadHistoryRepository: DownloadHistoryRepository,
    val banRepository: BanRepository,
    val settingsRepository: SettingsRepository,
    val boardRepository: BoardRepository,
    val historyRepository: HistoryRepository,
    val novelHistoryRepository: NovelHistoryRepository,
    val muteRepository: MuteRepository,
    val novelRepository: NovelRepository?,
    val updateCheckClient: HttpClient,
)
