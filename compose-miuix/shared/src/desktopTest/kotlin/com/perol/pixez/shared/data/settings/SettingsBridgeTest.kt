package com.perol.pixez.shared.data.settings

import com.russhwolf.settings.PreferencesSettings
import org.junit.Test
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M2 设置桥接单测。
 * 验证 [SettingsRepository] 可以读写旧 Flutter 应用使用的键，且默认值与旧版一致。
 */
class SettingsBridgeTest {

    @Test
    fun `default values match legacy Flutter defaults`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/defaults")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        assertEquals(0, repo.zoomQuality)
        assertEquals(0, repo.pictureQuality)
        assertEquals("i.pximg.net", repo.pictureSource)
        assertEquals("ech", repo.apiNetworkMode)
        assertEquals("ech", repo.oauthNetworkMode)
        assertEquals("home", repo.welcomePageType)
        assertEquals(0, repo.saveMode)
        assertTrue(repo.useDynamicColor)
        assertFalse(repo.isAmoled)
        assertEquals(2, repo.maxRunningTask)
        assertEquals("title:{title}\npainter:{user_name}\nillust id:{illust_id}", repo.copyInfoText)
        assertEquals("{illust_id}_p{part}", repo.format)
        assertFalse(repo.fileNameEval)
        assertEquals("", repo.nameEval)
        assertFalse(repo.overSanityLevelFolder)
    }

    @Test
    fun `read and write legacy keys`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/readwrite")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        repo.zoomQuality = 2
        repo.pictureSource = "i.pximg.net"
        repo.apiNetworkMode = "ech"
        repo.themeMode = 1
        repo.useDynamicColor = false
        repo.isAmoled = true

        assertEquals(2, repo.zoomQuality)
        assertEquals("i.pximg.net", repo.pictureSource)
        assertEquals("ech", repo.apiNetworkMode)
        assertEquals(1, repo.themeMode)
        assertFalse(repo.useDynamicColor)
        assertTrue(repo.isAmoled)

        // 验证底层 Preferences 里确实写入了对应的键
        assertEquals(2, node.getInt("zoom_quality", -1))
        assertEquals("i.pximg.net", node.get("picture_source", null))
    }

    @Test
    fun `generic string accessors support migration keys`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/migration")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        repo.setString("flutter.legacy_key", "legacy_value")
        assertEquals("legacy_value", repo.getString("flutter.legacy_key"))

        repo.setString("flutter.null_key", null)
        assertNull(repo.getString("flutter.null_key"))
    }

    @Test
    fun `flutter prefixed legacy keys fall back correctly`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/prefix")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        // 旧版 shared_preferences 在 Android/iOS/Desktop 的底层存储中都将键名作为纯文本保留，
        // 包括 flutter. 前缀。在 Desktop 的 Java Preferences 中也应作为单个 key 写入，
        // 而非通过 node("flutter") 拆成子节点。
        node.putInt("flutter.zoom_quality", 2)

        // 新键不存在时，应回退读取 flutter.zoom_quality
        assertEquals(2, repo.zoomQuality)

        // 写入新键后，新键优先
        repo.zoomQuality = 1
        assertEquals(1, repo.zoomQuality)
    }

    @Test
    fun `saveMode falls back to legacy is_helplessway when save_mode is absent`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/savemode")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        // 旧版逻辑：is_helplessway == true -> saveMode 2
        repo.setBoolean("is_helplessway", true)
        assertEquals(2, repo.saveMode)

        // 一旦显式写入 save_mode，优先使用新值，并清除旧键
        repo.saveMode = 0
        assertEquals(0, repo.saveMode)
        assertFalse(node.getBoolean("is_helplessway", false))
    }

    @Test
    fun `format read and write`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/format")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        assertEquals("{illust_id}_p{part}", repo.format)
        repo.format = "{user_name}_{illust_id}"
        assertEquals("{user_name}_{illust_id}", repo.format)
        assertEquals("{user_name}_{illust_id}", node.get("save_format", null))
    }

    @Test
    fun `fileNameEval falls back to legacy int 0 or 1`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/filenameeval")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        assertFalse(repo.fileNameEval)

        // 旧版 Flutter 使用 int 1 表示开启
        node.putInt("file_name_eval", 1)
        assertTrue(repo.fileNameEval)

        // 写入新版布尔值后优先使用新值
        repo.fileNameEval = false
        assertFalse(repo.fileNameEval)
    }

    @Test
    fun `overSanityLevelFolder read and write`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/sanity")
        node.clear()
        val repo = SettingsRepository(PreferencesSettings(node))

        assertFalse(repo.overSanityLevelFolder)
        repo.overSanityLevelFolder = true
        assertTrue(repo.overSanityLevelFolder)
        assertTrue(node.getBoolean("is_over_sanity_level_folder", false))
    }
}
