package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.state.UserDetailPageState

/** 底栏单项：文案与图标由一级标签目录求值后传入，避免皮肤层反向依赖导航目录。 */
internal data class SkinNavItem(
    val title: String,
    val icon: ImageVector,
    val showBadge: Boolean,
)

/**
 * COUI 底栏。Android 在 API 34+ 使用 COUI 官方 NavigationBar，API 31-33 用自绘胶囊平替。
 */
@Composable
internal expect fun CouiMainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
)

/**
 * 一级标签页的 COUI 宿主：COUI 实现不可用时（或标签尚未迁移）执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiMainTabPage(
    tab: RootComponent.MainTab,
    component: RootComponent,
    feedRestore: MainFeedRestore,
    fallback: @Composable () -> Unit,
)

/**
 * 插画详情页的 COUI 宿主：抢先在 [fallback] 之前渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiIllustDetailPage(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onRelatedIllustsClick: (Long) -> Unit,
    onIllustSeriesClick: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onIllustClick: ((Long) -> Unit)?,
    onNovelClick: ((Long) -> Unit)?,
    fallback: @Composable () -> Unit,
)

/**
 * 用户详情页（画师详情）的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiUserDetailPage(
    userId: Long,
    pageState: UserDetailPageState,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onFollowListClick: (Long) -> Unit,
    onFollowerListClick: (Long) -> Unit,
    onRelatedUsersClick: ((Long) -> Unit)?,
    onUserNovelsClick: ((Long, String) -> Unit)?,
    initialTab: Int,
    fallback: @Composable () -> Unit,
)

/**
 * 小说阅读器（小说详情）的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiNovelViewerPage(
    novelId: Long,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 评论页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiCommentsPage(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    isNovel: Boolean,
    fallback: @Composable () -> Unit,
)

/**
 * 相关作品页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiRelatedIllustsPage(
    illustId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 下载历史页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiDownloadHistoryPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 账号编辑页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiAccountEditPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 登录页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiLoginPage(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNetworkSettingClick: (() -> Unit)?,
    fallback: @Composable () -> Unit,
)

/**
 * 插画系列页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiIllustSeriesPage(
    seriesId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 用户小说列表页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiUserNovelListPage(
    userId: Long,
    authorName: String,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)?,
    fallback: @Composable () -> Unit,
)

/** 用户列表的三种来源：关注 / 粉丝 / 相关画师，三页共用一套 COUI 实现。 */
internal enum class CouiUserListKind {
    Following,
    Followers,
    Related,
}

/**
 * 用户列表页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiUserListPage(
    kind: CouiUserListKind,
    userId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 下载任务页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiDownloadTaskPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 历史页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiHistoryPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 设置页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiSettingsPage(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    onShieldClick: () -> Unit,
    onLoginClick: () -> Unit,
    onDownloadHistoryClick: () -> Unit,
    onThemeSettingClick: () -> Unit,
    onNetworkSettingClick: () -> Unit,
    onDownloadSettingClick: () -> Unit,
    onLayoutSettingClick: () -> Unit,
    onLanguageSettingClick: () -> Unit,
    onWidgetRecommendSettingClick: () -> Unit,
    onInteractionSettingClick: () -> Unit,
    onQualitySettingClick: () -> Unit,
    onCopyTextSettingClick: () -> Unit,
    onWelcomePageSettingClick: () -> Unit,
    onBookTagClick: () -> Unit,
    onUpdateSettingClick: () -> Unit,
    onAccountEditClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDownloadTaskClick: () -> Unit,
    onDataExportClick: () -> Unit,
    onBoardClick: () -> Unit,
    onGuideClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    onUserBookmarksClick: (Long) -> Unit,
    onAccountManageClick: () -> Unit,
    onNovelBrowseClick: () -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 公告板页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiBoardPage(
    onBack: () -> Unit,
    fallback: @Composable () -> Unit,
)

/**
 * 推荐画师页的 COUI 宿主：抢先渲染 COUI 版本；
 * 平台不支持或 COUI 版不可用时执行 [fallback] 的 MIUIX 页面。
 */
@Composable
internal expect fun CouiRecomUserPage(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
)
