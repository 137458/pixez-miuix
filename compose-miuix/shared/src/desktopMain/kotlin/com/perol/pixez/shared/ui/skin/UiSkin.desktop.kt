package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.state.UserDetailPageState

actual val isCouiSkinSupported: Boolean = false

@Composable
actual fun CouiSkinHost(enabled: Boolean, content: @Composable () -> Unit) {
    content()
}

@Composable
internal actual fun CouiMainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    // Desktop 不支持 COUI；调用方按 isCouiSkinSupported 保证不会进入此分支。
}

@Composable
internal actual fun CouiMainTabPage(
    tab: RootComponent.MainTab,
    component: RootComponent,
    feedRestore: MainFeedRestore,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiIllustDetailPage(
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
) {
    fallback()
}

@Composable
internal actual fun CouiUserDetailPage(
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
) {
    fallback()
}

@Composable
internal actual fun CouiNovelViewerPage(
    novelId: Long,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onIllustClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiCommentsPage(
    illustId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    isNovel: Boolean,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiRelatedIllustsPage(
    illustId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiLoginPage(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNetworkSettingClick: (() -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiIllustSeriesPage(
    seriesId: Long,
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiUserNovelListPage(
    userId: Long,
    authorName: String,
    onBack: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiUserListPage(
    kind: CouiUserListKind,
    userId: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiDownloadTaskPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiHistoryPage(
    onBack: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
    fallback: @Composable () -> Unit,
) {
    fallback()
}

@Composable
internal actual fun CouiSettingsPage(
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
) {
    fallback()
}
