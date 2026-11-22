package com.perol.pixez.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisplayModePolicyTest {

    private val sampleModes = listOf(
        DisplayModeCandidate(modeId = 1, refreshRate = 60f, physicalWidth = 1080, physicalHeight = 2400),
        DisplayModeCandidate(modeId = 2, refreshRate = 60f, physicalWidth = 1440, physicalHeight = 3200),
        DisplayModeCandidate(modeId = 3, refreshRate = 90f, physicalWidth = 1440, physicalHeight = 3200),
        DisplayModeCandidate(modeId = 4, refreshRate = 120f, physicalWidth = 1440, physicalHeight = 3200),
    )

    @Test
    fun `system display mode clears explicit refresh rate and mode lock`() {
        val selection = DisplayModePolicy.select(DisplayModePolicy.DISPLAY_MODE_SYSTEM, sampleModes)
        assertEquals(0, selection.modeId)
        assertEquals(0f, selection.preferredRefreshRate)
    }

    @Test
    fun `60Hz display mode picks highest resolution 60Hz mode`() {
        val selection = DisplayModePolicy.select(DisplayModePolicy.DISPLAY_MODE_LIMIT_60HZ, sampleModes)
        assertEquals(2, selection.modeId)
        assertEquals(60f, selection.preferredRefreshRate)
    }

    @Test
    fun `high refresh display mode picks highest refresh rate mode`() {
        val selection = DisplayModePolicy.select(DisplayModePolicy.DISPLAY_MODE_HIGH_REFRESH, sampleModes)
        assertEquals(4, selection.modeId)
        assertEquals(120f, selection.preferredRefreshRate)
    }

    @Test
    fun `high refresh display mode falls back safely when modes are empty`() {
        val selection = DisplayModePolicy.select(DisplayModePolicy.DISPLAY_MODE_HIGH_REFRESH, emptyList())
        assertEquals(0, selection.modeId)
        assertEquals(60f, selection.preferredRefreshRate)
    }

    @Test
    fun `MainActivity observes displayMode changes from SettingsRepository`() {
        val mainActivityFile = locateComposeAppMainActivity()
        val source = mainActivityFile.readText()
        assertTrue(
            source.contains("observeDisplayModeChanges()") && source.contains("settingsRepository.displayMode"),
            "MainActivity 应在设置修改 displayMode 后立即重新应用刷新率配置，而非仅等 onResume",
        )
    }

    private fun locateComposeAppMainActivity(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val suffix = "src/main/kotlin/com/perol/pixez/android/MainActivity.kt"
        while (dir != null) {
            val direct = File(dir, suffix)
            if (direct.isFile) return direct
            val nested = File(dir, "composeApp/$suffix")
            if (nested.isFile) return nested
            val rootNested = File(dir, "compose-miuix/composeApp/$suffix")
            if (rootNested.isFile) return rootNested
            dir = dir.parentFile
        }
        error("Unable to locate MainActivity.kt from ${System.getProperty("user.dir")}")
    }
}
