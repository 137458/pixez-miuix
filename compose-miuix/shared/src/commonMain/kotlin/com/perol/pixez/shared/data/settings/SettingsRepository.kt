package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 用户设置仓库，桥接旧 Flutter 应用的 SharedPreferences / NSUserDefaults。
 *
 * 当前只暴露 M2 阶段最常用的设置项；其余键通过 [Settings] 的泛型 get/set 访问。
 * 读取时兼容旧版 `shared_preferences` 生成的 `flutter.` 前缀键，写入统一使用新键。
 * 默认值与旧版 `lib/store/user_setting.dart` 保持一致。
 */
class SettingsRepository(
    private val settings: Settings,
) {
    // region 画质与网络
    var zoomQuality: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.ZOOM_QUALITY, 0)
        set(value) { settings[SettingsKeys.ZOOM_QUALITY] = value }

    var feedPreviewQuality: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.FEED_PREVIEW_QUALITY, 0)
        set(value) { settings[SettingsKeys.FEED_PREVIEW_QUALITY] = value }

    var pictureQuality: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.PICTURE_QUALITY, 0)
        set(value) { settings[SettingsKeys.PICTURE_QUALITY] = value }

    var mangaQuality: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.MANGA_QUALITY, 0)
        set(value) { settings[SettingsKeys.MANGA_QUALITY] = value }

    var pictureSource: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.PICTURE_SOURCE,
            DEFAULT_PICTURE_SOURCE,
        )
        set(value) { settings[SettingsKeys.PICTURE_SOURCE] = value }

    var apiNetworkMode: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.API_NETWORK_MODE,
            DEFAULT_NETWORK_MODE,
        )
        set(value) { settings[SettingsKeys.API_NETWORK_MODE] = value }

    var oauthNetworkMode: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.OAUTH_NETWORK_MODE,
            DEFAULT_NETWORK_MODE,
        )
        set(value) { settings[SettingsKeys.OAUTH_NETWORK_MODE] = value }

    // region 主题与显示
    var themeMode: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.THEME_MODE, 0)
        set(value) { settings[SettingsKeys.THEME_MODE] = value }

    var useDynamicColor: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.USE_DYNAMIC_COLOR, true)
        set(value) { settings[SettingsKeys.USE_DYNAMIC_COLOR] = value }

    var seedColor: Int?
        get() = settings.getIntWithLegacyFallbackOrNull(SettingsKeys.SEED_COLOR)
        set(value) { settings[SettingsKeys.SEED_COLOR] = value }

    var isAmoled: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.IS_AMOLED, false)
        set(value) { settings[SettingsKeys.IS_AMOLED] = value }

    // region 保存与下载
    var saveMode: Int
        get() {
            // 旧版首次安装默认 0；若曾经设置过 is_helplessway，则按旧逻辑回退 1/2
            val explicit: Int? = settings[SettingsKeys.SAVE_MODE]
            if (explicit != null) return explicit
            val isHelplessWay: Boolean? = settings.getBooleanWithLegacyFallbackOrNull(
                SettingsKeys.IS_HELPLESS_WAY,
            )
            return when (isHelplessWay) {
                true -> SAVE_MODE_LEGACY_HELPLESS
                false -> SAVE_MODE_LEGACY_SAFE
                else -> SAVE_MODE_DEFAULT
            }
        }
        set(value) {
            // Multiplatform Settings 各平台底层（SharedPreferences.Editor/NSUserDefaults 等）
            // 对连续写入已做事务或延迟提交，先删旧键再写新键，避免异常路径下旧键残留导致读取歧义。
            settings.remove(SettingsKeys.IS_HELPLESS_WAY)
            settings[SettingsKeys.SAVE_MODE] = value
        }

    var storePath: String?
        get() = settings.getStringWithLegacyFallbackOrNull(SettingsKeys.STORE_PATH)
        set(value) { settings[SettingsKeys.STORE_PATH] = value }

    var singleFolder: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.SINGLE_FOLDER, false)
        set(value) { settings[SettingsKeys.SINGLE_FOLDER] = value }

    var maxRunningTask: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.MAX_RUNNING_TASK, 2)
        set(value) { settings[SettingsKeys.MAX_RUNNING_TASK] = value }

    // region 通用
    var languageNum: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.LANGUAGE_NUM, 0)
        set(value) { settings[SettingsKeys.LANGUAGE_NUM] = value }

    var welcomePageType: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.WELCOME_PAGE_TYPE,
            DEFAULT_WELCOME_PAGE_TYPE,
        )
        set(value) { settings[SettingsKeys.WELCOME_PAGE_TYPE] = value }

    var nsfwMask: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.NSFW_MASK, false)
        set(value) { settings[SettingsKeys.NSFW_MASK] = value }

    /**
     * 是否本地过滤 AI 生成作品（illust_ai_type == 2）。
     * 对应旧 Flutter mute_store 中的 `ban_ai_illust`。
     */
    var banAIIllust: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.BAN_AI_ILLUST, false)
        set(value) { settings[SettingsKeys.BAN_AI_ILLUST] = value }

    var defaultPrivateLike: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.DEFAULT_PRIVATE_LIKE, false)
        set(value) { settings[SettingsKeys.DEFAULT_PRIVATE_LIKE] = value }

    /**
     * 收藏后自动保存作品。
     */
    var saveAfterStar: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.SAVE_AFTER_STAR, false)
        set(value) { settings[SettingsKeys.SAVE_AFTER_STAR] = value }

    /**
     * 保存后自动收藏作品。
     */
    var starAfterSave: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.STAR_AFTER_SAVE, false)
        set(value) { settings[SettingsKeys.STAR_AFTER_SAVE] = value }

    /**
     * 长按保存时显示确认。
     */
    var longPressSaveConfirm: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.LONG_PRESS_SAVE_CONFIRM, false)
        set(value) { settings[SettingsKeys.LONG_PRESS_SAVE_CONFIRM] = value }

    /**
     * 插画详情页点击保存按钮直接保存，无需长按。
     */
    var illustDetailSaveSkipLongPress: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.ILLUST_DETAIL_SAVE_SKIP_LONG_PRESS,
            false,
        )
        set(value) { settings[SettingsKeys.ILLUST_DETAIL_SAVE_SKIP_LONG_PRESS] = value }

    /**
     * 收藏作品时自动使用收藏标签。
     */
    var autoTagWhenStar: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.AUTO_TAG_WHEN_STAR, false)
        set(value) { settings[SettingsKeys.AUTO_TAG_WHEN_STAR] = value }

    /**
     * 桌面小部件推荐类型：recom / rank / news。
     */
    var widgetIllustType: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.WIDGET_ILLUST_TYPE,
            DEFAULT_WIDGET_ILLUST_TYPE,
        )
        set(value) { settings[SettingsKeys.WIDGET_ILLUST_TYPE] = value }

    /**
     * 适配刘海/挖孔屏（异形屏）。
     */
    var isBangs: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.IS_BANGS, false)
        set(value) { settings[SettingsKeys.IS_BANGS] = value }

    /**
     * 是否限制 R18 内容展示（H 是不行的）。
     * 沿用旧版键 `h_is_not_allow`。
     */
    var hIsNotAllow: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.H_IS_NOT_ALLOW_LEGACY,
            false,
        )
        set(value) { settings[SettingsKeys.H_IS_NOT_ALLOW_LEGACY] = value }

    /**
     * 再次返回退出应用。
     */
    var isReturnAgainToExit: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.IS_RETURN_AGAIN_TO_EXIT,
            false,
        )
        set(value) { settings[SettingsKeys.IS_RETURN_AGAIN_TO_EXIT] = value }

    /**
     * 插画详情页左右滑动切换作品。
     */
    var swipeChangeArtwork: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.SWIPE_CHANGE_ARTWORK,
            true,
        )
        set(value) { settings[SettingsKeys.SWIPE_CHANGE_ARTWORK] = value }

    /**
     * 是否在 Feed 中显示 AI 生成标识。
     */
    var feedAIBadge: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.FEED_AI_BADGE, true)
        set(value) { settings[SettingsKeys.FEED_AI_BADGE] = value }

    /**
     * 收藏作品后自动关注画师。
     */
    var followAfterStar: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.IS_FOLLOW_AFTER_STAR,
            false,
        )
        set(value) { settings[SettingsKeys.IS_FOLLOW_AFTER_STAR] = value }

    /**
     * 使用 WebView 打开 SauceNAO 搜索结果。
     */
    var useSaunceNaoWebview: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.USE_SAUNCE_NAO_WEBVIEW,
            false,
        )
        set(value) { settings[SettingsKeys.USE_SAUNCE_NAO_WEBVIEW] = value }

    /**
     * 竖屏是否启用按宽度自适应网格列数。
     */
    var crossAdapt: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.CROSS_ADAPT, false)
        set(value) { settings[SettingsKeys.CROSS_ADAPT] = value }

    /**
     * 竖屏自适应宽度阈值（100-2160）。
     */
    var crossAdapterWidth: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.CROSS_ADAPT_WIDTH, 100)
            .coerceIn(MIN_CROSS_ADAPTER_WIDTH, MAX_CROSS_ADAPTER_WIDTH)
        set(value) {
            settings[SettingsKeys.CROSS_ADAPT_WIDTH] = value.coerceIn(
                MIN_CROSS_ADAPTER_WIDTH,
                MAX_CROSS_ADAPTER_WIDTH,
            )
        }

    /**
     * 横屏是否启用按宽度自适应网格列数。
     */
    var hCrossAdapt: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.H_CROSS_ADAPT, false)
        set(value) { settings[SettingsKeys.H_CROSS_ADAPT] = value }

    /**
     * 横屏自适应宽度阈值（100-2160）。
     */
    var hCrossAdapterWidth: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.H_CROSS_ADAPT_WIDTH, 100)
            .coerceIn(MIN_CROSS_ADAPTER_WIDTH, MAX_CROSS_ADAPTER_WIDTH)
        set(value) {
            settings[SettingsKeys.H_CROSS_ADAPT_WIDTH] = value.coerceIn(
                MIN_CROSS_ADAPTER_WIDTH,
                MAX_CROSS_ADAPTER_WIDTH,
            )
        }

    /**
     * 平板模式：0=V:H, 1=V:V, 2=H:H。
     */
    var padMode: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.PAD_MODE, 0)
            .coerceIn(PAD_MODE_MIN, PAD_MODE_MAX)
        set(value) { settings[SettingsKeys.PAD_MODE] = value.coerceIn(PAD_MODE_MIN, PAD_MODE_MAX) }

    /**
     * 竖屏固定网格列数（2-4）。
     */
    var crossCount: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.CROSS_COUNT, 2)
            .coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX)
        set(value) { settings[SettingsKeys.CROSS_COUNT] = value.coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX) }

    /**
     * 横屏固定网格列数（2-4）。
     */
    var hCrossCount: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.H_CROSS_COUNT, 2)
            .coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX)
        set(value) { settings[SettingsKeys.H_CROSS_COUNT] = value.coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX) }

    /**
     * 收藏标签列表，用于作品收藏或搜索时快速选择标签。
     */
    var bookTagList: List<String>
        get() = getStringList(SettingsKeys.BOOK_TAG_LIST).orEmpty()
        set(value) { setStringList(SettingsKeys.BOOK_TAG_LIST, value) }

    var copyInfoText: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.COPY_INFO_TEXT,
            "title:{title}\npainter:{user_name}\nillust id:{illust_id}",
        )
        set(value) { settings[SettingsKeys.COPY_INFO_TEXT] = value }

    // region 低级访问
    /**
     * 读取任意旧键的字符串值，优先读新键，再读带 `flutter.` 前缀的旧键。
     */
    fun getString(key: String, default: String? = null): String? =
        if (default != null) {
            settings.getStringWithLegacyFallback(key, default)
        } else {
            settings.getStringWithLegacyFallbackOrNull(key)
        }

    fun setString(key: String, value: String?) {
        settings[key] = value
    }

    fun getInt(key: String, default: Int = 0): Int =
        settings.getIntWithLegacyFallback(key, default)

    fun setInt(key: String, value: Int) {
        settings[key] = value
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        settings.getBooleanWithLegacyFallback(key, default)

    fun setBoolean(key: String, value: Boolean) {
        settings[key] = value
    }

    /**
     * 读取字符串列表。
     * 旧版 shared_preferences 在 Desktop 的 JSON 中直接存 JSON 数组；新存储统一以 JSON 字符串保存，
     * 读取时按 JSON 数组解析，解析失败返回 null。
     */
    fun getStringList(key: String): List<String>? {
        val json = settings.getStringWithLegacyFallbackOrNull(key) ?: return null
        return try {
            Json.decodeFromString(ListSerializer(String.serializer()), json)
        } catch (e: Exception) {
            Napier.w("读取字符串列表失败 key=$key", e)
            null
        }
    }

    fun setStringList(key: String, value: List<String>) {
        settings[key] = Json.encodeToString(ListSerializer(String.serializer()), value)
    }

    companion object {
        // 与旧版 lib/store/user_setting.dart 中 ImageHost / NetworkMode.ech 保持一致
        private const val DEFAULT_PICTURE_SOURCE = "i.pximg.net"
        private const val DEFAULT_NETWORK_MODE = "ech"
        private const val DEFAULT_WELCOME_PAGE_TYPE = "home"
        private const val DEFAULT_WIDGET_ILLUST_TYPE = "recom"

        // 旧版 save_mode 取值：0 默认、1 旧安全模式、2 旧 helpless 模式
        private const val SAVE_MODE_DEFAULT = 0
        private const val SAVE_MODE_LEGACY_SAFE = 1
        private const val SAVE_MODE_LEGACY_HELPLESS = 2

        // 跨适配宽度阈值范围，与旧版 setting_cross_adapter_page.dart 保持一致
        private const val MIN_CROSS_ADAPTER_WIDTH = 100
        private const val MAX_CROSS_ADAPTER_WIDTH = 2160

        // 平板模式取值范围：0=V:H, 1=V:V, 2=H:H
        private const val PAD_MODE_MIN = 0
        private const val PAD_MODE_MAX = 2

        // 固定网格列数范围
        private const val CROSS_COUNT_MIN = 2
        private const val CROSS_COUNT_MAX = 4
    }
}

/**
 * 带旧版 `flutter.` 前缀回退的读取辅助函数。
 * 优先读取新键；若不存在则读取带 `flutter.` 前缀的旧键；仍不存在则返回默认值。
 */
private fun Settings.getStringWithLegacyFallback(key: String, default: String): String {
    val value: String? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key, default]
}

private fun Settings.getStringWithLegacyFallbackOrNull(key: String): String? {
    val value: String? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key]
}

private fun Settings.getIntWithLegacyFallback(key: String, default: Int): Int {
    val value: Int? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key, default]
}

private fun Settings.getIntWithLegacyFallbackOrNull(key: String): Int? {
    val value: Int? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key]
}

private fun Settings.getBooleanWithLegacyFallback(key: String, default: Boolean): Boolean {
    val value: Boolean? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key, default]
}

private fun Settings.getBooleanWithLegacyFallbackOrNull(key: String): Boolean? {
    val value: Boolean? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key]
}

private const val SettingsRepository_LegacyPrefix = "flutter."
