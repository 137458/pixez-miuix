package com.perol.pixez.shared.ui.navigation

import com.perol.pixez.shared.AppDependencies
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
import com.perol.pixez.shared.data.repository.SauceNaoRepository
import com.perol.pixez.shared.data.repository.SearchRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.repository.UpdateRepository
import com.perol.pixez.shared.data.repository.WatchlistRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.AppUpdateDownloader

/**
 * RootContent 链路的仓储聚合容器：将原本 RootContent → RootChildContent → render*
 * 逐级透传的 15 个仓储收敛为单一不可变参数。
 *
 * 字段与 [AppDependencies] 中的同名仓储一一对应，由 [toRepositoryRegistry] 投影后
 * 一次性传入 [RootContent]，各页面渲染分支按需从本容器取值。
 */
data class RepositoryRegistry(
    val illustRepository: IllustRepository,
    val pixivisionRepository: PixivisionRepository,
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
    // 中-9：AppDependencies.novelRepository 为非空 by lazy，唯一构造点传非空值，
    // 可空标注为历史遗留；LocalNovelRepository 快速失败语义要求非空类型。
    val novelRepository: NovelRepository,
    val updateRepository: UpdateRepository,
    // 进程级更新下载器：弹窗与更新页共享同一份下载状态（不是仓储，但同属长生命周期依赖）。
    val updateDownloader: AppUpdateDownloader,
    val sauceNaoRepository: SauceNaoRepository,
    val watchlistRepository: WatchlistRepository,
)

/**
 * 容器投影：把 [AppDependencies] 的仓储字段映射为 UI 侧唯一清单。
 *
 * 装配点只有一处——组合根持有容器，容器持有仓储实例；[AppDependencies] 之外不得再出现
 * 「逐个仓储搬运」的清单，否则新增仓储时要改多处且容易漏。映射保持与构造参数同名同序。
 */
internal fun AppDependencies.toRepositoryRegistry(): RepositoryRegistry = RepositoryRegistry(
    illustRepository = illustRepository,
    pixivisionRepository = pixivisionRepository,
    searchRepository = searchRepository,
    userRepository = userRepository,
    accountRepository = accountRepository,
    bookmarkRepository = bookmarkRepository,
    downloadRepository = downloadRepository,
    downloadHistoryRepository = downloadHistoryRepository,
    banRepository = banRepository,
    settingsRepository = settingsRepository,
    boardRepository = boardRepository,
    historyRepository = historyRepository,
    novelHistoryRepository = novelHistoryRepository,
    muteRepository = muteRepository,
    novelRepository = novelRepository,
    updateRepository = updateRepository,
    updateDownloader = updateDownloader,
    sauceNaoRepository = sauceNaoRepository,
    watchlistRepository = watchlistRepository,
)
