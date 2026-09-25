package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.FaultyDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackEvent
import com.perol.pixez.shared.ui.navigation.RootComponent

/**
 * Xiaomi HyperOS / MIUIX 核心动效曲线：
 * 具有强阻尼与迅速启动特征的非线性贝塞尔曲线。
 */
val HyperOSDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * 构造 MIUIX / HyperOS「卡片展开」出入栈转场。
 *
 * 动画器按「本次转场涉及的两个页面配置」解析来源卡片矩形：
 * - push（[Direction.ENTER_FRONT]）：以新入场的详情页配置为键，取出对应列表卡片矩形。
 * - pop（[Direction.EXIT_FRONT]）：以即将离场的详情页配置为键，取出对应列表卡片矩形。
 *
 * 无法解析到卡片矩形时（分享链接直达、进程重建恢复、由非列表入口进入），
 * 转场自动退化为无变换切换，避免出现无依据的缩放跳动。
 *
 * @param registry 卡片几何信息源，由 [LocalSharedBoundsRegistry] 提供。
 * @param containerBounds 页面容器自身的窗口矩形。
 * @param containerCornerRadius 设备屏幕物理圆角。
 * @return 可直接交给 Children(animation = ...) 使用的 [StackAnimation]。
 */
@OptIn(ExperimentalDecomposeApi::class, FaultyDecomposeApi::class)
fun miuixCardExpandStackAnimation(
    registry: SharedBoundsRegistry,
    containerBounds: Rect,
    containerCornerRadius: Dp,
): StackAnimation<RootComponent.Config, RootComponent.Child> =
    stackAnimation { initialChild, targetChild, direction ->
        // 转场发生的瞬间解析来源卡片矩形；解析不到则由动画器内部退化为经典平移。
        val sourceBounds = resolveTransitionIllustId(direction, initialChild, targetChild)
            ?.let(registry::get)
        cardExpandStackAnimator(
            sourceBounds = sourceBounds,
            containerBounds = containerBounds,
            containerCornerRadius = containerCornerRadius,
        )
    }

/**
 * 解析一次转场所对应的来源卡片作品 ID。
 *
 * @param direction 转场方向。
 * @param initialChild 转场前的栈顶页面，push 时为 null。
 * @param targetChild 转场后的栈顶页面。
 * @return 卡片作品 ID；本次转场与作品详情页无关时返回 null。
 */
private fun resolveTransitionIllustId(
    direction: Direction,
    initialChild: Child.Created<RootComponent.Config, RootComponent.Child>?,
    targetChild: Child.Created<RootComponent.Config, RootComponent.Child>,
): Int? = when (direction) {
    // push：新页面入场所依据的卡片，即 targetChild 自身。
    Direction.ENTER_FRONT -> (targetChild.configuration as? RootComponent.Config.IllustDetail)?.illustId
    // pop：即将离场页面所依据的卡片，即 initialChild 自身。
    Direction.EXIT_FRONT -> (initialChild?.configuration as? RootComponent.Config.IllustDetail)?.illustId
    else -> null
}

/**
 * 创建 MIUIX / HyperOS「卡片收回」预测性返回动画。
 *
 * 手势拖拽期间，顶层详情页随手指进度从整屏收缩回列表卡片位置，
 * 圆角同步由 0 渐变到设备屏幕物理圆角，并在收缩态投出边界阴影；
 * 底层页面保持静止仅叠加消退遮罩，让「收回卡片」的纵深关系清晰可读。
 *
 * 未登记卡片矩形时回退为纯左右平移，保留既有 MIUIX 侧滑手感。
 *
 * @param initialBackEvent 手势起始事件。
 * @param registry 卡片几何信息源，用于取出手势来源页对应的卡片矩形。
 * @param illustId 当前栈顶作品详情页的作品 ID，非作品详情页时为 null。
 * @param containerWidthPx 页面容器的物理像素宽度，用于回退平移方案。
 * @param containerBounds 页面容器窗口矩形，用于归一化卡片几何。
 * @param deviceCornerRadius 设备屏幕硬件物理圆角半径。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixCardExpandPredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    registry: SharedBoundsRegistry,
    illustId: Int?,
    containerWidthPx: Float,
    containerBounds: Rect,
    deviceCornerRadius: Dp = 0.dp,
): PredictiveBackAnimatable {
    val sourceBounds = illustId?.let(registry::get)
        ?: return miuixSlidePredictiveBackAnimatable(
            initialBackEvent = initialBackEvent,
            containerWidthPx = containerWidthPx,
            deviceCornerRadius = deviceCornerRadius,
        )

    return predictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        exitModifier = { progress, _ ->
            Modifier.cardExpandLayer(
                expansion = predictiveBackCardExpandExpansion(progress = progress),
                sourceBounds = sourceBounds,
                containerBounds = containerBounds,
                containerCornerRadius = deviceCornerRadius,
            )
        },
        enterModifier = { progress, _ ->
            Modifier.cardExpandScrim(expansion = predictiveBackCardExpandExpansion(progress = progress))
        },
    )
}

/**
 * 创建 MIUIX / HyperOS 经典纯左右平移视差预测性返回手势（Slide Predictive Back Animatable）。
 *
 * 作为卡片展开方案的兜底：手势力来源页没有卡片几何信息（分享链接直达、进程重建恢复）时使用。
 *
 * 核心特性：
 * 1. **纯平移无缩小（No Scaling）**：保持 1.0 原始页面缩放比例，不作卡片缩小与圆角变形。
 * 2. **硬件级屏幕物理圆角自适应**：顶层滑出页面在拖拽时贴合设备屏幕圆角 [deviceCornerRadius]。
 * 3. **100% 视口全行程位移**：顶层页面随手势从 0 平移至 containerWidthPx（100% 屏幕宽度滑出）。
 * 4. **底层页面 30% 视差滑入与柔和遮罩**：底层页面从 -30% 屏幕宽度平滑推进至 0。
 * 5. **无缝生命周期终结**：手势确认完成时顶层页面自然完全滑出屏幕右侧，出栈切换时零闪现。
 *
 * @param initialBackEvent 手势起始事件。
 * @param containerWidthPx 页面容器当前的物理像素宽度。
 * @param deviceCornerRadius 设备屏幕硬件物理圆角半径。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixSlidePredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    containerWidthPx: Float,
    deviceCornerRadius: Dp = 0.dp,
): PredictiveBackAnimatable {
    val shape = if (deviceCornerRadius > 0.dp) RoundedCornerShape(deviceCornerRadius) else null

    return predictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        exitModifier = { progress, _ ->
            val translationX = progress * containerWidthPx
            Modifier.graphicsLayer {
                this.translationX = translationX
                shadowElevation = 16f
                if (shape != null) {
                    this.shape = shape
                    this.clip = true
                }
            }
        },
        enterModifier = { progress, _ ->
            Modifier
                .graphicsLayer {
                    this.translationX = -(1f - progress) * (containerWidthPx * 0.30f)
                }
                .cardExpandScrim(
                    expansion = predictiveBackCardExpandExpansion(progress = progress),
                    maxAlpha = 0.20f,
                )
        },
    )
}
