package com.perol.pixez.shared.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.repository.ReleaseInfo
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.state.NovelBrowsePageState
import com.perol.pixez.shared.ui.state.UserDetailPageState

/** 底栏单项：文案、图标与未读标记由一级标签目录求值后传入，避免皮肤层反向依赖导航目录。 */
internal data class SkinNavItem(
    val title: String,
    val icon: ImageVector,
    val showBadge: Boolean,
    val tab: RootComponent.MainTab? = null,
)

/** 用户列表的三种来源：关注 / 粉丝 / 相关画师，三页共用一套 COUI 实现。 */
internal enum class CouiUserListKind {
    Following,
    Followers,
    Related,
}

/**
 * COUI 皮肤提供的页面实现集合。
 *
 * 皮肤差异只在这一次接缝上表达：调用方读 [LocalSkinPages]，非 null 就调这里的方法，否则直接渲染 MIUIX 原页面。
 * 方法签名与 MIUIX 同名页面保持一致，调用点因而能在 if/else 两支里复用同一组实参表达式。
 */
internal interface SkinPages {
    /**
     * COUI 底栏。浮动模式走官方 NavigationBar（左设置圆钮 + 中间胶囊 + 右搜索圆钮），
     * 非浮动模式或 API < 34 退回简洁贴底 COUI 栏。
     */
    @Composable
    fun navigationBar(
        items: List<SkinNavItem>,
        selectedIndex: Int,
        isFloating: Boolean,
        onSelect: (Int) -> Unit,
        onLeftAccessoryClick: (() -> Unit)? = null,
        onRightAccessoryClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
    )

    /** COUI 宽屏侧边导航栏：平板/宽屏非浮动底栏模式下替代 MIUIX NavigationRail。 */
    @Composable
    fun navigationRail(
        items: List<SkinNavItem>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        onSearchClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
    )

    /** COUI 全局 Toast 覆盖层，替代 MIUIX Squircle Toast。 */
    @Composable
    fun toastOverlay(
        toast: ToastData?,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
    )

    /** COUI 根级版本更新弹窗，替代 MIUIX WindowDialog。 */
    @Composable
    fun updateDialog(
        releaseInfo: ReleaseInfo,
        onDismiss: () -> Unit,
        onUpdate: (url: String) -> Unit,
        onIgnore: ((version: String) -> Unit)?,
    )

    /** COUI 根级登录凭证失效弹窗，替代 MIUIX WindowDialog。 */
    @Composable
    fun authExpiredDialog(
        show: Boolean,
        onRelogin: () -> Unit,
        onDismiss: () -> Unit,
    )

    /** COUI 根级剪贴板 OAuth 二次确认弹窗，替代 MIUIX WindowDialog。 */
    @Composable
    fun clipboardOAuthDialog(
        show: Boolean,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    )

    /**
     * 一级标签页的 COUI 宿主：标签与实现一一对应，没有未覆盖的标签，因此不需要 MIUIX 回退参数。
     * 皮肤关闭时调用方直接在 if/else 的另一支渲染 MIUIX 原页面。
     */
    @Composable
    fun mainTabPage(
        tab: RootComponent.MainTab,
        component: RootComponent,
        feedRestore: MainFeedRestore,
    )

    /** 插画详情页的 COUI 实现。 */
    @Composable
    fun illustDetail(
        illustId: Long,
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
        onCommentsClick: (Long) -> Unit,
        onRelatedIllustsClick: (Long) -> Unit,
        onIllustSeriesClick: (Long) -> Unit,
        onTagClick: (String) -> Unit,
        onIllustClick: ((Long) -> Unit)?,
        onNovelClick: ((Long) -> Unit)?,
    )

    /** 用户详情页（画师详情）的 COUI 实现。 */
    @Composable
    fun userDetail(
        userId: Long,
        pageState: UserDetailPageState,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
        onFollowListClick: (Long) -> Unit,
        onFollowerListClick: (Long) -> Unit,
        onRelatedUsersClick: ((Long) -> Unit)?,
        onUserNovelsClick: ((Long, String) -> Unit)?,
        initialTab: Int,
    )

    /** 小说阅读器（小说详情）的 COUI 实现。 */
    @Composable
    fun novelViewer(
        novelId: Long,
        onBack: () -> Unit,
        onNovelClick: (Long) -> Unit,
        onIllustClick: (Long) -> Unit,
        onUserClick: (Long) -> Unit,
        onCommentsClick: (Long) -> Unit,
    )

    /** 评论页的 COUI 实现。 */
    @Composable
    fun comments(
        illustId: Long,
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
        isNovel: Boolean,
    )

    /** 相关作品页的 COUI 实现。 */
    @Composable
    fun relatedIllusts(
        illustId: Long,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    )

    /** 下载历史页的 COUI 实现。 */
    @Composable
    fun downloadHistory(
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    )

    /** 账号编辑页的 COUI 实现。 */
    @Composable
    fun accountEdit(
        onBack: () -> Unit,
    )

    /** 登录页的 COUI 实现。 */
    @Composable
    fun login(
        onBack: () -> Unit,
        onLoginSuccess: () -> Unit,
        onNetworkSettingClick: (() -> Unit)?,
    )

    /** 插画系列页的 COUI 实现。 */
    @Composable
    fun illustSeries(
        seriesId: Long,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    )

    /** 用户小说列表页的 COUI 实现。 */
    @Composable
    fun userNovelList(
        userId: Long,
        authorName: String,
        onBack: () -> Unit,
        onNovelClick: (Long) -> Unit,
        onUserClick: ((Long) -> Unit)?,
    )

    /** 用户列表页的 COUI 实现。 */
    @Composable
    fun userList(
        kind: CouiUserListKind,
        userId: Long,
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
    )

    /** 下载任务页的 COUI 实现。 */
    @Composable
    fun downloadTask(
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
    )

    /** 历史页的 COUI 实现。 */
    @Composable
    fun history(
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
        onNovelClick: (Long) -> Unit,
    )

    /** 设置页的 COUI 实现。 */
    @Composable
    fun settings(
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
    )

    /** 公告板页的 COUI 实现。 */
    @Composable
    fun board(
        onBack: () -> Unit,
    )

    /** 推荐画师页的 COUI 实现。 */
    @Composable
    fun recomUser(
        onBack: () -> Unit,
        onUserClick: (Long) -> Unit,
    )

    /** 屏蔽设置页的 COUI 实现。 */
    @Composable
    fun shield(
        onBack: () -> Unit,
        onAISettingClick: (showAI: Boolean) -> Unit,
    )

    /** AI 作品显示设置页的 COUI 实现。 */
    @Composable
    fun aiSetting(
        showAI: Boolean,
        onBack: () -> Unit,
    )

    /** 欢迎页设置页的 COUI 实现。 */
    @Composable
    fun welcomePageSetting(
        onBack: () -> Unit,
    )

    /** 桌面小组件设置页的 COUI 实现。 */
    @Composable
    fun widgetRecommend(
        onBack: () -> Unit,
    )

    /** 交互设置页的 COUI 实现。 */
    @Composable
    fun interactionSetting(
        onBack: () -> Unit,
    )

    /** 画质设置页的 COUI 实现。 */
    @Composable
    fun qualitySetting(
        onBack: () -> Unit,
    )

    /** 语言设置页的 COUI 实现。 */
    @Composable
    fun languageSetting(
        onBack: () -> Unit,
    )

    /** 分享格式设置页的 COUI 实现。 */
    @Composable
    fun copyTextSetting(
        onBack: () -> Unit,
    )

    /** 下载设置页的 COUI 实现。 */
    @Composable
    fun downloadSetting(
        onBack: () -> Unit,
    )

    /** 网络设置页的 COUI 实现。 */
    @Composable
    fun networkSetting(
        onBack: () -> Unit,
    )

    /** 关于页的 COUI 实现。 */
    @Composable
    fun about(
        onBack: () -> Unit,
        onThanksClick: () -> Unit,
        onUpdateClick: () -> Unit,
    )

    /** 致谢页的 COUI 实现。 */
    @Composable
    fun thanks(
        onBack: () -> Unit,
    )

    /** 收藏标签页的 COUI 实现。 */
    @Composable
    fun bookTag(
        onBack: () -> Unit,
        onTagSearch: (String) -> Unit,
    )

    /** 主题设置页的 COUI 实现。 */
    @Composable
    fun themeSetting(
        onBack: () -> Unit,
    )

    /** 账号管理页的 COUI 实现。 */
    @Composable
    fun accountManage(
        onBack: () -> Unit,
        onAddAccount: () -> Unit,
    )

    /** 小说浏览页的 COUI 实现。 */
    @Composable
    fun novel(
        pageState: NovelBrowsePageState,
        onBack: () -> Unit,
        onNovelClick: (Long) -> Unit,
        onUserClick: ((Long) -> Unit)?,
    )

    /** 布局设置页的 COUI 实现。 */
    @Composable
    fun layoutSetting(
        onBack: () -> Unit,
    )

    /** 更新设置页的 COUI 实现。 */
    @Composable
    fun updateSetting(
        onBack: () -> Unit,
    )

    /** 数据导入导出页的 COUI 实现。 */
    @Composable
    fun dataExport(
        onBack: () -> Unit,
    )

    /** Spotlight 特辑详情页的 COUI 实现。 */
    @Composable
    fun spotlightDetail(
        article: SpotlightArticle,
        onBack: () -> Unit,
        onIllustClick: (Long) -> Unit,
        onUserClick: (Long) -> Unit,
        onArticleClick: (SpotlightArticle) -> Unit,
    )

    /** 首次启动引导页的 COUI 实现。 */
    @Composable
    fun guide(
        onLoginClick: () -> Unit,
        onFinish: () -> Unit,
    )

    /** 搜索页的 COUI 实现。 */
    @Composable
    fun search(
        initialQuery: String,
        onIllustClick: (Long) -> Unit,
        onUserClick: (Long) -> Unit,
        onNovelClick: (Long) -> Unit,
    )
}

/** 当前平台与皮肤提供的页面实现；为 null 表示不提供替换实现，调用方走 MIUIX 原页面。 */
internal val LocalSkinPages = staticCompositionLocalOf<SkinPages?> { null }
