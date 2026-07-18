package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.util.prefs.Preferences

/**
 * Desktop(JVM) 平台的 [Settings] 工厂。
 *
 * 使用 Java Preferences 作为新存储，并在首次启动时从旧 Flutter 桌面应用生成的
 * `shared_preferences.json` 导入已有设置，避免用户升级后设置丢失。
 */
actual class SettingsFactory {
    actual fun createSettings(): Settings {
        val userRoot = Preferences.userRoot()
        val node = userRoot.node("com/perol/pixez")
        migrateLegacySharedPreferencesIfNeeded(node)
        return PreferencesSettings(node)
    }

    /**
     * 从旧 Flutter 桌面应用的 `shared_preferences.json` 一次性迁移设置。
     *
     * 旧文件位置与旧版 `path_provider.getApplicationSupportDirectory()` 一致：
     * - Windows：`%APPDATA%/com.perol.pixez/shared_preferences.json`
     * - macOS：`~/Library/Application Support/com.perol.pixez/shared_preferences.json`
     * - Linux：`~/.local/share/com.perol.pixez/shared_preferences.json`
     *
     * 迁移完成后在 Java Preferences 中写入标记，避免重复执行。
     */
    private fun migrateLegacySharedPreferencesIfNeeded(target: Preferences) {
        if (target.getBoolean(MIGRATION_FLAG_KEY, false)) return

        val legacyFile = findLegacySharedPreferencesFile() ?: return

        try {
            val json = Json.parseToJsonElement(legacyFile.readText()).jsonObject
            json.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive -> writePrimitive(target, key, value)
                    is JsonArray -> {
                        // shared_preferences 的 List<String> 在桌面 JSON 中存为字符串数组，
                        // 新存储统一序列化为 JSON 字符串，由 SettingsRepository.getStringList 解析。
                        val list = value.mapNotNull { it.jsonPrimitive.contentOrNull }
                        target.put(key, Json.encodeToString(ListSerializer(String.serializer()), list))
                    }
                    else -> {
                        // 忽略对象类型，shared_preferences 不存储对象值。
                    }
                }
            }
            target.putBoolean(MIGRATION_FLAG_KEY, true)
            target.flush()
            Napier.i("Desktop 旧 Flutter 设置迁移完成：${legacyFile.absolutePath}")
        } catch (e: Exception) {
            // 迁移失败不应阻塞应用启动，记录后继续。
            Napier.e("Desktop 旧 Flutter 设置迁移失败", e)
        }
    }

    private fun writePrimitive(target: Preferences, key: String, value: JsonPrimitive) {
        when {
            value.isString -> target.put(key, value.content)
            value.booleanOrNull != null -> target.putBoolean(key, value.booleanOrNull!!)
            value.intOrNull != null -> target.putInt(key, value.intOrNull!!)
            value.longOrNull != null -> target.putLong(key, value.longOrNull!!)
            value.doubleOrNull != null -> target.putDouble(key, value.doubleOrNull!!)
            else -> target.put(key, value.content)
        }
    }

    private fun findLegacySharedPreferencesFile(): File? {
        val root = legacyAppSupportRoot() ?: return null
        val file = File(root, "shared_preferences.json")
        return if (file.exists()) file else null
    }

    /**
     * 返回旧 Flutter 桌面端 `path_provider.getApplicationSupportDirectory()` 目录。
     */
    private fun legacyAppSupportRoot(): File? {
        val home = System.getProperty("user.home") ?: return null
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        return when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrBlank()) File(appData, LEGACY_PACKAGE_DIR)
                else File(home, "AppData/Roaming/$LEGACY_PACKAGE_DIR")
            }
            os.contains("mac") -> File(home, "Library/Application Support/$LEGACY_PACKAGE_DIR")
            else -> File(home, ".local/share/$LEGACY_PACKAGE_DIR")
        }
    }

    companion object {
        private const val LEGACY_PACKAGE_DIR = "com.perol.pixez"
        private const val MIGRATION_FLAG_KEY = "__pixez_m2_settings_migrated__"
    }
}
