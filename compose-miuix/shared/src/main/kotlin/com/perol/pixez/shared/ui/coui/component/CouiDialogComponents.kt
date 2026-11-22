package com.perol.pixez.shared.ui.coui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.effect.CouiContourStyle
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.effect.rememberCouiSpotlight
import com.itos.coui.input.CouiHapticFeedback
import com.itos.coui.input.performCouiHapticFeedback
import com.itos.coui.motion.couiPressMotion
import com.itos.coui.motion.rememberCouiPressMotion
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

private const val CouiProgressAnimMs = 250
private const val CouiIndeterminateAnimMs = 1200
private val CouiDialogMinTouchTarget = 48.dp

/**
 * 弹窗内自绘可点件的统一交互外壳：COUI 按压蒙层 + 凝光光效 + 可选按压缩放 + 触觉反馈。
 * 触控扩边放在视觉裁剪与缩放外侧，保证命中区达标且视觉轮廓不膨胀。
 */
@Composable
internal fun Modifier.couiDialogPressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    shape: Shape? = null,
    minTouchTarget: Dp = 0.dp,
    scaleOnPress: Boolean = false,
    spotlight: SpotlightPreset? = SpotlightPreset.ChipTab,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    LaunchedEffect(interactionSource, view) {
        var down = false
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                if (!down) {
                    down = true
                    view.performCouiHapticFeedback(CouiHapticFeedback.Switch)
                }
            } else if (interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) {
                down = false
            }
        }
    }
    val press = if (scaleOnPress) rememberCouiPressMotion() else null
    val spotlightPair = spotlight?.let { preset ->
        rememberCouiSpotlight(
            interactive = enabled,
            preset = preset,
            dark = CouiTheme.isDark,
        )
    }
    val pressDetector = if (press != null) {
        Modifier.couiPressMotion(press = press, enabled = enabled, cancelOnConsumedMove = true)
    } else {
        Modifier
    }
    val visualSurface = Modifier
        .then(
            if (press != null) {
                Modifier.graphicsLayer {
                    scaleX = press.scale.value
                    scaleY = press.scale.value
                }
            } else {
                Modifier
            },
        )
        .then(if (shape != null) Modifier.clip(shape) else Modifier)
        .couiButtonFeedback(
            dark = CouiTheme.isDark,
            interactionSource = interactionSource,
            enabled = enabled,
            list = false,
        )
        .then(spotlightPair?.first ?: Modifier)
        .then(
            if (spotlightPair != null) {
                Modifier.drawBehind { spotlightPair.second?.invoke(this) }
            } else {
                Modifier
            },
        )

    return this
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
        .then(pressDetector)
        .couiMinimumTouchTarget(minTouchTarget)
        .then(visualSurface)
}

private fun Modifier.couiMinimumTouchTarget(target: Dp): Modifier {
    if (target <= 0.dp) return this
    return layout { measurable, constraints ->
        val targetPx = target.roundToPx()
        val placeable = measurable.measure(constraints)
        val width = placeable.width
            .coerceAtLeast(targetPx)
            .coerceAtMost(constraints.maxWidth)
            .coerceAtLeast(constraints.minWidth)
        val height = placeable.height
            .coerceAtLeast(targetPx)
            .coerceAtMost(constraints.maxHeight)
            .coerceAtLeast(constraints.minHeight)
        layout(width, height) {
            placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
        }
    }
}

/**
 * 与 COUI 原生 `DialogButton(Secondary)` 等价的弹窗辅助按钮：
 * 沿用 COUI 次级按键表面色、胶囊圆角、`CouiDialogDefaults.buttonHeight` 与凝光轮廓材质。
 */
@Composable
internal fun CouiDialogSecondaryButton(
    text: String,
    contentColor: Color,
    hasMaterial: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorSchemeFor(dark)
    val surfaceColor = when {
        hasMaterial && dark -> Color(0x33FFFFFF)
        hasMaterial -> Color(0xA6FFFFFF)
        dark -> Color(0xFF353535)
        else -> Color(0xFFEDEDEB)
    }
    Box(
        modifier = modifier
            .heightIn(min = CouiDialogDefaults.buttonHeight)
            .couiDialogPressable(
                shape = CouiCapsuleShape,
                spotlight = SpotlightPreset.ChipTab,
                scaleOnPress = true,
                minTouchTarget = CouiDialogMinTouchTarget,
                onClick = onClick,
            )
            .couiContourSurface(
                shape = CouiCapsuleShape,
                surfaceColor = surfaceColor,
                cornerRadius = 999.dp,
                cornerWeight = 2f,
                style = CouiContourStyle.Button,
                dark = dark,
                emphasized = false,
                innerGlowAlpha = if (dark) 0.01f else 0.05f,
                fallbackBorderColor = colors.onSurface.copy(alpha = if (dark) 0.14f else 0.08f),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemValue,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * COUI 胶囊水平进度条：确定进度平滑补间填充，[progress] 为 null 时显示连续滑移的不定态光带。
 */
@Composable
internal fun CouiLinearProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorSchemeFor(dark)
    val trackColor = scheme.accent.copy(alpha = if (dark) 0.22f else 0.14f)
    if (progress != null) {
        val animated by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = CouiProgressAnimMs),
            label = "coui-linear-progress",
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(CouiCapsuleShape)
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(CouiCapsuleShape)
                    .background(scheme.accent),
            )
        }
    } else {
        val sweepWidth = 0.36f
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val transition = rememberInfiniteTransition(label = "coui-linear-indeterminate")
        val sweep by transition.animateFloat(
            initialValue = -sweepWidth,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = CouiIndeterminateAnimMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "coui-linear-sweep",
        )
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(CouiCapsuleShape)
                .background(trackColor),
        ) {
            val width = size.width * sweepWidth
            val left = if (isRtl) size.width * (1f - sweep) - width else size.width * sweep
            drawRect(
                color = scheme.accent,
                topLeft = Offset(left, 0f),
                size = Size(width, size.height),
            )
        }
    }
}
