package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
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
 * 前层不是作品详情页、或解析不到卡片矩形时（分享链接直达、进程重建恢复、由非列表入口进入），
 * 转场回退为逐层复刻 miuix-nav `NavTransitions.MiuixDefault` 的默认全宽侧滑，
 * 保证除「列表 -> 作品详情」外的所有页面都与 Miuix 官方导航默认行为一致。
 *
 * @param registry 卡片几何信息源，由 [LocalSharedBoundsRegistry] 提供。
 * @param containerBounds 页面容器自身的窗口矩形。
 * @param containerCornerRadius 设备屏幕物理圆角。
 * @return 可直接交给 Children(animation = ...) 使用的 [StackAnimation]。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixCardExpandStackAnimation(
    registry: SharedBoundsRegistry,
    containerBounds: Rect,
    containerCornerRadius: Dp,
): StackAnimation<RootComponent.Config, RootComponent.Child> =
    stackAnimation { child ->
        val rawIllustId = (child.configuration as? RootComponent.Config.IllustDetail)?.illustId
        cardExpandStackAnimator(
            rawIllustId = rawIllustId,
            containerBounds = containerBounds,
            containerCornerRadius = containerCornerRadius,
            registry = registry,
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
    illustId: Long?,
    containerWidthPx: Float,
    containerBounds: Rect,
    deviceCornerRadius: Dp = 0.dp,
): PredictiveBackAnimatable {
    val anchor = registry.resolveAnchor(
        // 与出栈动画同一套候选顺序：当前展示的作品 → 本次打开的作品。
        illustIdCandidates = listOf(registry.resolveEffectiveIllustId(illustId), illustId),
        containerBounds = containerBounds,
    ) ?: run {
        registry.updateTransitionState(null, 0f)
        return miuixSlidePredictiveBackAnimatable(
            initialBackEvent = initialBackEvent,
            containerWidthPx = containerWidthPx,
            deviceCornerRadius = deviceCornerRadius,
            registry = registry,
        )
    }
    val sourceCard = anchor.card

    // 进度按弹簧平滑驱动：系统进度派发稀疏/跳变（澎湃 / MIUI）时页面不再瞬间跳位。
    return MiuixSmoothedPredictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        getExitModifier = { progress, _ ->
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            registry.updateTransitionState(
                illustId = anchor.illustId,
                expansion = expansion,
                sourceBounds = sourceCard.rect,
                containerBounds = containerBounds,
            )
            Modifier.cardExpandLayer(
                expansion = expansion,
                sourceBounds = sourceCard.rect,
                cardCornerRadiusDp = sourceCard.cornerRadiusDp,
                containerBounds = containerBounds,
                containerCornerRadius = deviceCornerRadius,
            )
        },
        getEnterModifier = { progress, _ ->
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            Modifier.cardExpandScrim(
                alpha = cardExpandScrimAlpha(expansion),
                expansion = expansion,
                sourceBounds = sourceCard.rect,
                containerBounds = containerBounds,
                containerCornerRadius = deviceCornerRadius,
            )
        },
    )
}

/**
 * 创建 Miuix 默认全宽平移预测性返回手势（MiuixDefault Predictive Back Animatable）。
 *
 * 作为卡片展开方案的兜底：手势力来源页没有卡片几何信息（非作品详情页、分享链接直达、
 * 进程重建恢复）时使用。映射逐层复刻 miuix-nav `NavTransitions.MiuixDefault` 手势路径
 * （与弹簧收敛路径共用同一 d -> visual 关系）：
 *
 * 1. **顶层页面全行程平移**：随手势从 0 平移至 containerWidthPx（100% 屏幕宽度滑出），
 *    滑动中贴合设备屏幕物理圆角 [deviceCornerRadius] 裁切，无边框阴影。
 * 2. **被露出页面 25% 视差与轻微淡出**：从 -25% 屏幕宽度与 alpha 0.9 随手势还原至 0 与 1.0。
 * 3. **线性暗色遮罩**：被露出页面随覆盖进度叠加最深 0.5 的遮罩，手势结束完全消退。
 *
 * @param initialBackEvent 手势起始事件。
 * @param containerWidthPx 页面容器当前的物理像素宽度。
 * @param deviceCornerRadius 设备屏幕硬件物理圆角半径。
 * @param registry 卡片几何信息源，用于同步底层列表视差位移。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixSlidePredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    containerWidthPx: Float,
    deviceCornerRadius: Dp = 0.dp,
    registry: SharedBoundsRegistry? = null,
): PredictiveBackAnimatable {
    return MiuixSmoothedPredictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        getExitModifier = { progress, _ ->
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = true,
                fraction = progress,
                widthPx = containerWidthPx,
                cornerRadius = deviceCornerRadius,
                registry = registry,
            )
        },
        getEnterModifier = { progress, _ ->
            val coveredFraction = (1f - progress).coerceIn(0f, 1f)
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = false,
                fraction = coveredFraction,
                widthPx = containerWidthPx,
                cornerRadius = deviceCornerRadius,
                registry = registry,
            )
        },
    )
}
