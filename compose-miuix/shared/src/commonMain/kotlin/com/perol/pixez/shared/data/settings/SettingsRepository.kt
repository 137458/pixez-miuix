package com.perol.pixez.shared.data.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository?> { null }

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
    var changeVersion by mutableIntStateOf(0)
        private set

    fun notifyChanged() {
        changeVersion++
    }

    // region 画质与网络
    var zoomQuality: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.ZOOM_QUALITY, 0)
        set(value) { settings[SettingsKeys.ZOOM_QUALITY] = value; notifyChanged() }

    var feedPreviewQuality: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.FEED_PREVIEW_QUALITY, 0)
        set(value) { settings[SettingsKeys.FEED_PREVIEW_QUALITY] = value; notifyChanged() }

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
        set(value) { settings[SettingsKeys.THEME_MODE] = value; notifyChanged() }

    var useDynamicColor: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.USE_DYNAMIC_COLOR, true)
        set(value) { settings[SettingsKeys.USE_DYNAMIC_COLOR] = value; notifyChanged() }

    var seedColor: Int?
        get() = settings.getIntWithLegacyFallbackOrNull(SettingsKeys.SEED_COLOR)
        set(value) { settings[SettingsKeys.SEED_COLOR] = value; notifyChanged() }

    var isAmoled: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.IS_AMOLED, false)
        set(value) { settings[SettingsKeys.IS_AMOLED] = value; notifyChanged() }

    /**
     * MIUIX 调色板风格索引，对应 [top.yukonga.miuix.kmp.theme.ThemePaletteStyle.entries] 顺序。
     * 默认 0 即 TonalSpot，与 MIUIX 默认行为保持一致。
     */
    var miuixPaletteStyle: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.MIUIX_PALETTE_STYLE, 0)
        set(value) { settings[SettingsKeys.MIUIX_PALETTE_STYLE] = value; notifyChanged() }

    /**
     * 是否使用 Material 2025 色彩规范（Spec2025）。
     * 仅在部分调色板风格下生效，其余风格会由 MIUIX 自动回退到 Spec2021。
     */
    var miuixUseSpec2025: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.MIUIX_USE_SPEC_2025, false)
        set(value) { settings[SettingsKeys.MIUIX_USE_SPEC_2025] = value; notifyChanged() }

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

    /**
     * 保存格式模板，用于非脚本文件名模式。
     * 默认与原 Flutter 版一致：{illust_id}_p{part}。
     */
    var format: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.SAVE_FORMAT,
            DEFAULT_SAVE_FORMAT,
        )
        set(value) { settings[SettingsKeys.SAVE_FORMAT] = value.trim() }

    /**
     * 是否使用脚本文件名（file_name_eval）。
     * 旧版 Flutter 用 int 0/1 存储，读取时兼容转换；新版统一用布尔值存储。
     */
    var fileNameEval: Boolean
        get() = settings.getBooleanWithIntLegacyFallback(
            SettingsKeys.FILE_NAME_EVAL_LEGACY,
            false,
        )
        set(value) { settings[SettingsKeys.FILE_NAME_EVAL_LEGACY] = value }

    /**
     * 脚本文件名代码（name_eval）。开启 fileNameEval 后由该脚本计算文件名。
     */
    var nameEval: String
        get() = settings.getStringWithLegacyFallback(SettingsKeys.NAME_EVAL, "")
        set(value) { settings[SettingsKeys.NAME_EVAL] = value }

    /**
     * R18 作品是否保存到独立文件夹。
     */
    var overSanityLevelFolder: Boolean
        get() = settings.getBooleanWithLegacyFallback(
            SettingsKeys.IS_OVER_SANITY_LEVEL_FOLDER,
            false,
        )
        set(value) { settings[SettingsKeys.IS_OVER_SANITY_LEVEL_FOLDER] = value }

    // region 通用
    var languageNum: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.LANGUAGE_NUM, 0)
        set(value) { settings[SettingsKeys.LANGUAGE_NUM] = value; notifyChanged() }

    /**
     * 是否已完成初次启动向导流程。
     * 旧版 Flutter 中 guide_enable 为 false 时表示向导已完成；null 表示尚未执行向导。
     */
    var hasCompletedGuide: Boolean
        get() {
            val legacy = settings.getBooleanWithLegacyFallbackOrNull(SettingsKeys.GUIDE_ENABLE)
            return legacy != null && !legacy
        }
        set(value) {
            settings[SettingsKeys.GUIDE_ENABLE] = !value
            notifyChanged()
        }

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

    // region 搜索设置
    var searchSort: String
        get() = settings.getStringWithLegacyFallback(SettingsKeys.SEARCH_SORT, "date_desc")
        set(value) { settings[SettingsKeys.SEARCH_SORT] = value }

    var searchTarget: String
        get() = settings.getStringWithLegacyFallback(SettingsKeys.SEARCH_TARGET, "partial_match_for_tags")
        set(value) { settings[SettingsKeys.SEARCH_TARGET] = value }

    var searchAiType: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.SEARCH_AI_TYPE, 0)
        set(value) { settings[SettingsKeys.SEARCH_AI_TYPE] = value }

    var searchBookmarkThreshold: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.SEARCH_BOOKMARK_THRESHOLD, 0)
        set(value) { settings[SettingsKeys.SEARCH_BOOKMARK_THRESHOLD] = value }

    var searchUgoiraFilter: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.SEARCH_UGOIRA_FILTER, 0)
        set(value) { settings[SettingsKeys.SEARCH_UGOIRA_FILTER] = value }

    var searchStartDate: String
        get() = settings.getStringWithLegacyFallback(SettingsKeys.SEARCH_START_DATE, "")
        set(value) { settings[SettingsKeys.SEARCH_START_DATE] = value }

    var searchEndDate: String
        get() = settings.getStringWithLegacyFallback(SettingsKeys.SEARCH_END_DATE, "")
        set(value) { settings[SettingsKeys.SEARCH_END_DATE] = value }

    var searchHistory: List<String>
        get() {
            val raw = settings.getStringWithLegacyFallback(SettingsKeys.SEARCH_HISTORY, "")
            if (raw.isBlank()) return emptyList()
            return try {
                Json.decodeFromString(ListSerializer(String.serializer()), raw)
            } catch (_: Exception) {
                raw.split(",").filter { it.isNotBlank() }
            }
        }
        set(value) {
            val json = Json.encodeToString(ListSerializer(String.serializer()), value)
            settings[SettingsKeys.SEARCH_HISTORY] = json
        }


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
     * 桌面小部件推荐类型：recom / day / week / month / day_male / day_female / news / follow。
     */
    var widgetIllustType: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.WIDGET_ILLUST_TYPE,
            DEFAULT_WIDGET_ILLUST_TYPE,
        )
        set(value) {
            settings[SettingsKeys.WIDGET_ILLUST_TYPE] = value
            notifyChanged()
        }

    /**
     * 桌面小部件独立图片代理源，为空时表示跟随全局设置。
     */
    var widgetPictureSource: String
        get() = settings.getStringWithLegacyFallback(
            SettingsKeys.WIDGET_PICTURE_SOURCE,
            "",
        )
        set(value) {
            settings[SettingsKeys.WIDGET_PICTURE_SOURCE] = value
            notifyChanged()
        }

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
        set(value) {
            settings[SettingsKeys.CROSS_COUNT] = value.coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX)
            notifyChanged()
        }

    /**
     * 横屏固定网格列数（2-4）。
     */
    var hCrossCount: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.H_CROSS_COUNT, 2)
            .coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX)
        set(value) {
            settings[SettingsKeys.H_CROSS_COUNT] = value.coerceIn(CROSS_COUNT_MIN, CROSS_COUNT_MAX)
            notifyChanged()
        }

    /**
     * 是否启用悬浮底栏模式（Liquid Glass 悬浮胶囊底栏 vs 标准全宽毛玻璃底栏）。
     * 默认 true。
     */
    var useFloatingBottomBar: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.USE_FLOATING_BOTTOM_BAR, true)
        set(value) {
            settings[SettingsKeys.USE_FLOATING_BOTTOM_BAR] = value
            notifyChanged()
        }

    /**
     * 悬浮底栏液态玻璃折射强度等级。
     * 0: 弱 (16dp)
     * 1: 标准 (24dp)
     * 2: 强 (36dp - 默认)
     * 3: 超强 (48dp)
     * 4: 极致 (64dp)
     */
    var liquidRefractionLevel: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.LIQUID_REFRACTION_LEVEL, 2)
        set(value) {
            settings[SettingsKeys.LIQUID_REFRACTION_LEVEL] = value
            notifyChanged()
        }


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

    /**
     * 已忽略的版本号；当存在新版本且版本号与此值相同时跳过更新提醒。
     */
    var ignoreUpdateVersion: String?
        get() = settings.getStringWithLegacyFallbackOrNull(SettingsKeys.IGNORE_UPDATE_VERSION)
        set(value) { settings[SettingsKeys.IGNORE_UPDATE_VERSION] = value }

    /**
     * 是否在应用启动时自动检查新版本。
     * 默认 true。
     */
    var autoCheckUpdate: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.AUTO_CHECK_UPDATE, true)
        set(value) { settings[SettingsKeys.AUTO_CHECK_UPDATE] = value }

    // region Android 平台专属
    /**
     * 屏幕显示模式索引。
     * 旧版 Flutter 用 `display_mode` 键存 int，默认 0 表示跟随系统/第一个模式。
     */
    var displayMode: Int
        get() = settings.getIntWithLegacyFallback(SettingsKeys.DISPLAY_MODE, 0)
        set(value) { settings[SettingsKeys.DISPLAY_MODE] = value }

    /**
     * 图片选择器类型。
     * 任务要求暴露为 String，但旧版 Flutter 用 `image_picker_type_renew` 键存 int 0/1。
     * 读取时优先按 String，再兼容旧版 int，最后回退到空字符串；写入统一用新键 String。
     */
    var imagePickerType: String
        get() = settings.getStringOrIntLegacyFallback(
            SettingsKeys.IMAGE_PICKER_TYPE,
            "",
        )
        set(value) { settings[SettingsKeys.IMAGE_PICKER_TYPE] = value }

    /**
     * 是否已开启「默认打开链接」（Android 12+）。
     * 旧版 Flutter 无此持久化键，仅通过 OpenSettingPlugin 跳转系统设置；
     * 新版增加布尔记录，用于在平台设置页展示开关状态。
     */
    var openByDefault: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.OPEN_BY_DEFAULT, false)
        set(value) { settings[SettingsKeys.OPEN_BY_DEFAULT] = value }

    // endregion

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

        // 默认保存格式，与原 Flutter 版 intialFormat 保持一致
        private const val DEFAULT_SAVE_FORMAT = "{illust_id}_p{part}"

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

/**
 * 兼容旧版将 boolean 存为 int 0/1 的键。
 * 旧版 file_name_eval 在 Android/iOS 用 int 0/1 存储，直接按布尔读取可能因类型不匹配崩溃，
 * 因此先尝试 int 再尝试 bool；新版统一用 bool 存储。
 * 对带 flutter. 前缀的旧键做同样回退。
 */
private fun Settings.getBooleanWithIntLegacyFallback(key: String, default: Boolean): Boolean {
    // 先尝试读取旧版 int 0/1
    val intValue: Int? = try { this[key] } catch (_: Exception) { null }
    if (intValue != null) return intValue == 1
    // 再尝试新版 bool
    val boolValue: Boolean? = try { this[key] } catch (_: Exception) { null }
    if (boolValue != null) return boolValue
    // 带 flutter. 前缀的旧键同样先 int 后 bool
    val legacyKey = SettingsRepository_LegacyPrefix + key
    val legacyInt: Int? = try { this[legacyKey] } catch (_: Exception) { null }
    if (legacyInt != null) return legacyInt == 1
    val legacyBool: Boolean? = try { this[legacyKey] } catch (_: Exception) { null }
    if (legacyBool != null) return legacyBool
    return default
}

private fun Settings.getBooleanWithLegacyFallbackOrNull(key: String): Boolean? {
    val value: Boolean? = this[key]
    if (value != null) return value
    return this[SettingsRepository_LegacyPrefix + key]
}

/**
 * 兼容旧版将图片选择器类型存为 int 0/1 的键。
 * 新版统一按 String 存储，读取时若当前键为 String 则直接返回；
 * 若不存在则尝试读取 int 并转换为 "0"/"1"；带 `flutter.` 前缀的旧键同样处理。
 */
private fun Settings.getStringOrIntLegacyFallback(key: String, default: String): String {
    val stringValue: String? = this[key]
    if (stringValue != null) return stringValue
    val intValue: Int? = try { this[key] } catch (_: Exception) { null }
    if (intValue != null) return intValue.toString()
    val legacyKey = SettingsRepository_LegacyPrefix + key
    val legacyString: String? = this[legacyKey]
    if (legacyString != null) return legacyString
    val legacyInt: Int? = try { this[legacyKey] } catch (_: Exception) { null }
    if (legacyInt != null) return legacyInt.toString()
    return default
}

private const val SettingsRepository_LegacyPrefix = "flutter."
