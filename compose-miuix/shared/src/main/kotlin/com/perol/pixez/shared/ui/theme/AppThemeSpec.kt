package com.perol.pixez.shared.ui.theme

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 主题设置快照：调用方（RootContent）从 SettingsRepository 读出的全部主题相关取值。
 *
 * 快照是纯数据，不持有仓储引用，因此「设置 -> 主题参数」的映射可在无组合环境中断言（nav10）。
 *
 * @param themeMode 0 跟随系统，1 浅色，2 深色。
 * @param isAmoled 是否开启 AMOLED 纯黑深色模式。
 * @param useDynamicColor 是否使用 Monet 动态颜色。
 * @param seedColor 非动态色模式下的种子色（ARGB）。
 * @param paletteStyleIndex 调色板风格索引，对应 [ThemePaletteStyle] 枚举顺序。
 * @param useSpec2025 是否使用 Material 2025 色彩规范。
 */
internal data class AppThemeSettings(
    val themeMode: Int,
    val isAmoled: Boolean,
    val useDynamicColor: Boolean,
    val seedColor: Int,
    val paletteStyleIndex: Int,
    val useSpec2025: Boolean,
)

/**
 * 主题策略解析结果（值类型）：[ThemeController] 装配所需的全部主题参数与明暗判定。
 *
 * @param colorSchemeMode 明暗/动态色策略，直接交给 MIUIX 主题控制器。
 * @param keyColor 非动态色模式下的种子色；动态色模式下为 null（由系统取色）。
 * @param paletteStyle 调色板风格。
 * @param colorSpec 色彩规范。
 * @param lightColors 固定浅色色板。
 * @param darkColors AMOLED 深色色板；未开启 AMOLED 时为 null，沿用 MIUIX 默认深色。
 * @param resolvedDark 当前实际明暗（AMOLED 与动态色均不影响明暗判定，与 colorSchemeMode 同源）。
 */
internal data class AppThemeSpec(
    val colorSchemeMode: ColorSchemeMode,
    val keyColor: Color?,
    val paletteStyle: ThemePaletteStyle,
    val colorSpec: ThemeColorSpec,
    val lightColors: Colors,
    val darkColors: Colors?,
    val resolvedDark: Boolean,
)

/**
 * 把设置快照解析为主题策略的纯函数（nav10）。
 *
 * 逐条映射规则与原 RootContent.buildThemeController 完全一致：
 * 明暗三态 -> [ColorSchemeMode]（动态色开关切换 Monet 族）、种子色仅在非动态色模式注入、
 * palette 索引越界回退 [ThemePaletteStyle.TonalSpot]、AMOLED 才覆盖深色色板。
 *
 * @param systemInDarkTheme 系统明暗（themeMode = 0 时采用）；组合层由 isSystemInDarkTheme() 提供。
 */
internal fun resolveAppThemeSpec(
    settings: AppThemeSettings,
    systemInDarkTheme: Boolean,
): AppThemeSpec {
    val colorSchemeMode = if (settings.useDynamicColor) {
        when (settings.themeMode) {
            1 -> ColorSchemeMode.MonetLight
            2 -> ColorSchemeMode.MonetDark
            else -> ColorSchemeMode.MonetSystem
        }
    } else {
        when (settings.themeMode) {
            1 -> ColorSchemeMode.Light
            2 -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }
    }
    val resolvedDark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> systemInDarkTheme
    }
    return AppThemeSpec(
        colorSchemeMode = colorSchemeMode,
        keyColor = if (settings.useDynamicColor) null else Color(settings.seedColor),
        paletteStyle = ThemePaletteStyle.entries.getOrNull(settings.paletteStyleIndex)
            ?: ThemePaletteStyle.TonalSpot,
        colorSpec = if (settings.useSpec2025) ThemeColorSpec.Spec2025 else ThemeColorSpec.Spec2021,
        lightColors = AppLightColors,
        darkColors = if (settings.isAmoled) AppAmoledDarkColors else null,
        resolvedDark = resolvedDark,
    )
}

/** 按本策略装配 MIUIX 主题控制器；未开启 AMOLED 时不下发深色色板，沿用 MIUIX 默认深色。 */
internal fun AppThemeSpec.toThemeController(): ThemeController = if (darkColors != null) {
    ThemeController(
        colorSchemeMode = colorSchemeMode,
        keyColor = keyColor,
        lightColors = lightColors,
        darkColors = darkColors,
        colorSpec = colorSpec,
        paletteStyle = paletteStyle,
    )
} else {
    ThemeController(
        colorSchemeMode = colorSchemeMode,
        keyColor = keyColor,
        lightColors = lightColors,
        colorSpec = colorSpec,
        paletteStyle = paletteStyle,
    )
}

/** 自定义浅色色板：背景为浅灰色（#F6F7F9），卡片/容器为纯白，显式指定高对比度深色文本前景色。 */
private val AppLightColors: Colors = lightColorScheme(
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFF6F7F9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F1F4),
    surfaceContainerHighest = Color(0xFFE5E7EB),
    onBackground = Color(0xFF191919),
    onSurface = Color(0xFF191919),
    onSurfaceContainer = Color(0xFF191919),
    onSurfaceVariantSummary = Color(0xFF666666),
    onSurfaceSecondary = Color(0xFF888888),
)

/** AMOLED 深色色板：背景与表面设为纯黑，前景为高对比浅色。 */
private val AppAmoledDarkColors: Colors = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceContainer = Color(0xFFF3F4F6),
    onSurfaceVariantSummary = Color(0xFF9CA3AF),
    onSurfaceSecondary = Color(0xFF9CA3AF),
)
