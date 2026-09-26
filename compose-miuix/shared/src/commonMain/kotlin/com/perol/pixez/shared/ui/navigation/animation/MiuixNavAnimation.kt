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
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackEvent
import com.perol.pixez.shared.ui.navigation.RootComponent

/**
 * Xiaomi HyperOS / MIUIX 核心动效曲线：
 * 具有强阻尼与迅速启动特征的非线性贝塞尔曲线。
 */
val HyperOSDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/** 平移兜底方案收缩态下的遮罩最大不透明度（略深于卡片展开方案，补偿缺失的缩放纵深线索）。 */
private const val SLIDE_FALLBACK_SCRIM_ALPHA = 0.20f

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
    stackAnimation { child, otherChild, direction ->
        // 仅当本次转场的前层（入栈或出栈的栈顶页面）为作品详情页时，才使用对应卡片矩形执行展开/收缩；
        // 出栈（EXIT_FRONT / ENTER_BACK）时优先取详情页内实际滑切到的作品 ID。
        val isPop = direction == Direction.EXIT_FRONT || direction == Direction.ENTER_BACK
        val frontConfig = if (direction.isFront) child.configuration else otherChild.configuration
        val rawIllustId = (frontConfig as? RootComponent.Config.IllustDetail)?.illustId
        val effectiveIllustId = if (isPop) registry.resolveEffectiveIllustId(rawIllustId) else rawIllustId
        val sourceBounds = effectiveIllustId?.let { registry.getVisibleInContainer(it, containerBounds) }

        cardExpandStackAnimator(
            sourceBounds = sourceBounds,
            containerBounds = containerBounds,
            containerCornerRadius = containerCornerRadius,
            isTopLayer = direction.isFront,
            registry = registry,
            illustId = effectiveIllustId,
        )
    }

/**
 * 创建 MIUIX / HyperOS「卡片收回」预测性返回动画。
 *
 * 手势拖拽期间，顶层详情页随手指进度从整屏收缩回列表卡片位置，
 * 圆角同步由设备屏幕物理圆角渐变到卡片圆角，并在收缩态投出边界阴影；
 * 底层页面随手势进度由 0.96 缩放还原至 1.0 并消退遮罩，让「收回卡片」的纵深关系清晰可读。
 *
 * 以下情况没有可用的来源卡片几何，一律显式回退为经典纯左右平移：
 * 1. 当前栈顶不是作品详情页（[illustId] 为 null）；
 * 2. 该详情页由分享链接直达、进程重建恢复进入，或用户在详情页内滑动切换到了不在当前列表的作品；
 * 3. 卡片矩形随列表滚出可视区超过阈值或已被注销。
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
    val effectiveIllustId = registry.resolveEffectiveIllustId(illustId)
    val sourceBounds = resolveGestureSourceBounds(
        illustId = effectiveIllustId,
        registry = registry,
        containerBounds = containerBounds,
    ) ?: run {
        registry.updateTransitionState(null, 0f)
        return miuixSlidePredictiveBackAnimatable(
            initialBackEvent = initialBackEvent,
            containerWidthPx = containerWidthPx,
            deviceCornerRadius = deviceCornerRadius,
        )
    }

    return predictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        exitModifier = { progress, _ ->
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            registry.updateTransitionState(effectiveIllustId, expansion)
            Modifier.cardExpandLayer(
                expansion = expansion,
                sourceBounds = sourceBounds,
                containerBounds = containerBounds,
                containerCornerRadius = deviceCornerRadius,
            )
        },
        enterModifier = { progress, _ ->
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            Modifier.cardExpandScrim(
                alpha = cardExpandScrimAlpha(expansion),
                expansion = expansion,
                sourceBounds = sourceBounds,
                containerBounds = containerBounds,
                containerCornerRadius = deviceCornerRadius,
            )
        },
    )
}

/**
 * 解析预测性返回手势可用的来源卡片矩形。
 *
 * 这是「有来源卡片 / 无来源卡片」的唯一判定点：返回 null 即代表本次手势必须
 * 走经典侧滑兜底。
 *
 * @param illustId 当前栈顶作品详情页的作品 ID，非作品详情页时为 null。
 * @param registry 卡片几何信息源。
 * @param containerBounds 页面容器窗口矩形。
 * @return 可用的卡片窗口矩形；没有来源卡片信息或已滚出容器可视范围时返回 null。
 */
internal fun resolveGestureSourceBounds(
    illustId: Int?,
    registry: SharedBoundsRegistry,
    containerBounds: Rect = Rect.Zero,
): Rect? {
    val id = illustId ?: return null
    val bounds = registry.getVisibleInContainer(id, containerBounds) ?: return null
    return bounds.takeIf { it.width > 0f && it.height > 0f }
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
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            Modifier
                .graphicsLayer {
                    this.translationX = -(1f - progress) * (containerWidthPx * 0.30f)
                }
                .cardExpandScrim(
                    alpha = cardExpandScrimAlpha(
                        expansion = expansion,
                        maxAlpha = SLIDE_FALLBACK_SCRIM_ALPHA,
                    ),
                    expansion = expansion,
                    containerCornerRadius = deviceCornerRadius,
                )
        },
    )
}
