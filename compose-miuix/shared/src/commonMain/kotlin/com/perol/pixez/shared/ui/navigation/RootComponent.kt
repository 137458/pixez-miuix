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
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.state.IllustFeedRestoreCache
import com.perol.pixez.shared.ui.state.IllustFeedSnapshot
import com.perol.pixez.shared.ui.state.MainFeedRestore
import com.perol.pixez.shared.ui.state.RankingFeedRestoreCache
import com.perol.pixez.shared.ui.state.RankingFeedSnapshot
import kotlinx.serialization.builtins.serializer

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

    /**
     * Main child 跨页恢复容器（高-5）。Child 镜像删除（中-6a）后 childStack 的 child 实例
     * 即可序列化的 [Config]，无法承载运行时对象，改由 [createChild] 在 Main child 创建时
     * 经 [createMainFeedRestore] 接线到此字段；栈内同一时刻至多一个 Main child，且实例
     * 先于其组合出现，组合层（MainChildSection）直接读取本字段。
     *
     * 实例随 Main child 的 InstanceKeeper 存活整个会话；Decompose 对等配置的栈重置
     * （onLoginSuccess/onGuideFinished）复用既有 back-stack child，工厂不再调用、
     * 恢复容器实例保留（desktopTest 有守护），仅异配置重置或进程死亡时随 child 重建。
     */
    lateinit var mainFeedRestore: MainFeedRestore
        private set

    /**
     * child 实例类型（中-6b 试点起为 Any）：作品详情页为 [IllustDetailComponent]，
     * 其余页面维持平移后的 [Config] 本身（新页面按子 Component 范式接入，旧页面逐步迁移）。
     */
    val stack: Value<ChildStack<Config, Any>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = resolveWelcomePageConfig(settingsRepository),
        // 返回事件由 RootContent 中的 predictiveBackAnimation 显式接管与渲染，
        // 并在单页面/主页标签时自动禁用以允许系统退出。
        handleBackButton = false,
        childFactory = ::createChild,
    )

    private val KEY_SELECTED_TAB = "key_root_selected_tab"

    private val restoredTab: MainTab = stateKeeper.consume(KEY_SELECTED_TAB, String.serializer())
        ?.let { runCatching { MainTab.valueOf(it) }.getOrNull() }
        ?: when (val init = resolveWelcomePageConfig(settingsRepository)) {
            is Config.Main -> init.tab
            else -> MainTab.Hello
        }

    private val _selectedTab = kotlinx.coroutines.flow.MutableStateFlow(restoredTab)
    val selectedTab: kotlinx.coroutines.flow.StateFlow<MainTab> = _selectedTab

    init {
        stateKeeper.register(KEY_SELECTED_TAB, String.serializer()) {
            _selectedTab.value.name
        }
    }


    private val _tabReselectEvents = kotlinx.coroutines.flow.MutableSharedFlow<MainTab>(extraBufferCapacity = 1)
    val tabReselectEvents: kotlinx.coroutines.flow.SharedFlow<MainTab> = _tabReselectEvents

    fun onTabReselected(tab: MainTab) {
        _tabReselectEvents.tryEmit(tab)
    }

    /**
     * 指定标签的再选中事件流：仅当 [tab] 被再次选中时发射 [Unit]，
     * 供该标签页消费以实现回顶与刷新。
     * 统一在此构造过滤流，避免各标签页各自手写 flow 包装。
     */
    fun reselectFlowOf(tab: MainTab): kotlinx.coroutines.flow.Flow<Unit> =
        kotlinx.coroutines.flow.flow {
            tabReselectEvents.collect { event ->
                if (event == tab) emit(Unit)
            }
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
        if (active !is Config.Main) {
            navigation.navigate { currentStack ->
                val mainConfig = currentStack.firstOrNull { it is Config.Main } ?: Config.Main(tab)
                listOf(mainConfig)
            }
        }
        if (_selectedTab.value == tab && active is Config.Main) {
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
     * 当栈顶为一级主页面（[Config.Main]）时不消费返回事件，交由系统处理
     * （退出应用或触发「再次返回退出」）；在二级页面时执行 [navigation.pop] 并返回 true。
     */
    fun onBack(): Boolean {
        val active = stack.value.active.instance
        // 一级页面不拦截返回事件，让系统决定是否退出应用。
        if (active is Config.Main) return false
        // 二级页面消费返回事件，执行出栈。
        navigation.pop()
        return true
    }

    /**
     * 打开作品详情页。
     */
    fun onIllustClicked(illustId: Long) {
        navigation.pushToFront(Config.IllustDetail(illustId))
    }

    /**
     * 打开用户详情页。
     *
     * @param initialTab 初始选中的标签页：0 = 作品，1 = 收藏。
     */
    fun onUserClicked(userId: Long, initialTab: Int = 0) {
        navigation.pushToFront(Config.UserDetail(userId, initialTab))
    }

    fun onUserClicked(userId: Long) {
        onUserClicked(userId, 0)
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
    fun onNovelClicked(novelId: Long) {
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
    fun onCommentsClicked(illustId: Long) {
        navigation.pushToFront(Config.Comments(illustId))
    }

    /**
     * 打开小说评论页。
     */
    fun onNovelCommentsClicked(novelId: Long) {
        navigation.pushToFront(Config.Comments(novelId, isNovel = true))
    }

    /**
     * 打开相关作品页。
     */
    fun onRelatedIllustsClicked(illustId: Long) {
        navigation.pushToFront(Config.RelatedIllusts(illustId))
    }

    /**
     * 打开插画系列页。
     */
    fun onIllustSeriesClicked(seriesId: Long) {
        navigation.pushToFront(Config.IllustSeries(seriesId))
    }

    /**
     * 打开用户关注列表页。
     */
    fun onUserFollowListClicked(userId: Long) {
        navigation.pushToFront(Config.UserFollowList(userId))
    }

    /**
     * 打开用户粉丝列表页。
     */
    fun onUserFollowerListClicked(userId: Long) {
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

    /**
     * childStack 工厂（中-6a）：非试点页 child 实例仍为可序列化的 [Config] 本身，直接透传；
     * 作品详情页（中-6b 试点）实例化为 [IllustDetailComponent]，注入根导航回调。
     * 仅保留 Main 分支的跨页恢复接线（高-5）——consume/register 必须在 child 的
     * ComponentContext 上执行且只执行一次（重复 register 同 key 会抛 IllegalStateException）。
     */
    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): Any {
        if (config is Config.Main) {
            mainFeedRestore = createMainFeedRestore(
                stateKeeper = componentContext.stateKeeper,
                instanceKeeper = componentContext.instanceKeeper,
            )
        }
        if (config is Config.IllustDetail) {
            return IllustDetailComponent(
                componentContext = componentContext,
                config = config,
                onBack = ::onBack,
                onUserClick = ::onUserClicked,
                onCommentsClick = ::onCommentsClicked,
                onRelatedIllustsClick = ::onRelatedIllustsClicked,
                onIllustSeriesClick = ::onIllustSeriesClicked,
                onTagClick = ::onSearchClicked,
                onIllustClick = ::onIllustClicked,
                onNovelClick = ::onNovelClicked,
            )
        }
        return config
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
private fun resolveWelcomePageConfig(settingsRepository: SettingsRepository): Config {
    if (!settingsRepository.hasCompletedGuide) {
        return Config.Guide
    }
    return when (settingsRepository.welcomePageType) {
        "rank" -> Config.Main(RootComponent.MainTab.Ranking)
        "quick_view" -> Config.Main(RootComponent.MainTab.New)
        "search" -> Config.Main(RootComponent.MainTab.Search)
        "setting" -> Config.Settings
        else -> Config.Main(RootComponent.MainTab.Hello)
    }
}

/** Main child 跨页恢复快照（高-5）的 stateKeeper / InstanceKeeper key。 */
internal const val KEY_MAIN_HELLO_FEED = "key_main_hello_feed"
internal const val KEY_MAIN_RANKING_FEED = "key_main_ranking_feed"

/**
 * Main child 跨页恢复接线（高-5）：consume / stateKeeper 注册 / InstanceKeeper 实例获取
 * 全部在此完成，必须在 Main child 组件初始化（createChild 的 Config.Main 分支）调用一次。
 * 禁止放入组合期 remember{}：栈动画换层会销毁重建组合，重复 register 同 key 会触发
 * Essenty 2.5.0 的 IllegalStateException（"Another supplier is already registered"）。
 *
 * 两级恢复语义：InstanceKeeper 实例跨组合与 Activity 重建保留运行时快照；
 * stateKeeper 快照（supplier 每次保存读取最新值）仅用于进程死亡恢复——以保存态注入
 * [IllustFeedRestoreCache] / [RankingFeedRestoreCache] 初始值，恢复后经仓库 illustsCache
 * 按 ID 回查原始数据。
 */
internal fun createMainFeedRestore(
    stateKeeper: StateKeeper,
    instanceKeeper: InstanceKeeper,
): MainFeedRestore {
    val helloSaved = stateKeeper.consume(KEY_MAIN_HELLO_FEED, IllustFeedSnapshot.serializer())
    val hello = instanceKeeper.getOrCreateSimple(KEY_MAIN_HELLO_FEED) {
        IllustFeedRestoreCache(helloSaved)
    }
    stateKeeper.register(KEY_MAIN_HELLO_FEED, IllustFeedSnapshot.serializer()) {
        hello.snapshot
    }

    val rankingSaved = stateKeeper.consume(KEY_MAIN_RANKING_FEED, RankingFeedSnapshot.serializer())
    val ranking = instanceKeeper.getOrCreateSimple(KEY_MAIN_RANKING_FEED) {
        RankingFeedRestoreCache(rankingSaved?.pages.orEmpty())
    }
    stateKeeper.register(KEY_MAIN_RANKING_FEED, RankingFeedSnapshot.serializer()) {
        RankingFeedSnapshot(pages = ranking.snapshot())
    }

    return MainFeedRestore(hello = hello, ranking = ranking)
}
