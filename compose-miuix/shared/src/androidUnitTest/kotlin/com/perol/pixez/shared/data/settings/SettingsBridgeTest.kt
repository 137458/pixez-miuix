package com.perol.pixez.shared.data.settings

import com.perol.pixez.shared.data.settings.SettingsKeys
import com.perol.pixez.shared.testutil.InMemorySettings
import org.junit.Test
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
        val repo = SettingsRepository(InMemorySettings())

        assertEquals(0, repo.zoomQuality)
        assertEquals(0, repo.pictureQuality)
        assertEquals("i.pximg.net", repo.pictureSource)
        assertEquals("ech", repo.apiNetworkMode)
        assertEquals("ech", repo.oauthNetworkMode)
        assertEquals("home", repo.welcomePageType)
        assertEquals(0, repo.saveMode)
        assertFalse(repo.useDynamicColor)
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
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        repo.zoomQuality = 2
        repo.pictureSource = "i.pximg.net"
        repo.apiNetworkMode = "ech"
        repo.themeMode = 1
        repo.useDynamicColor = true
        repo.isAmoled = true

        assertEquals(2, repo.zoomQuality)
        assertEquals("i.pximg.net", repo.pictureSource)
        assertEquals("ech", repo.apiNetworkMode)
        assertEquals(1, repo.themeMode)
        assertTrue(repo.useDynamicColor)
        assertTrue(repo.isAmoled)

        // 验证底层 Settings 里确实写入了对应的键
        assertEquals(2, backing.getInt("zoom_quality", -1))
        assertEquals("i.pximg.net", backing.getStringOrNull("picture_source"))
    }

    @Test
    fun `generic string accessors support migration keys`() {
        val repo = SettingsRepository(InMemorySettings())

        repo.setString("flutter.legacy_key", "legacy_value")
        assertEquals("legacy_value", repo.getString("flutter.legacy_key"))

        repo.setString("flutter.null_key", null)
        assertNull(repo.getString("flutter.null_key"))
    }

    @Test
    fun `flutter prefixed legacy keys fall back correctly`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        backing.putInt("flutter.zoom_quality", 2)

        // 新键不存在时，应回退读取 flutter.zoom_quality
        assertEquals(2, repo.zoomQuality)

        // 写入新键后，新键优先
        repo.zoomQuality = 1
        assertEquals(1, repo.zoomQuality)
    }

    @Test
    fun `saveMode falls back to legacy is_helplessway when save_mode is absent`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        // 旧版逻辑：is_helplessway == true -> saveMode 2
        repo.setBoolean("is_helplessway", true)
        assertEquals(2, repo.saveMode)

        // 一旦显式写入 save_mode，优先使用新值，并清除旧键
        repo.saveMode = 0
        assertEquals(0, repo.saveMode)
        assertFalse(backing.getBoolean("is_helplessway", false))
    }

    @Test
    fun `format read and write`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        assertEquals("{illust_id}_p{part}", repo.format)
        repo.format = "{user_name}_{illust_id}"
        assertEquals("{user_name}_{illust_id}", repo.format)
        assertEquals("{user_name}_{illust_id}", backing.getStringOrNull("save_format"))
    }

    @Test
    fun `fileNameEval falls back to legacy int 0 or 1`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        assertFalse(repo.fileNameEval)

        // 旧版 Flutter 使用 int 1 表示开启
        backing.putInt("file_name_eval", 1)
        assertTrue(repo.fileNameEval)

        // 写入新版布尔值后优先使用新值
        repo.fileNameEval = false
        assertFalse(repo.fileNameEval)
    }

    @Test
    fun `overSanityLevelFolder read and write`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        assertFalse(repo.overSanityLevelFolder)
        repo.overSanityLevelFolder = true
        assertTrue(repo.overSanityLevelFolder)
        assertTrue(backing.getBoolean("is_over_sanity_level_folder", false))
    }

    @Test
    fun `hIsNotAllow triggers notifyChanged and persists legacy key`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)
        val initialVersion = repo.changeVersion.value

        assertFalse(repo.hIsNotAllow)
        repo.hIsNotAllow = true
        assertEquals(initialVersion + 1, repo.changeVersion.value)
        assertTrue(repo.hIsNotAllow)
        assertTrue(backing.getBoolean("h_is_not_allow", false))

        repo.hIsNotAllow = false
        assertEquals(initialVersion + 2, repo.changeVersion.value)
        assertFalse(repo.hIsNotAllow)
        assertFalse(backing.getBoolean("h_is_not_allow", true))
    }

    @Test
    fun `banAIIllust nsfwMask feedAIBadge trigger notifyChanged`() {
        val repo = SettingsRepository(InMemorySettings())
        val initialVersion = repo.changeVersion.value

        repo.banAIIllust = true
        assertEquals(initialVersion + 1, repo.changeVersion.value)
        assertTrue(repo.banAIIllust)

        repo.nsfwMask = true
        assertEquals(initialVersion + 2, repo.changeVersion.value)
        assertTrue(repo.nsfwMask)

        repo.feedAIBadge = false
        assertEquals(initialVersion + 3, repo.changeVersion.value)
        assertFalse(repo.feedAIBadge)
    }

    @Test
    fun `non filter write bumps changeVersion but not filterChangeVersion`() {
        val repo = SettingsRepository(InMemorySettings())

        assertEquals(0, repo.changeVersion.value)
        assertEquals(0, repo.filterChangeVersion.value)

        // 普通设置写入只走全局版本号，过滤版本号必须保持不动
        repo.themeMode = 1

        assertEquals(1, repo.changeVersion.value)
        assertEquals(0, repo.filterChangeVersion.value)
    }

    @Test
    fun `filter write bumps both filterChangeVersion and changeVersion`() {
        val repo = SettingsRepository(InMemorySettings())

        // 过滤事件是全局事件子集：过滤版本号自增时全局版本号连带自增
        repo.banAIIllust = true

        assertEquals(1, repo.filterChangeVersion.value)
        assertEquals(1, repo.changeVersion.value)

        repo.nsfwMask = true

        assertEquals(2, repo.filterChangeVersion.value)
        assertEquals(2, repo.changeVersion.value)
    }

    @Test
    fun `hCrossAdapt and hCrossAdapterWidth fall back to legacy keys and persist to new keys`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        // 当 h_cross_adapt / h_cross_adapt_width 不存在时，回退至 cross_adapt / cross_adapt_width
        backing.putBoolean("cross_adapt", false)
        backing.putInt("cross_adapt_width", 240)

        assertFalse(repo.hCrossAdapt)
        assertEquals(240, repo.hCrossAdapterWidth)

        // 写入独立横屏设置后，优先使用独立设置并持久化至 h_cross_adapt / h_cross_adapt_width
        repo.hCrossAdapt = true
        repo.hCrossAdapterWidth = 60

        assertTrue(repo.hCrossAdapt)
        assertEquals(60, repo.hCrossAdapterWidth)
        assertTrue(backing.getBoolean("h_cross_adapt", false))
        assertEquals(60, backing.getInt("h_cross_adapt_width", -1))
    }

    @Test
    fun `searchHistory bookTagList and low level setters bump changeVersion`() {
        val backing = InMemorySettings()
        val repo = SettingsRepository(backing)

        var expectedVersion = repo.changeVersion.value
        repo.searchHistory = listOf("pixiv", "miuix")
        assertEquals(++expectedVersion, repo.changeVersion.value)

        repo.bookTagList = listOf("landscape")
        assertEquals(++expectedVersion, repo.changeVersion.value)

        repo.setString("custom_str", "val")
        assertEquals(++expectedVersion, repo.changeVersion.value)

        repo.setInt("custom_int", 42)
        assertEquals(++expectedVersion, repo.changeVersion.value)

        repo.setBoolean("custom_bool", true)
        assertEquals(++expectedVersion, repo.changeVersion.value)

        repo.setStringList("custom_list", listOf("a", "b"))
        assertEquals(++expectedVersion, repo.changeVersion.value)

        assertTrue(repo.hyperOs3Effect)
        repo.hyperOs3Effect = false
        assertEquals(++expectedVersion, repo.changeVersion.value)
        assertFalse(repo.hyperOs3Effect)
        assertFalse(backing.getBoolean(SettingsKeys.HYPER_OS3_EFFECT, true))
    }
}


