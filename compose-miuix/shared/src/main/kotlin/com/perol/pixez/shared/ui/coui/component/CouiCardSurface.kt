package com.perol.pixez.shared.ui.coui.component

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogActionColors
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.CouiDialogMaterial
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.effect.CouiContour
import com.itos.coui.effect.CouiContourStyle
import com.itos.coui.effect.LocalCouiContourGlowEnabled
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme

/** 卡片与对话框表面：卡片底色圆角，以及对话框的背景材质、落位与按键配色统一在此。 */

/**
 * COUI 卡片容器：官方分组卡片口径，列表分组与结果行共用。
 *
 * 配方照抄官方 Group 的卡片本体（CouiPageComponents.kt:92-93）：
 * `padding(horizontal).fillMaxWidth().clip(shape).background(colors.card)`，
 * 这里只取其中「撑满宽度 + 圆角裁切 + 卡片底色」三步，不改用 Group 本身 ——
 * Group 还会强制加 groupSpacing(16dp) 顶部间距与 horizontalMargin(16dp) 横向外边距，
 * 且要求 ColumnScope，会改动调用页既有布局（CouiIllustDetailScreen / CouiSearchResultLists
 * 等页自带 Modifier.padding(horizontal = 12.dp)，CouiSpotlightDetailScreen 在卡片外自挂 clickable）。
 *
 * - 圆角：`CouiPageDefaults.shape` = CouiShape(24.dp, cornerWeight 3f)（CouiPageComponents.kt:54、shape/CouiShape.kt:21-58）；
 * - 底色：`CouiPageDefaults.themeColors().card`，与 CouiTheme.colorScheme.card 同值（CouiPageComponents.kt:66-67）；
 * - 宽度：撑满约束，如需留白由调用方在自己传进来的 [modifier] 上加 padding（modifier 在最外层，与官方 Group 同序）。
 *
 * 卡片本身不是可点表面：官方 Item 只在有 onClick 时才装点击与反馈（CouiPageComponents.kt:138-142），
 * 卡片级点击由调用页自行挂 clickable(interactionSource, indication = null) + couiPressFeedback。
 */
@Composable
internal fun CouiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CouiPageDefaults.shape)
            .background(CouiPageDefaults.themeColors().card),
        content = content,
    )
}

/**
 * COUI 弹窗材质：达到库的门槛且窗口具备背景采样时才启用玻璃，否则退回不透明。
 *
 * 两个前置条件与库内约定逐条对应：CouiDialogWindow 明确「可选材质要求 API 33 与有效 backdrop；
 * 缺失时明确报错」（dialog/CouiDialogWindow.kt:95），渲染时 backdrop 取实参或
 * LocalCouiMenuBackdrop（CouiDialogWindow.kt:173），材质非空即 checkNotNull(backdrop)
 * 再据此算 Kawase 模糊（CouiDialogWindow.kt:309、323）。
 * 因此这里同时校验两项：SDK 达到同包 CouiSdkGates.kt 的 [COUI_DIALOG_MATERIAL_MIN_SDK]（33，
 * 与 `CouiDialogDefaults.isMaterialSupported()` 同值，dialog/CouiDialogDefaults.kt:127-128），
 * 且能经 `rememberCouiWindowBackdrop()`（menu/CouiMenuBackdrop.kt:16）取得窗口背景采样；
 * 任一不满足就返回 null，让弹窗退回不透明表面。不能借用 ChipTabRow 的 34 门槛，
 * 否则 API 33 设备会无故丢失玻璃材质。
 */
@Composable
internal fun couiDialogMaterialOrNull(): CouiDialogMaterial? =
    if (Build.VERSION.SDK_INT >= COUI_DIALOG_MATERIAL_MIN_SDK && rememberCouiWindowBackdrop() != null) {
        CouiDialogDefaults.material(CouiTheme.isDark)
    } else {
        null
    }

/**
 * COUI 弹窗排布：宽屏居中，手机底部弹出。
 *
 * 官方只有 Bottom / Center 两个落位，默认 Bottom 且不按宽度自动切换
 * （dialog/Dialog.kt:61、dialog/CouiDialogWindow.kt:106），落位只影响窗口 gravity、
 * 进场动画与底部留白（CouiDialogWindow.kt:196、207-208、242），
 * 因此 600dp 断点由调用侧决定。
 */
@Composable
internal fun couiDialogPlacement(): CouiDialogPlacement =
    if (LocalConfiguration.current.screenWidthDp >= 600) {
        CouiDialogPlacement.Center
    } else {
        CouiDialogPlacement.Bottom
    }

/**
 * 主操作配色：破坏性操作使用库内 destructive 色。
 *
 * destructive 取 CouiDialogDefaults.colors(dark)（dialog/CouiDialogDefaults.kt:96-104），
 * 其余走当前主题 accent；最终转成官方 primary 配色（同文件 :125 primaryActionColors，
 * 内部即 DialogButtonDefaults.primaryColors(accent)，dialog/CouiDialogButton.kt:57-59）。
 */
@Composable
internal fun couiPrimaryActionColors(danger: Boolean = false): CouiDialogActionColors {
    val accent = if (danger) {
        CouiDialogDefaults.colors(CouiTheme.isDark).destructive
    } else {
        CouiTheme.colorScheme.accent
    }
    return CouiDialogDefaults.primaryActionColors(accent)
}

/** 玻璃弹窗内内容区（说明卡等）使用的半透明表面底色，直接承接弹窗玻璃而不叠不透明卡片。 */
@Composable
internal fun couiDialogSurfaceColor(elevated: Boolean = false): Color {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val alpha = when {
        elevated && dark -> 0.13f
        elevated -> 0.08f
        dark -> 0.09f
        else -> 0.055f
    }
    return scheme.onSurface.copy(alpha = alpha)
}

/** 轮廓凝光材质与弹窗背景材质同门槛（API 33+）。 */
internal fun couiContourSurfaceEnabled(sdkInt: Int): Boolean =
    sdkInt >= COUI_DIALOG_MATERIAL_MIN_SDK

/**
 * COUI 原厂凝光轮廓材质修饰符：
 * - API 33+ 通过 [CouiContour] 运行时着色器在底色上叠加柔光边缘与内高光；
 * - API 31–32 退化为纯底色（按需叠加 0.5dp 细腻描边）。
 */
@Composable
internal fun Modifier.couiContourSurface(
    shape: Shape,
    surfaceColor: Color,
    cornerRadius: Dp,
    cornerWeight: Float = 2f,
    style: CouiContourStyle = CouiContourStyle.Button,
    dark: Boolean = CouiTheme.isDark,
    emphasized: Boolean = false,
    edgeAlpha: Float = when {
        emphasized -> if (dark) 0.24f else 0.16f
        dark -> 0.14f
        else -> 0.05f
    },
    innerGlowAlpha: Float = when {
        emphasized -> if (dark) 0.10f else 0.06f
        dark -> 0.04f
        else -> 0.02f
    },
    edgeWidthDp: Float = if (emphasized) 2.5f else 2.2f,
    fallbackBorderColor: Color = Color.Transparent,
): Modifier {
    if (Build.VERSION.SDK_INT < COUI_DIALOG_MATERIAL_MIN_SDK || !couiContourSurfaceEnabled(Build.VERSION.SDK_INT)) {
        return this
            .clip(shape)
            .background(surfaceColor)
            .then(
                if (fallbackBorderColor.alpha > 0f) {
                    Modifier.border(0.5.dp, fallbackBorderColor, shape)
                } else {
                    Modifier
                },
            )
    }

    return couiContourSurface33(
        shape = shape,
        surfaceColor = surfaceColor,
        cornerRadius = cornerRadius,
        cornerWeight = cornerWeight,
        style = style,
        edgeAlpha = edgeAlpha,
        innerGlowAlpha = innerGlowAlpha,
        edgeWidthDp = edgeWidthDp,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.couiContourSurface33(
    shape: Shape,
    surfaceColor: Color,
    cornerRadius: Dp,
    cornerWeight: Float,
    style: CouiContourStyle,
    edgeAlpha: Float,
    innerGlowAlpha: Float,
    edgeWidthDp: Float,
): Modifier {
    val contourGlow = LocalCouiContourGlowEnabled.current
    val contour = remember(contourGlow, style) {
        if (contourGlow) CouiContour(floating = false, style = style) else null
    }
    val shaderPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    val density = LocalDensity.current.density
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }

    return this
        .clip(shape)
        .drawWithContent {
            drawRect(surfaceColor)
            if (contour != null && size.width > 0f && size.height > 0f) {
                val resolvedRadius = minOf(cornerRadiusPx, size.minDimension / 2f)
                val resolvedWeight = if (style == CouiContourStyle.Button && size.width == size.height) {
                    2f
                } else {
                    cornerWeight
                }
                shaderPaint.shader = contour.shader(
                    size = size,
                    density = density,
                    radiusPx = resolvedRadius,
                    cornerWeight = resolvedWeight,
                    surfaceColor = Color.Transparent,
                    innerGlowAlpha = innerGlowAlpha,
                    lineAlpha = edgeAlpha,
                    effectAlpha = 1f,
                    lineWidthPx = edgeWidthDp * density,
                )
                drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, shaderPaint)
            }
            drawContent()
        }
}
