package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator
import com.arkivanov.essenty.backhandler.BackEvent
import com.perol.pixez.shared.ui.navigation.RootComponent

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
        exitModifier = { progress, _ ->
            val translationX = progress * containerWidthPx
            Modifier.graphicsLayer {
                this.translationX = translationX
                shadowElevation = 16f
            }
        },
        enterModifier = { progress, _ ->
            val parallaxRatio = 0.30f
            val translationX = -(1f - progress) * (containerWidthPx * parallaxRatio)
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
 * 底部主标签顺序表，用于判断标签切换时的前进/后退物理方向。
 */
private val MAIN_TAB_ORDER = listOf(
    RootComponent.MainTab.Hello,
    RootComponent.MainTab.Search,
    RootComponent.MainTab.Ranking,
    RootComponent.MainTab.New,
    RootComponent.MainTab.Spotlight,
)

/**
 * 创建 MIUIX / HyperOS 风格的默认页面出入栈平移转场动画。
 *
 * 特性：
 * 1. 一级主标签切换：智能感知目标标签物理顺序，从左往右切时向右滑出，从右往左切时向左滑出。
 * 2. 二级详情页面推进（Push）：新页面从右侧滑入（100% 不透明），旧页面向左滑出。
 * 3. 二级详情页面回退（Pop）：旧页面向右滑出，新页面自左侧滑回。
 */
@OptIn(com.arkivanov.decompose.FaultyDecomposeApi::class)
fun miuixSlideStackAnimation(): StackAnimation<RootComponent.Config, RootComponent.Child> {
    val forwardSlide: StackAnimator = slide(
        animationSpec = tween(
            durationMillis = 320,
            easing = HyperOSDecelerateEasing,
        ),
    )

    val backwardSlide: StackAnimator = stackAnimator(
        animationSpec = tween(
            durationMillis = 320,
            easing = HyperOSDecelerateEasing,
        ),
    ) { factor, direction, content ->
        val translationXFactor = when (direction) {
            Direction.ENTER_FRONT -> -factor
            Direction.EXIT_BACK -> factor
            Direction.ENTER_BACK -> factor
            Direction.EXIT_FRONT -> -factor
        }
        content(
            Modifier.graphicsLayer {
                translationX = translationXFactor * size.width
            },
        )
    }

    return stackAnimation { child, otherChild, direction ->
        val childInstance = child.instance
        val otherInstance = otherChild.instance

        if (childInstance is RootComponent.Child.Main && otherInstance is RootComponent.Child.Main) {
            val isEnter = direction == Direction.ENTER_FRONT || direction == Direction.ENTER_BACK
            val targetIdx = MAIN_TAB_ORDER.indexOf(if (isEnter) childInstance.tab else otherInstance.tab)
            val sourceIdx = MAIN_TAB_ORDER.indexOf(if (isEnter) otherInstance.tab else childInstance.tab)
            if (targetIdx < sourceIdx) {
                backwardSlide
            } else {
                forwardSlide
            }
        } else {
            forwardSlide
        }
    }
}
