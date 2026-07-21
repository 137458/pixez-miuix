package com.perol.pixez.shared.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
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
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Main(MainTab.Hello),
        handleBackButton = true,
        childFactory = ::createChild,
    )

    /**
     * 底部标签切换：将栈重置为对应的主页标签。
     *
     * 使用 replaceCurrent 而非 push，避免底部标签切换累积栈深度，
     * 确保按返回键时直接退出应用而不是在历史标签间回退。
     */
    fun onMainTabSelected(tab: MainTab) {
        // 当前已经在该标签时不重复替换。
        val active = stack.value.active.instance
        if (active is Child.Main && active.tab == tab) return
        navigation.replaceCurrent(Config.Main(tab))
    }

    /**
     * 返回上一级页面。
     */
    fun onBack() {
        navigation.pop()
    }

    /**
     * 打开作品详情页。
     */
    fun onIllustClicked(illustId: Int) {
        navigation.push(Config.IllustDetail(illustId))
    }

    /**
     * 打开用户详情页。
     */
    fun onUserClicked(userId: Int) {
        navigation.push(Config.UserDetail(userId))
    }

    /**
     * 打开设置页。
     */
    fun onSettingsClicked() {
        navigation.push(Config.Settings)
    }

    /**
     * 打开关于页。
     */
    fun onAboutClicked() {
        navigation.push(Config.About)
    }

    /**
     * 打开屏蔽设置页。
     */
    fun onShieldClicked() {
        navigation.push(Config.Shield)
    }

    /**
     * 打开 AI 作品显示设置页。
     *
     * @param showAI 当前账号是否显示 AI 作品，由入口页面从服务器加载后传入。
     */
    fun onAISettingClicked(showAI: Boolean) {
        navigation.push(Config.AISetting(showAI))
    }

    /**
     * 打开主题设置页。
     */
    fun onThemeSettingClicked() {
        navigation.push(Config.ThemeSetting)
    }

    /**
     * 打开网络设置页。
     */
    fun onNetworkSettingClicked() {
        navigation.push(Config.NetworkSetting)
    }

    /**
     * 打开下载设置页。
     */
    fun onDownloadSettingClicked() {
        navigation.push(Config.DownloadSetting)
    }

    /**
     * 打开画质设置页。
     */
    fun onQualitySettingClicked() {
        navigation.push(Config.QualitySetting)
    }

    /**
     * 打开分享格式设置页。
     */
    fun onCopyTextSettingClicked() {
        navigation.push(Config.CopyTextSetting)
    }

    /**
     * 打开隐私设置页。
     */
    fun onPrivacySettingClicked() {
        navigation.push(Config.PrivacySetting)
    }

    /**
     * 打开登录页。
     */
    fun onLoginClicked() {
        navigation.push(Config.Login)
    }

    /**
     * 打开作品评论页。
     */
    fun onCommentsClicked(illustId: Int) {
        navigation.push(Config.Comments(illustId))
    }

    /**
     * 打开相关作品页。
     */
    fun onRelatedIllustsClicked(illustId: Int) {
        navigation.push(Config.RelatedIllusts(illustId))
    }

    /**
     * 打开插画系列页。
     */
    fun onIllustSeriesClicked(seriesId: Int) {
        navigation.push(Config.IllustSeries(seriesId))
    }

    /**
     * 打开用户关注列表页。
     */
    fun onUserFollowListClicked(userId: Int) {
        navigation.push(Config.UserFollowList(userId))
    }

    /**
     * 打开用户粉丝列表页。
     */
    fun onUserFollowerListClicked(userId: Int) {
        navigation.push(Config.UserFollowerList(userId))
    }

    /**
     * 打开推荐用户列表页。
     */
    fun onRecomUserListClicked() {
        navigation.push(Config.RecomUserList)
    }

    /**
     * 打开搜索页并填入指定关键词。
     */
    fun onSearchClicked(query: String) {
        navigation.push(Config.Search(query))
    }

    /**
     * 打开下载历史页。
     */
    fun onDownloadHistoryClicked() {
        navigation.push(Config.DownloadHistory)
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): Child = when (config) {
        is Config.Main -> Child.Main(config.tab)
        is Config.IllustDetail -> Child.IllustDetail(config.illustId)
        is Config.UserDetail -> Child.UserDetail(config.userId)
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
        Config.QualitySetting -> Child.QualitySetting
        Config.CopyTextSetting -> Child.CopyTextSetting
        Config.PrivacySetting -> Child.PrivacySetting
        Config.About -> Child.About
        Config.Login -> Child.Login
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
        data object Settings : Config()

        @Serializable
        data object About : Config()

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
        data object QualitySetting : Config()

        @Serializable
        data object CopyTextSetting : Config()

        @Serializable
        data object PrivacySetting : Config()
    }

    sealed class Child {
        data class Main(val tab: MainTab) : Child()
        data class IllustDetail(val illustId: Int) : Child()
        data class UserDetail(val userId: Int) : Child()
        data object Settings : Child()
        data object About : Child()
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
        data object QualitySetting : Child()
        data object CopyTextSetting : Child()
        data object PrivacySetting : Child()
    }
}
