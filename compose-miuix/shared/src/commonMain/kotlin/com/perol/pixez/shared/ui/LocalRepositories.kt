package com.perol.pixez.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf
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
import com.perol.pixez.shared.data.repository.UpdateRepository
import com.perol.pixez.shared.data.repository.UserRepository

/**
 * 各数据仓储的组合注入通道（中-9 单通道收敛）：由 RootContent 在应用根部统一 provide，
 * UI Composable 只读 `LocalXxx.current`，与 [LocalSettingsRepository] 同包同风格。
 *
 * 通道规则（中-9 裁决）：
 * - 全部无默认值，缺失时读取即抛（快速失败），装配缺失首帧即暴露；
 * - Decompose Component/后台/Widget 等非组合环境走构造注入，不走本组 Local；
 * - 禁止回退 AppDependencies 全局单例（中-20）。
 */

/** 插画数据通道：作品详情、相关作品、排行榜/推荐等插画列表页的取数入口。 */
val LocalIllustRepository = staticCompositionLocalOf<IllustRepository> {
    error("LocalIllustRepository not provided")
}

/** 特辑（Pixivision）数据通道：特辑列表与特辑详情页。 */
val LocalPixivisionRepository = staticCompositionLocalOf<PixivisionRepository> {
    error("LocalPixivisionRepository not provided")
}

/** 搜索数据通道：搜索页与搜索结果页。 */
val LocalSearchRepository = staticCompositionLocalOf<SearchRepository> {
    error("LocalSearchRepository not provided")
}

/** 用户数据通道：用户详情、关注/粉丝列表、推荐用户等。 */
val LocalUserRepository = staticCompositionLocalOf<UserRepository> {
    error("LocalUserRepository not provided")
}

/** 账号数据通道：登录、账号编辑/管理、引导页与登录态检测。 */
val LocalAccountRepository = staticCompositionLocalOf<AccountRepository> {
    error("LocalAccountRepository not provided")
}

/** 收藏数据通道：插画/用户详情页的收藏操作。 */
val LocalBookmarkRepository = staticCompositionLocalOf<BookmarkRepository> {
    error("LocalBookmarkRepository not provided")
}

/** 浏览历史通道：详情页自动写入历史与历史记录页（原定义于 App.kt，中-9 收敛至 ui 包）。 */
val LocalHistoryRepository = staticCompositionLocalOf<HistoryRepository> {
    error("LocalHistoryRepository not provided")
}

/** 下载通道：卡片与轻量组件的单页快捷下载及下载任务页（原定义于 App.kt，中-9 收敛至 ui 包）。 */
val LocalDownloadRepository = staticCompositionLocalOf<DownloadRepository> {
    error("LocalDownloadRepository not provided")
}

/** 屏蔽通道：卡片快捷屏蔽与屏蔽管理页（原定义于 App.kt，中-9 收敛至 ui 包）。 */
val LocalBanRepository = staticCompositionLocalOf<BanRepository> {
    error("LocalBanRepository not provided")
}

/** 下载历史通道：下载历史页与下载任务页。 */
val LocalDownloadHistoryRepository = staticCompositionLocalOf<DownloadHistoryRepository> {
    error("LocalDownloadHistoryRepository not provided")
}

/** 公告板数据通道：公告页与设置页公告摘要。 */
val LocalBoardRepository = staticCompositionLocalOf<BoardRepository> {
    error("LocalBoardRepository not provided")
}

/** 小说浏览历史通道：数据导出/导入页。 */
val LocalNovelHistoryRepository = staticCompositionLocalOf<NovelHistoryRepository> {
    error("LocalNovelHistoryRepository not provided")
}

/** 屏蔽数据（Mute）通道：数据导出/导入页。 */
val LocalMuteRepository = staticCompositionLocalOf<MuteRepository> {
    error("LocalMuteRepository not provided")
}

/** 小说数据通道：小说浏览页与小说阅读器。 */
val LocalNovelRepository = staticCompositionLocalOf<NovelRepository> {
    error("LocalNovelRepository not provided")
}

/** 更新检查通道：设置页与更新设置页。 */
val LocalUpdateRepository = staticCompositionLocalOf<UpdateRepository> {
    error("LocalUpdateRepository not provided")
}
