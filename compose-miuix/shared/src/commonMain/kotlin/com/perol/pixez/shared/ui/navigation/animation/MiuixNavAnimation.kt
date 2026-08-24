package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
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
 * 创建 MIUIX / HyperOS 经典纯左右平移视差预测性返回手势（Slide Predictive Back Animatable）。
 *
 * 核心特性：
 * 1. **纯平移无缩小（No Scaling）**：保持 1.0 原始页面缩放比例，不作卡片缩小与圆角变形，符合 MIUIX / iOS 经典侧滑手势。
 * 2. **100% 视口全行程位移**：顶层页面随手势从 0 平移至 containerWidthPx（100% 屏幕宽度滑出），左侧带有立体边缘阴影。
 * 3. **底层页面 30% 视差滑入与柔和遮罩**：底层页面从 -30% 屏幕宽度平滑推进至 0，同时 20% 暗色遮罩随手势平滑消退。
 * 4. **无缝生命周期终结**：手势确认完成时，顶层页面自然完全滑出屏幕右侧（translationX = 100%），出栈切换时绝对零闪现。
 *
 * @param initialBackEvent 手势起始事件。
 * @param containerWidthPx 页面容器当前的物理像素宽度。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixSlidePredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    containerWidthPx: Float,
): PredictiveBackAnimatable {
    return predictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        exitModifier = { progress, edge ->
            val translationX = when (edge) {
                BackEvent.SwipeEdge.LEFT -> progress * containerWidthPx
                BackEvent.SwipeEdge.RIGHT -> -progress * containerWidthPx
                else -> progress * containerWidthPx
            }
            Modifier.graphicsLayer {
                this.translationX = translationX
                shadowElevation = 16f
            }
        },
        enterModifier = { progress, edge ->
            val parallaxRatio = 0.30f
            val translationX = when (edge) {
                BackEvent.SwipeEdge.LEFT -> -(1f - progress) * (containerWidthPx * parallaxRatio)
                BackEvent.SwipeEdge.RIGHT -> (1f - progress) * (containerWidthPx * parallaxRatio)
                else -> -(1f - progress) * (containerWidthPx * parallaxRatio)
            }
            val scrimAlpha = (1f - progress) * 0.20f

            Modifier
                .graphicsLayer {
                    this.translationX = translationX
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
 * 创建 MIUIX / HyperOS 风格的默认页面出入栈平移转场动画（非手势返回与前进导航）。
 *
 * 采用 100% 不透明纯平移（带 320ms HyperOS 曲线），彻底杜绝透明度淡入淡出导致的黑屏闪烁。
 */
fun <C : Any, T : Any> miuixSlideStackAnimation(): StackAnimation<C, T> {
    val slideAnimator: StackAnimator = slide(
        animationSpec = tween(
            durationMillis = 320,
            easing = HyperOSDecelerateEasing,
        ),
    )
    return stackAnimation(slideAnimator)
}
