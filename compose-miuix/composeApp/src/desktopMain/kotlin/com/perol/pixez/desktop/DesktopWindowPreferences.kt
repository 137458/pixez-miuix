package com.perol.pixez.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import com.perol.pixez.shared.data.settings.SettingsRepository
import kotlin.math.roundToInt

internal data class DesktopWindowPlacement(
    val size: DpSize,
    val position: WindowPosition,
    val placement: WindowPlacement,
)

/**
 * Persists Desktop window geometry separately from the cross-platform layout settings.
 * Values are expressed in Compose dp so they stay consistent with [WindowPosition].
 */
internal object DesktopWindowPreferences {
    private const val WidthKey = "desktop.window.width_dp"
    private const val HeightKey = "desktop.window.height_dp"
    private const val XKey = "desktop.window.x_dp"
    private const val YKey = "desktop.window.y_dp"
    private const val HasPositionKey = "desktop.window.has_position"
    private const val MaximizedKey = "desktop.window.maximized"

    private const val DefaultWidth = 1120
    private const val DefaultHeight = 760
    const val MinimumWidth = 600
    const val MinimumHeight = 500
    private const val MaximumDimension = 10_000
    private const val MaximumCoordinate = 20_000

    fun load(settings: SettingsRepository): DesktopWindowPlacement {
        val width = settings.getInt(WidthKey, DefaultWidth).coerceIn(MinimumWidth, MaximumDimension)
        val height = settings.getInt(HeightKey, DefaultHeight).coerceIn(MinimumHeight, MaximumDimension)
        val hasValidPosition = settings.getBoolean(HasPositionKey) &&
            isPlausibleCoordinate(settings.getInt(XKey)) &&
            isPlausibleCoordinate(settings.getInt(YKey))
        val position = if (hasValidPosition) {
            WindowPosition.Absolute(settings.getInt(XKey).dp, settings.getInt(YKey).dp)
        } else {
            WindowPosition.PlatformDefault
        }
        val placement = if (settings.getBoolean(MaximizedKey)) {
            WindowPlacement.Maximized
        } else {
            WindowPlacement.Floating
        }
        return DesktopWindowPlacement(DpSize(width.dp, height.dp), position, placement)
    }

    fun save(settings: SettingsRepository, state: DesktopWindowPlacement) {
        settings.setInt(WidthKey, state.size.width.value.roundToInt().coerceIn(MinimumWidth, MaximumDimension))
        settings.setInt(HeightKey, state.size.height.value.roundToInt().coerceIn(MinimumHeight, MaximumDimension))
        settings.setBoolean(MaximizedKey, state.placement == WindowPlacement.Maximized)

        val position = state.position as? WindowPosition.Absolute ?: return
        val x = position.x.value.roundToInt()
        val y = position.y.value.roundToInt()
        if (isPlausibleCoordinate(x) && isPlausibleCoordinate(y)) {
            settings.setInt(XKey, x)
            settings.setInt(YKey, y)
            settings.setBoolean(HasPositionKey, true)
        }
    }

    private fun isPlausibleCoordinate(value: Int): Boolean = value in -MaximumCoordinate..MaximumCoordinate
}
