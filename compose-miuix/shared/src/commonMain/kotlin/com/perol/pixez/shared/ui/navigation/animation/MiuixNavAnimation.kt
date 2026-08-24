package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackEvent

/**
 * Xiaomi HyperOS / MIUIX 核心动效曲线：
 * 具有强阻尼与迅速启动特征的非线性贝塞尔曲线。
 */
val HyperOSDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * 创建 MIUIX / HyperOS 风格的预测性返回手势动画控制器（Predictive Back Animatable）。
 *
 * 核心特性：
 * 1. **非线性物理阻尼**：手势滑动过程中对进度进行非线性拟合，提供跟手且具有物理质量的阻尼感。
 * 2. **视口自适应位移**：根据容器实际物理宽度 [containerWidthPx] 动态计算滑移距离，避免固定绝对像素导致的微小位移问题。
 * 3. **硬件级圆角倒角与立体阴影**：顶层卡片随手势进度从 0dp 演进至 28dp 标准圆角并投射多层景深阴影。
 * 4. **底页视差深度与暗色遮罩（Scrim）**：底层预览页面从 93% 深度微缩与轻微左偏视差推进至全屏，并随手势逐渐消退 28% 暗色遮罩，突出操作焦点。
 *
 * @param initialBackEvent 手势起始事件。
 * @param density 屏幕密度，用于精确换算 dp / px。
 * @param containerWidthPx 页面容器当前的物理像素宽度。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixPredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    density: Density,
    containerWidthPx: Float,
): PredictiveBackAnimatable {
    val maxHorizontalShift = (containerWidthPx * 0.18f).coerceAtLeast(density.density * 60f)
    val maxParallaxShift = (containerWidthPx * 0.08f).coerceAtLeast(density.density * 28f)

    return predictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        exitModifier = { progress, edge ->
            val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
            val scale = 1f - (eased * 0.10f)
            val translationX = when (edge) {
                BackEvent.SwipeEdge.LEFT -> eased * maxHorizontalShift
                BackEvent.SwipeEdge.RIGHT -> -eased * maxHorizontalShift
                else -> 0f
            }
            val cornerRadiusDp = (eased * 28f).dp
            val elevationDp = (eased * 20f).dp

            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
                shape = RoundedCornerShape(cornerRadiusDp)
                clip = true
                shadowElevation = with(density) { elevationDp.toPx() }
            }
        },
        enterModifier = { progress, edge ->
            val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
            val scale = 0.93f + (eased * 0.07f)
            val translationX = when (edge) {
                BackEvent.SwipeEdge.LEFT -> -(1f - eased) * maxParallaxShift
                BackEvent.SwipeEdge.RIGHT -> (1f - eased) * maxParallaxShift
                else -> 0f
            }
            val scrimAlpha = (1f - eased) * 0.28f
            val contentAlpha = 0.85f + (eased * 0.15f)

            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.translationX = translationX
                    alpha = contentAlpha
                }
                .drawWithContent {
                    drawContent()
                    if (scrimAlpha > 0.001f) {
                        drawRect(Color.Black.copy(alpha = scrimAlpha))
                    }
                }
        },
    )
}

/**
 * 创建 MIUIX / HyperOS 风格的默认页面出入栈转场动画（非手势返回与前进导航）。
 *
 * 采用 HyperOS 2.0 连续空间位移与淡入淡出曲线，转场时长 320ms。
 */
fun <C : Any, T : Any> miuixStackAnimation(): StackAnimation<C, T> {
    val slideAnimator: StackAnimator = slide(
        animationSpec = tween(
            durationMillis = 320,
            easing = HyperOSDecelerateEasing,
        ),
    )
    val fadeAnimator: StackAnimator = fade(
        animationSpec = tween(
            durationMillis = 240,
            easing = HyperOSDecelerateEasing,
        ),
    )

    return stackAnimation(slideAnimator + fadeAnimator)
}
