package com.perol.pixez.shared.ui.navigation

import com.perol.pixez.shared.data.model.SpotlightArticle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 序列化判别符共用前缀：平移前 [Config] 嵌套于 RootComponent 时的包级 FQN 前缀。 */
private const val LEGACY_CONFIG_SERIAL_PREFIX = "com.perol.pixez.shared.ui.navigation.RootComponent.Config."

/**
 * 应用路由配置（中-6a 自 [RootComponent] 平移为顶层类）：childStack 的 child 实例，
 * 可序列化以支持状态保存。子类声明顺序保持平移前原样（纯移动，不重排）。
 *
 * 兼容性：sealed 子类序列化默认携带 `type` 判别符（值为类 FQN）。平移前判别符为
 * `com.perol.pixez.shared.ui.navigation.RootComponent.Config.X`，为保持 Android
 * savedInstanceState（Essenty StateKeeper）跨版本恢复的 wire format 不变，各子类以
 * `@SerialName` 显式钉住平移前 FQN；后续再调整嵌套位置时同步维护。
 *
 * 按域索引：
 * - 一级页面：[Main]
 * - 作品域：[IllustDetail] [Comments] [RelatedIllusts] [IllustSeries] [SpotlightDetail]
 * - 用户域：[UserDetail] [UserFollowList] [UserFollowerList] [RecomUserList] [AccountEdit] [AccountManage]
 * - 小说域：[Novel] [NovelViewer]
 * - 搜索与历史：[Search] [History] [DownloadHistory] [DownloadTask] [DataExport] [BookTag] [Board]
 * - 设置域：[Settings] [Shield] [AISetting] [ThemeSetting] [NetworkSetting] [DownloadSetting]
 *   [LayoutSetting] [LanguageSetting] [WidgetRecommendSetting] [InteractionSetting] [UpdateSetting]
 *   [QualitySetting] [CopyTextSetting] [WelcomePageSetting]
 * - 其他：[Guide] [About] [Thanks] [Login]
 */
@Serializable
sealed class Config {
    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Main")
    data class Main(val tab: RootComponent.MainTab) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "IllustDetail")
    data class IllustDetail(val illustId: Long) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "UserDetail")
    data class UserDetail(val userId: Long, val initialTab: Int = 0) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "SpotlightDetail")
    data class SpotlightDetail(val article: SpotlightArticle) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Guide")
    data object Guide : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Settings")
    data object Settings : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "About")
    data object About : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "BookTag")
    data object BookTag : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Thanks")
    data object Thanks : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Login")
    data object Login : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Comments")
    data class Comments(val illustId: Long, val isNovel: Boolean = false) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "RelatedIllusts")
    data class RelatedIllusts(val illustId: Long) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "IllustSeries")
    data class IllustSeries(val seriesId: Long) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "UserFollowList")
    data class UserFollowList(val userId: Long) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "UserFollowerList")
    data class UserFollowerList(val userId: Long) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "RecomUserList")
    data object RecomUserList : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Search")
    data class Search(val query: String) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "DownloadHistory")
    data object DownloadHistory : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Shield")
    data object Shield : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "AISetting")
    data class AISetting(val showAI: Boolean) : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "ThemeSetting")
    data object ThemeSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "NetworkSetting")
    data object NetworkSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "DownloadSetting")
    data object DownloadSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "LayoutSetting")
    data object LayoutSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "LanguageSetting")
    data object LanguageSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "WidgetRecommendSetting")
    data object WidgetRecommendSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "InteractionSetting")
    data object InteractionSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "UpdateSetting")
    data object UpdateSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "AccountEdit")
    data object AccountEdit : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "History")
    data object History : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "DownloadTask")
    data object DownloadTask : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "DataExport")
    data object DataExport : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Board")
    data object Board : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "QualitySetting")
    data object QualitySetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "CopyTextSetting")
    data object CopyTextSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "WelcomePageSetting")
    data object WelcomePageSetting : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "AccountManage")
    data object AccountManage : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "Novel")
    data object Novel : Config()

    @Serializable
    @SerialName(LEGACY_CONFIG_SERIAL_PREFIX + "NovelViewer")
    data class NovelViewer(val novelId: Long) : Config()
}
