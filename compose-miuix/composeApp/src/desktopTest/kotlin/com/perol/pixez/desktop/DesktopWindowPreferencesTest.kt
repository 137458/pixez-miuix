package com.perol.pixez.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.russhwolf.settings.PreferencesSettings
import org.junit.Test
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopWindowPreferencesTest {
    @Test
    fun `load falls back to a safe default placement`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/window-default")
        node.clear()

        val placement = DesktopWindowPreferences.load(SettingsRepository(PreferencesSettings(node)))

        assertEquals(1120.dp, placement.size.width)
        assertEquals(760.dp, placement.size.height)
        assertEquals(WindowPlacement.Floating, placement.placement)
        assertIs<WindowPosition.PlatformDefault>(placement.position)
    }

    @Test
    fun `save preserves normal bounds and maximized state`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/window-round-trip")
        node.clear()
        val repository = SettingsRepository(PreferencesSettings(node))
        val original = DesktopWindowPlacement(
            size = androidx.compose.ui.unit.DpSize(1280.dp, 840.dp),
            position = WindowPosition.Absolute((-720).dp, 48.dp),
            placement = WindowPlacement.Maximized,
        )

        DesktopWindowPreferences.save(repository, original)
        val restored = DesktopWindowPreferences.load(repository)

        assertEquals(original.size, restored.size)
        assertEquals(WindowPlacement.Maximized, restored.placement)
        val position = assertIs<WindowPosition.Absolute>(restored.position)
        assertEquals((-720).dp, position.x)
        assertEquals(48.dp, position.y)
    }

    @Test
    fun `load clamps invalid dimensions and ignores implausible coordinates`() {
        val node = Preferences.userRoot().node("com/perol/pixez/test/window-invalid")
        node.clear()
        val repository = SettingsRepository(PreferencesSettings(node))
        repository.setInt("desktop.window.width_dp", 1)
        repository.setInt("desktop.window.height_dp", 99_999)
        repository.setInt("desktop.window.x_dp", 99_999)
        repository.setInt("desktop.window.y_dp", -99_999)
        repository.setBoolean("desktop.window.has_position", true)

        val restored = DesktopWindowPreferences.load(repository)

        assertEquals(DesktopWindowPreferences.MinimumWidth.dp, restored.size.width)
        assertEquals(10_000.dp, restored.size.height)
        assertIs<WindowPosition.PlatformDefault>(restored.position)
    }
}
