package com.perol.pixez.shared.ui.skin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.menu.ProvideCouiMenuBackdrop
import com.itos.coui.theme.CouiColorScheme
import com.itos.coui.theme.CouiTheme as CouiKitTheme
import com.itos.coui.theme.CouiTypography
import com.perol.pixez.shared.ui.coui.core.LocalCouiFloatingBackdrop
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 界面皮肤：同一套业务逻辑下的两套系统视觉。
 *
 * - [HYPEROS]：MIUIX 原生组件（默认）。
 * - [COLOROS]：COUI 原语（Android 皮肤切换）。
 */
enum class UiSkin(val storageIndex: Int) {
    HYPEROS(0),
    COLOROS(1),
    ;

    companion object {
        fun fromIndex(index: Int): UiSkin = entries.firstOrNull { it.storageIndex == index } ?: HYPEROS
    }
}

/** 当前生效皮肤，由根组合在 [com.perol.pixez.shared.ui.navigation.RootContent] 下发。 */
val LocalUiSkin = compositionLocalOf { UiSkin.HYPEROS }

/** 当前平台是否提供 COUI 皮肤（Android API 31+ 恒支持）。 */
val isCouiSkinSupported: Boolean = true

/**
 * COUI 皮肤宿主：开启时把 MIUIX 主题桥接成 ColorOS 色板，并注入 COUI 的页面实现。
 * 必须放在 MiuixTheme 内部调用，以保证 COUI 配色由当前 MIUIX 主题派生。
 *
 * 皮肤差异只在这一处登记：页面调用方只认 [LocalSkinPages]，不再关心当前是哪套皮肤。
 * 关闭时不提供实现，[LocalSkinPages] 保持 null，调用方自然走 MIUIX 原页面。
 *
 * @param dark 当前实际明暗，取自 AppThemeSpec.resolvedDark。COUI 组件的明暗分支（顶栏渐隐色、
 * 底栏选中色、弹层材质）全部以它为准，不允许组件内部再从底色亮度反推。
 */
@Composable
fun CouiSkinHost(enabled: Boolean, dark: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val colors = MiuixTheme.colorScheme
    val textStyles = MiuixTheme.textStyles
    val colorScheme = remember(colors, dark) { couiColorSchemeOf(colors, dark) }
    val typography = remember(textStyles) {
        CouiTypography(
            body = textStyles.body1,
            label = textStyles.footnote1,
            menuItem = textStyles.body2,
            chipLabelFontFamily = textStyles.body2.fontFamily,
            dialogTitleFontFamily = textStyles.title4.fontFamily,
            dialogMessageFontFamily = textStyles.body2.fontFamily,
            dialogButtonFontFamily = textStyles.body1.fontFamily,
            dialogInputFontFamily = textStyles.body1.fontFamily,
            dialogCounterFontFamily = textStyles.footnote1.fontFamily,
        )
    }
    // 浮动底栏的采样源：根层持有，正文容器用 [skinFloatingBarSource] 登记，底栏消费。
    val floatingBackdrop = rememberCouiBackdrop(diagnosticName = "coui-floating-bar")
    CouiKitTheme(
        darkTheme = dark,
        colorScheme = colorScheme,
        typography = typography,
        materialTint = Color.Transparent,
        content = {
            // 窗口级采样源：菜单与弹层材质采样「被它覆盖的整屏内容」，必须在根层登记一次。
            ProvideCouiMenuBackdrop(isolateContent = false) { windowSource ->
                Box(modifier = Modifier.fillMaxSize().then(windowSource)) {
                    CompositionLocalProvider(
                        LocalSkinPages provides CouiSkinPages,
                        LocalCouiFloatingBackdrop provides floatingBackdrop,
                        content = content,
                    )
                }
            }
        },
    )
}

/**
 * 皮肤浮动栏的正文采样源。
 *
 * COUI 浮动底栏的玻璃材质是把「被它遮挡的正文」采样后重建的：皮肤开启时把正文登记为采样源，
 * 皮肤关闭时本地方为空，正文容器原样返回，不产生任何采样开销。挂在正文容器上，且不能覆盖浮动栏自身所在的节点。
 */
@Composable
fun Modifier.skinFloatingBarSource(): Modifier {
    val backdrop = LocalCouiFloatingBackdrop.current ?: return this
    return couiBackdrop(backdrop)
}

private fun couiColorSchemeOf(colors: Colors, isDark: Boolean): CouiColorScheme =
    CouiColorScheme(
        accent = colors.primary,
        onAccent = colors.onPrimary,
        surface = colors.surfaceContainer,
        container = if (isDark) {
            colors.surfaceContainerHighest
        } else {
            lerp(colors.surface, colors.surfaceContainerHigh, 0.55f)
        },
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.onSurfaceVariantSummary,
        outlineVariant = if (isDark) colors.dividerLine else colors.dividerLine.copy(alpha = 0.7f),
        accentOnSurface = if (isDark) colors.onPrimaryVariant else colors.primary,
        menuSurface = colors.surfaceContainer,
        checkboxAccent = colors.primary,
        menuCheckboxAccent = if (isDark) colors.onPrimaryVariant else colors.primary,
        background = colors.surface,
        card = colors.surfaceContainer,
    )
