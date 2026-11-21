package com.perol.pixez.shared.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Miuix 主题令牌之外的项目自有语义色（I-26）：
 * miuix 无收藏/Toast 定制令牌，这些色值原为各组件内硬编码。
 * 取值保持现网原值（含透明度），明暗由 themeMode 解析而非 luminance 反推。
 */
@Immutable
data class AppExtraColors(
    val isDark: Boolean,
    /** toast 胶囊底色（含透明度）：浅 0xFFF2F2F7@0.92 / 深 0xFF222224@0.88。 */
    val toastSurface: Color,
    /** toast 正文前景：浅 0xFF1C1C1E / 深 Color.White。 */
    val onToastSurface: Color,
    /** 深色模式错误 toast 前景覆盖色（浅色为 null，沿用 colorScheme.error）。 */
    val toastErrorTextOverride: Color?,
    /** 已收藏心形强调色：0xFFFF4D6A（miuix 无收藏令牌，项目自有语义，全库唯一使用点）。 */
    val bookmarkActive: Color,
)

val lightAppExtraColors: AppExtraColors = AppExtraColors(
    isDark = false,
    toastSurface = Color(0xFFF2F2F7).copy(alpha = 0.92f),
    onToastSurface = Color(0xFF1C1C1E),
    toastErrorTextOverride = null,
    bookmarkActive = Color(0xFFFF4D6A),
)

val darkAppExtraColors: AppExtraColors = AppExtraColors(
    isDark = true,
    toastSurface = Color(0xFF222224).copy(alpha = 0.88f),
    onToastSurface = Color.White,
    toastErrorTextOverride = Color(0xFFFF6961),
    bookmarkActive = Color(0xFFFF4D6A),
)

val LocalAppExtraColors = staticCompositionLocalOf { lightAppExtraColors }
