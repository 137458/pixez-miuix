package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator

/**
 * MIUIX / HyperOS 二级页面转场的统一时长（毫秒）。
 */
private const val TRANSITION_DURATION_MILLIS = 340

/** 无来源卡片时顶层入场/退场页面的水平视差位移比例（相对于容器宽度）。 */
private const val FALLBACK_FRONT_SLIDE_FRACTION = 0.28f

/** 无来源卡片时底层页面退后/回前的水平视差位移比例（相对于容器宽度）。 */
private const val FALLBACK_BACK_PARALLAX_FRACTION = 0.10f

/**
 * 构造 MIUIX / HyperOS 统一的二级页面转场动画器。
 *
 * 顶层页面被视为一张从作品列表卡片「生长」出来的容器：打开详情页时按卡片矩形
 * 的尺寸与位置等比展开并平滑淡入，关闭时反向收缩归位；圆角由卡片圆角
 * 渐变到设备屏幕物理圆角 [containerCornerRadius]，形成连贯的空间连续性。
 *
 * [sourceBounds] 为 null 时（画师页、设置页这类本就没有来源卡片的二级页面，
 * 以及分享链接直达、进程重建恢复等入口）采用 HyperOS 视差侧滑 + 透明度渐变 + 纵深遮罩，
 * 消除生硬的整屏线性硬推。
 *
 * @param sourceBounds 发起转场的卡片窗口矩形，为 null 时使用视差侧滑 + 淡入淡出兜底。
 * @param containerBounds 页面容器自身的窗口矩形，用于归一化卡片几何。
 * @param containerCornerRadius 设备屏幕物理圆角，收缩态下用于裁切顶层页面。
 * @return 可直接交给 stackAnimation 使用的 [StackAnimator]。
 */
internal fun cardExpandStackAnimator(
    sourceBounds: Rect?,
    containerBounds: Rect,
    containerCornerRadius: Dp,
    isTopLayer: Boolean,
): StackAnimator {
    val duration: FiniteAnimationSpec<Float> = tween(
        durationMillis = TRANSITION_DURATION_MILLIS,
        easing = HyperOSDecelerateEasing,
    )
    if (sourceBounds == null) {
        return stackAnimator(animationSpec = duration) { factor, direction, content ->
            val frame = resolveCardExpandFrame(direction = direction, factor = factor, isTopLayer = isTopLayer)
            val widthPx = containerBounds.width.takeIf { it > 0f } ?: 1080f
            content(
                if (frame.isTopLayer) {
                    val progress = frame.expansion
                    val offsetFraction = 1f - progress
                    val scale = 0.96f + 0.04f * progress
                    Modifier.graphicsLayer {
                        translationX = widthPx * FALLBACK_FRONT_SLIDE_FRACTION * offsetFraction
                        alpha = progress.coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    }
                } else {
                    val progress = frame.expansion
                    Modifier
                        .graphicsLayer {
                            translationX = -widthPx * FALLBACK_BACK_PARALLAX_FRACTION * progress
                        }
                        .cardExpandScrim(
                            alpha = cardExpandScrimAlpha(progress),
                            expansion = progress,
                        )
                },
            )
        }
    }

    return stackAnimator(animationSpec = duration) { factor, direction, content ->
        val frame = resolveCardExpandFrame(direction = direction, factor = factor, isTopLayer = isTopLayer)
        content(
            if (frame.isTopLayer) {
                Modifier.cardExpandLayer(
                    expansion = frame.expansion,
                    sourceBounds = sourceBounds,
                    containerBounds = containerBounds,
                    containerCornerRadius = containerCornerRadius,
                )
            } else {
                Modifier.cardExpandScrim(
                    alpha = cardExpandScrimAlpha(frame.expansion),
                    expansion = frame.expansion,
                )
            },
        )
    }
}
