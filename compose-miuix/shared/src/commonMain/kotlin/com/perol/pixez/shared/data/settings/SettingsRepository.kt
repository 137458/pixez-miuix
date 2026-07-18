package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
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
                true -> 2
                false -> 1
                else -> 0
            }
        }
        set(value) {
            settings[SettingsKeys.SAVE_MODE] = value
            // 显式设置后清除旧键，避免后续回退到过期值
            settings.remove(SettingsKeys.IS_HELPLESS_WAY)
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

    var defaultPrivateLike: Boolean
        get() = settings.getBooleanWithLegacyFallback(SettingsKeys.DEFAULT_PRIVATE_LIKE, false)
        set(value) { settings[SettingsKeys.DEFAULT_PRIVATE_LIKE] = value }

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
        } catch (_: Exception) {
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

        private const val LEGACY_KEY_PREFIX = "flutter."
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
