package com.perol.pixez.shared.ui.theme

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 主题策略纯函数（nav10）回归：设置快照 -> 主题参数的映射与渲染结果逐字段等价。
 *
 * 断言的是从 RootContent 原样搬迁过来的映射规则，覆盖明暗三态、AMOLED、
 * 动态色开关与 palette 索引越界；这些组合正是主题设置页会写出的取值。
 */
class AppThemeSpecTest {

    private fun settings(
        themeMode: Int = 0,
        isAmoled: Boolean = false,
        useDynamicColor: Boolean = false,
        seedColor: Int = 0xFF3482FF.toInt(),
        paletteStyleIndex: Int = 0,
        useSpec2025: Boolean = false,
    ) = AppThemeSettings(
        themeMode = themeMode,
        isAmoled = isAmoled,
        useDynamicColor = useDynamicColor,
        seedColor = seedColor,
        paletteStyleIndex = paletteStyleIndex,
        useSpec2025 = useSpec2025,
    )

    @Test
    fun `themeMode 三态解析明暗与 ColorSchemeMode`() {
        val followSystemLight = resolveAppThemeSpec(settings(themeMode = 0), systemInDarkTheme = false)
        assertEquals(ColorSchemeMode.System, followSystemLight.colorSchemeMode)
        assertEquals(false, followSystemLight.resolvedDark, "跟随系统 + 系统浅色应解析为浅色")

        val followSystemDark = resolveAppThemeSpec(settings(themeMode = 0), systemInDarkTheme = true)
        assertEquals(ColorSchemeMode.System, followSystemDark.colorSchemeMode)
        assertEquals(true, followSystemDark.resolvedDark, "跟随系统 + 系统深色应解析为深色")

        val light = resolveAppThemeSpec(settings(themeMode = 1), systemInDarkTheme = true)
        assertEquals(ColorSchemeMode.Light, light.colorSchemeMode)
        assertEquals(false, light.resolvedDark, "强制浅色不受系统深色影响")

        val dark = resolveAppThemeSpec(settings(themeMode = 2), systemInDarkTheme = false)
        assertEquals(ColorSchemeMode.Dark, dark.colorSchemeMode)
        assertEquals(true, dark.resolvedDark, "强制深色不受系统浅色影响")
    }

    @Test
    fun `动态色开关切换 Monet 模式并清空种子色`() {
        assertEquals(
            ColorSchemeMode.MonetSystem,
            resolveAppThemeSpec(settings(themeMode = 0, useDynamicColor = true), systemInDarkTheme = false).colorSchemeMode,
        )
        assertEquals(
            ColorSchemeMode.MonetLight,
            resolveAppThemeSpec(settings(themeMode = 1, useDynamicColor = true), systemInDarkTheme = false).colorSchemeMode,
        )
        assertEquals(
            ColorSchemeMode.MonetDark,
            resolveAppThemeSpec(settings(themeMode = 2, useDynamicColor = true), systemInDarkTheme = false).colorSchemeMode,
        )

        val dynamic = resolveAppThemeSpec(settings(useDynamicColor = true), systemInDarkTheme = false)
        assertNull(dynamic.keyColor, "动态色模式下不注入种子色")

        val seeded = resolveAppThemeSpec(
            settings(useDynamicColor = false, seedColor = 0xFF123456.toInt()),
            systemInDarkTheme = false,
        )
        assertEquals(Color(0xFF123456), seeded.keyColor, "非动态色模式使用设置中的种子色")
    }

    @Test
    fun `AMOLED 开关决定深色色板是否被覆盖`() {
        val normal = resolveAppThemeSpec(settings(isAmoled = false), systemInDarkTheme = false)
        assertNull(normal.darkColors, "未开启 AMOLED 时不覆盖深色色板（沿用 Miuix 默认）")

        val amoled = resolveAppThemeSpec(settings(isAmoled = true), systemInDarkTheme = false)
        val darkColors = assertNotNull(amoled.darkColors, "开启 AMOLED 时必须提供纯黑深色色板")
        assertEquals(Color.Black, darkColors.background, "AMOLED 背景为纯黑")
        assertEquals(Color.Black, darkColors.surface, "AMOLED 表面为纯黑")
        assertEquals(Color(0xFF121212), darkColors.surfaceVariant, "AMOLED 次级表面保留 #121212")
        assertEquals(Color(0xFFF3F4F6), darkColors.onBackground, "AMOLED 前景为高对比浅色")
    }

    @Test
    fun `浅色色板固定为浅灰背景与纯白容器`() {
        val spec = resolveAppThemeSpec(settings(), systemInDarkTheme = true)
        assertEquals(Color(0xFFF6F7F9), spec.lightColors.background, "浅色背景为 #F6F7F9")
        assertEquals(Color(0xFFF6F7F9), spec.lightColors.surface, "浅色表面为 #F6F7F9")
        assertEquals(Color.White, spec.lightColors.surfaceContainer, "浅色容器为纯白")
        assertEquals(Color(0xFF191919), spec.lightColors.onBackground, "浅色前景为 #191919")
        // 深色系统主题不改变浅色色板：两套色板分别供明暗两态使用。
        assertEquals(
            resolveAppThemeSpec(settings(), systemInDarkTheme = false).lightColors.background,
            spec.lightColors.background,
        )
    }

    @Test
    fun `palette 索引越界回退 TonalSpot 且有效索引按枚举顺序映射`() {
        assertEquals(
            ThemePaletteStyle.TonalSpot,
            resolveAppThemeSpec(settings(paletteStyleIndex = -1), systemInDarkTheme = false).paletteStyle,
            "负索引回退默认调色板",
        )
        assertEquals(
            ThemePaletteStyle.TonalSpot,
            resolveAppThemeSpec(settings(paletteStyleIndex = 999), systemInDarkTheme = false).paletteStyle,
            "越界索引回退默认调色板",
        )
        val valid = ThemePaletteStyle.entries.size - 1
        assertEquals(
            ThemePaletteStyle.entries[valid],
            resolveAppThemeSpec(settings(paletteStyleIndex = valid), systemInDarkTheme = false).paletteStyle,
            "有效索引按 ThemePaletteStyle 枚举顺序映射",
        )
    }

    @Test
    fun `Spec2025 开关决定色彩规范`() {
        assertEquals(
            ThemeColorSpec.Spec2021,
            resolveAppThemeSpec(settings(useSpec2025 = false), systemInDarkTheme = false).colorSpec,
        )
        assertEquals(
            ThemeColorSpec.Spec2025,
            resolveAppThemeSpec(settings(useSpec2025 = true), systemInDarkTheme = false).colorSpec,
        )
    }

    @Test
    fun `主题参数整体对同一快照稳定`() {
        val snapshot = settings(themeMode = 2, isAmoled = true, paletteStyleIndex = 2, useSpec2025 = true)
        val first = resolveAppThemeSpec(snapshot, systemInDarkTheme = false)
        val second = resolveAppThemeSpec(snapshot, systemInDarkTheme = false)
        assertTrue(first == second, "同一设置快照必须解析出相等结果（remember 键稳定）")
    }
}
