package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.settings.SettingsRepository
import kotlinx.serialization.Serializable

/**
 * 应用根组件，使用 Decompose 管理页面栈。
 *
 * M3 阶段只区分两大类页面：
 * - Main：包含底部导航的 5 个一级标签页。
 * - Detail：从一级页面进入的二级详情页（作品详情、用户详情、设置、关于）。
 */
@OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)
class RootComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = resolveWelcomePageConfig(settingsRepository),
        // 返回事件由 RootContent 中的 predictiveBackAnimation 显式接管与渲染，
        // 并在单页面/主页标签时自动禁用以允许系统退出。
        handleBackButton = false,
        childFactory = ::createChild,
    )

    private val initialTab = when (val init = resolveWelcomePageConfig(settingsRepository)) {
        is Config.Main -> init.tab
        else -> MainTab.Hello
    }

    private val _selectedTab = kotlinx.coroutines.flow.MutableStateFlow(initialTab)
    val selectedTab: kotlinx.coroutines.flow.StateFlow<MainTab> = _selectedTab

    private val _tabReselectEvents = kotlinx.coroutines.flow.MutableSharedFlow<MainTab>(extraBufferCapacity = 1)
    val tabReselectEvents: kotlinx.coroutines.flow.SharedFlow<MainTab> = _tabReselectEvents

    fun onTabReselected(tab: MainTab) {
        _tabReselectEvents.tryEmit(tab)
    }

    /**
     * 底部标签切换：驱动 Pager 切换或回退至主界面。
     *
     * 若当前在二级页面，先将栈重置为主页；
     * 若已处于当前标签，触发 reselect 回顶与刷新事件；
     * 否则更新选中标签，驱动 Pager 平滑滚动。
     */
    fun onMainTabSelected(tab: MainTab) {
        if (tab == MainTab.New) {
            settingsRepository.hasUnreadFeedBadge = false
        }
        val active = stack.value.active.instance
        if (active !is Child.Main) {
            navigation.navigate { currentStack ->
                val mainConfig = currentStack.firstOrNull { it is Config.Main } ?: Config.Main(tab)
                listOf(mainConfig)
            }
        }
        if (_selectedTab.value == tab && active is Child.Main) {
            onTabReselected(tab)
        } else {
            _selectedTab.value = tab
        }
    }

    /**
     * 用户手势滑动 Pager 时同步组件选中标签。
     */
    fun onPageSwiped(tab: MainTab) {
        if (tab == MainTab.New) {
            settingsRepository.hasUnreadFeedBadge = false
        }
        _selectedTab.value = tab
    }

    /**
     * 返回上一级页面。
     *
     * 当栈顶为一级主页面（[Child.Main]）时不消费返回事件，交由系统处理
     * （退出应用或触发「再次返回退出」）；在二级页面时执行 [navigation.pop] 并返回 true。
     */
    fun onBack(): Boolean {
        val active = stack.value.active.instance
        // 一级页面不拦截返回事件，让系统决定是否退出应用。
        if (active is Child.Main) return false
        // 二级页面消费返回事件，执行出栈。
        navigation.pop()
        return true
    }

    /**
     * 打开作品详情页。
     */
    fun onIllustClicked(illustId: Int) {
        navigation.pushToFront(Config.IllustDetail(illustId))
    }

    /**
     * 打开用户详情页。
     */
    fun onUserClicked(userId: Int) {
        navigation.pushToFront(Config.UserDetail(userId))
    }

    /**
     * 打开设置页。
     */
    fun onSettingsClicked() {
        navigation.pushToFront(Config.Settings)
    }

    /**
     * 打开关于页。
     */
    fun onAboutClicked() {
        navigation.pushToFront(Config.About)
    }

    /**
     * 打开收藏标签页。
     */
    fun onBookTagClicked() {
        navigation.pushToFront(Config.BookTag)
    }

    /**
     * 打开致谢页。
     */
    fun onThanksClicked() {
        navigation.pushToFront(Config.Thanks)
    }

    /**
     * 打开屏蔽设置页。
     */
    fun onShieldClicked() {
        navigation.pushToFront(Config.Shield)
    }

    /**
     * 打开 AI 作品显示设置页。
     *
     * @param showAI 当前账号是否显示 AI 作品，由入口页面从服务器加载后传入。
     */
    fun onAISettingClicked(showAI: Boolean) {
        navigation.pushToFront(Config.AISetting(showAI))
    }

    /**
     * 打开主题设置页。
     */
    fun onThemeSettingClicked() {
        navigation.pushToFront(Config.ThemeSetting)
    }

    /**
     * 打开网络设置页。
     */
    fun onNetworkSettingClicked() {
        navigation.pushToFront(Config.NetworkSetting)
    }

    /**
     * 打开下载设置页。
     */
    fun onDownloadSettingClicked() {
        navigation.pushToFront(Config.DownloadSetting)
    }

    /**
     * 打开布局设置页。
     */
    fun onLayoutSettingClicked() {
        navigation.pushToFront(Config.LayoutSetting)
    }

    /**
     * 打开语言设置页。
     */
    fun onLanguageSettingClicked() {
        navigation.pushToFront(Config.LanguageSetting)
    }

    /**
     * 打开小部件推荐类型设置页。
     */
    fun onWidgetRecommendSettingClicked() {
        navigation.pushToFront(Config.WidgetRecommendSetting)
    }

    /**
     * 打开交互习惯开关页。
     */
    fun onInteractionSettingClicked() {
        navigation.pushToFront(Config.InteractionSetting)
    }

    /**
     * 打开更新设置页。
     */
    fun onUpdateSettingClicked() {
        navigation.pushToFront(Config.UpdateSetting)
    }

    /**
     * 打开账号信息编辑页。
     */
    fun onAccountEditClicked() {
        navigation.pushToFront(Config.AccountEdit)
    }

    /**
     * 打开多账号管理页。
     */
    fun onAccountManageClicked() {
        navigation.pushToFront(Config.AccountManage)
    }

    /**
     * 打开小说浏览页（推荐与排行榜）。
     */
    fun onNovelBrowseClicked() {
        navigation.pushToFront(Config.Novel)
    }

    /**
     * 打开小说阅读页。
     */
    fun onNovelClicked(novelId: Int) {
        navigation.pushToFront(Config.NovelViewer(novelId))
    }

    /**
     * 打开浏览历史页。
     */
    fun onHistoryClicked() {
        navigation.pushToFront(Config.History)
    }

    /**
     * 打开下载任务页。
     */
    fun onDownloadTaskClicked() {
        navigation.pushToFront(Config.DownloadTask)
    }

    /**
     * 打开应用数据导入导出页。
     */
    fun onDataExportClicked() {
        navigation.pushToFront(Config.DataExport)
    }

    /**
     * 打开公告板页。
     */
    fun onBoardClicked() {
        navigation.pushToFront(Config.Board)
    }

    /**
     * 打开画质设置页。
     */
    fun onQualitySettingClicked() {
        navigation.pushToFront(Config.QualitySetting)
    }

    /**
     * 打开分享格式设置页。
     */
    fun onCopyTextSettingClicked() {
        navigation.pushToFront(Config.CopyTextSetting)
    }

    /**
     * 打开欢迎页设置页。
     */
    fun onWelcomePageSettingClicked() {
        navigation.pushToFront(Config.WelcomePageSetting)
    }

    /**
     * 打开登录页。
     */
    fun onLoginClicked() {
        navigation.pushToFront(Config.Login)
    }

    /**
     * 登录成功：标记引导完成，清空登录栈并跳转至首页。
     */
    fun onLoginSuccess() {
        settingsRepository.hasCompletedGuide = true
        navigation.navigate { listOf(Config.Main(MainTab.Hello)) }
    }


    /**
     * 打开作品评论页。
     */
    fun onCommentsClicked(illustId: Int) {
        navigation.pushToFront(Config.Comments(illustId))
    }

    /**
     * 打开相关作品页。
     */
    fun onRelatedIllustsClicked(illustId: Int) {
        navigation.pushToFront(Config.RelatedIllusts(illustId))
    }

    /**
     * 打开插画系列页。
     */
    fun onIllustSeriesClicked(seriesId: Int) {
        navigation.pushToFront(Config.IllustSeries(seriesId))
    }

    /**
     * 打开用户关注列表页。
     */
    fun onUserFollowListClicked(userId: Int) {
        navigation.pushToFront(Config.UserFollowList(userId))
    }

    /**
     * 打开用户粉丝列表页。
     */
    fun onUserFollowerListClicked(userId: Int) {
        navigation.pushToFront(Config.UserFollowerList(userId))
    }

    /**
     * 打开推荐用户列表页。
     */
    fun onRecomUserListClicked() {
        navigation.pushToFront(Config.RecomUserList)
    }

    /**
     * 打开搜索页并填入指定关键词。
     */
    fun onSearchClicked(query: String) {
        navigation.pushToFront(Config.Search(query))
    }

    /**
     * 打开下载历史页。
     */
    fun onDownloadHistoryClicked() {
        navigation.pushToFront(Config.DownloadHistory)
    }

    /**
     * 打开 Spotlight 原生特辑阅读页。
     */
    fun onSpotlightArticleClicked(article: SpotlightArticle) {
        navigation.pushToFront(Config.SpotlightDetail(article))
    }

    /**
     * 打开启动向导页。
     */
    fun onGuideClicked() {
        navigation.pushToFront(Config.Guide)
    }

    /**
     * 完成启动向导后重置为欢迎页。
     */
    fun onGuideFinished() {
        settingsRepository.hasCompletedGuide = true
        if (stack.value.items.size > 1) {
            navigation.pop()
        } else {
            navigation.navigate { listOf(resolveWelcomePageConfig(settingsRepository)) }
        }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): Child = when (config) {
        is Config.Main -> Child.Main(config.tab)
        is Config.IllustDetail -> Child.IllustDetail(config.illustId)
        is Config.UserDetail -> Child.UserDetail(config.userId)
        is Config.SpotlightDetail -> Child.SpotlightDetail(config.article)
        Config.Guide -> Child.Guide
        is Config.Comments -> Child.Comments(config.illustId)
        is Config.RelatedIllusts -> Child.RelatedIllusts(config.illustId)
        is Config.IllustSeries -> Child.IllustSeries(config.seriesId)
        is Config.UserFollowList -> Child.UserFollowList(config.userId)
        is Config.UserFollowerList -> Child.UserFollowerList(config.userId)
        is Config.Search -> Child.Search(config.query)
        Config.RecomUserList -> Child.RecomUserList
        Config.DownloadHistory -> Child.DownloadHistory
        Config.Settings -> Child.Settings
        Config.Shield -> Child.Shield
        is Config.AISetting -> Child.AISetting(config.showAI)
        Config.ThemeSetting -> Child.ThemeSetting
        Config.NetworkSetting -> Child.NetworkSetting
        Config.DownloadSetting -> Child.DownloadSetting
        Config.LayoutSetting -> Child.LayoutSetting
        Config.LanguageSetting -> Child.LanguageSetting
        Config.WidgetRecommendSetting -> Child.WidgetRecommendSetting
        Config.InteractionSetting -> Child.InteractionSetting
        Config.UpdateSetting -> Child.UpdateSetting
        Config.AccountEdit -> Child.AccountEdit
        Config.History -> Child.History
        Config.DownloadTask -> Child.DownloadTask
        Config.DataExport -> Child.DataExport
        Config.Board -> Child.Board
        Config.QualitySetting -> Child.QualitySetting
        Config.CopyTextSetting -> Child.CopyTextSetting
        Config.WelcomePageSetting -> Child.WelcomePageSetting
        Config.About -> Child.About
        Config.BookTag -> Child.BookTag
        Config.Thanks -> Child.Thanks
        Config.Login -> Child.Login
        Config.AccountManage -> Child.AccountManage
        Config.Novel -> Child.Novel
        is Config.NovelViewer -> Child.NovelViewer(config.novelId)
    }

    /**
     * 底部 5 个固定标签，与原 Flutter 应用保持一致。
     */
    enum class MainTab {
        Hello,
        Search,
        Ranking,
        New,
        Spotlight,
    }

    /**
     * 路由配置，可序列化以支持状态保存。
     */
    @Serializable
    sealed class Config {
        @Serializable
        data class Main(val tab: MainTab) : Config()

        @Serializable
        data class IllustDetail(val illustId: Int) : Config()

        @Serializable
        data class UserDetail(val userId: Int) : Config()

        @Serializable
        data class SpotlightDetail(val article: SpotlightArticle) : Config()

        @Serializable
        data object Guide : Config()

        @Serializable
        data object Settings : Config()

        @Serializable
        data object About : Config()

        @Serializable
        data object BookTag : Config()

        @Serializable
        data object Thanks : Config()

        @Serializable
        data object Login : Config()

        @Serializable
        data class Comments(val illustId: Int) : Config()

        @Serializable
        data class RelatedIllusts(val illustId: Int) : Config()

        @Serializable
        data class IllustSeries(val seriesId: Int) : Config()

        @Serializable
        data class UserFollowList(val userId: Int) : Config()

        @Serializable
        data class UserFollowerList(val userId: Int) : Config()

        @Serializable
        data object RecomUserList : Config()

        @Serializable
        data class Search(val query: String) : Config()

        @Serializable
        data object DownloadHistory : Config()

        @Serializable
        data object Shield : Config()

        @Serializable
        data class AISetting(val showAI: Boolean) : Config()

        @Serializable
        data object ThemeSetting : Config()

        @Serializable
        data object NetworkSetting : Config()

        @Serializable
        data object DownloadSetting : Config()

        @Serializable
        data object LayoutSetting : Config()

        @Serializable
        data object LanguageSetting : Config()

        @Serializable
        data object WidgetRecommendSetting : Config()

        @Serializable
        data object InteractionSetting : Config()

        @Serializable
        data object UpdateSetting : Config()

        @Serializable
        data object AccountEdit : Config()

        @Serializable
        data object History : Config()

        @Serializable
        data object DownloadTask : Config()

        @Serializable
        data object DataExport : Config()

        @Serializable
        data object Board : Config()

        @Serializable
        data object QualitySetting : Config()

        @Serializable
        data object CopyTextSetting : Config()

        @Serializable
        data object WelcomePageSetting : Config()

        @Serializable
        data object AccountManage : Config()

        @Serializable
        data object Novel : Config()

        @Serializable
        data class NovelViewer(val novelId: Int) : Config()
    }

    sealed class Child {
        data class Main(val tab: MainTab) : Child()
        data class IllustDetail(val illustId: Int) : Child()
        data class UserDetail(val userId: Int) : Child()
        data class SpotlightDetail(val article: SpotlightArticle) : Child()
        data object Guide : Child()
        data object Settings : Child()
        data object About : Child()
        data object BookTag : Child()
        data object Thanks : Child()
        data object Login : Child()
        data class Comments(val illustId: Int) : Child()
        data class RelatedIllusts(val illustId: Int) : Child()
        data class IllustSeries(val seriesId: Int) : Child()
        data class UserFollowList(val userId: Int) : Child()
        data class UserFollowerList(val userId: Int) : Child()
        data object RecomUserList : Child()
        data class Search(val query: String) : Child()
        data object DownloadHistory : Child()
        data object Shield : Child()
        data class AISetting(val showAI: Boolean) : Child()
        data object ThemeSetting : Child()
        data object NetworkSetting : Child()
        data object DownloadSetting : Child()
        data object LayoutSetting : Child()
        data object LanguageSetting : Child()
        data object WidgetRecommendSetting : Child()
        data object InteractionSetting : Child()
        data object UpdateSetting : Child()
        data object AccountEdit : Child()
        data object History : Child()
        data object DownloadTask : Child()
        data object DataExport : Child()
        data object Board : Child()
        data object QualitySetting : Child()
        data object CopyTextSetting : Child()
        data object WelcomePageSetting : Child()
        data object AccountManage : Child()
        data object Novel : Child()
        data class NovelViewer(val novelId: Int) : Child()
    }

    companion object {
        val MAIN_TAB_ORDER = listOf(
            MainTab.Hello,
            MainTab.Search,
            MainTab.Ranking,
            MainTab.New,
            MainTab.Spotlight,
        )
    }
}

/**
 * 根据保存的欢迎页类型与首次启动状态解析初始路由配置。
 * 若尚未完成初次启动向导，优先进入向导页；其余情况按欢迎页设置启动。
 */
private fun resolveWelcomePageConfig(settingsRepository: SettingsRepository): RootComponent.Config {
    if (!settingsRepository.hasCompletedGuide) {
        return RootComponent.Config.Guide
    }
    return when (settingsRepository.welcomePageType) {
        "rank" -> RootComponent.Config.Main(RootComponent.MainTab.Ranking)
        "quick_view" -> RootComponent.Config.Main(RootComponent.MainTab.New)
        "search" -> RootComponent.Config.Main(RootComponent.MainTab.Search)
        "setting" -> RootComponent.Config.Settings
        else -> RootComponent.Config.Main(RootComponent.MainTab.Hello)
    }
}
