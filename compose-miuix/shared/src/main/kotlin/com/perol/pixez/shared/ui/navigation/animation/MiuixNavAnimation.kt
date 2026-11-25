package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.essenty.backhandler.BackEvent
import com.perol.pixez.shared.ui.navigation.Config
import com.perol.pixez.shared.ui.navigation.isModalRoute
import com.perol.pixez.shared.ui.navigation.toTransitionTarget

/**
 * Xiaomi HyperOS / MIUIX 核心动效曲线：
 * 具有强阻尼与迅速启动特征的非线性贝塞尔曲线。
 */
val HyperOSDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * 构造 MIUIX / HyperOS「卡片展开」出入栈转场（可打断版）。
 *
 * 转场调度为自研 [SeekableStackAnimation]：push 转场中反向返回时两层方向互换、
 * 从当前位置连续转向，不再排队重播；其余转场中事件保持排队语义。
 * 帧渲染复用 [cardExpandFrame]，动画器按「本次转场涉及的两个页面配置」解析来源卡片矩形：
 * - push（ENTER_FRONT）：以新入场的详情页配置为键，取出对应列表卡片矩形。
 * - pop（EXIT_FRONT）：以即将离场的详情页配置为键，取出对应列表卡片矩形。
 *
 * 前层不是作品详情页、或解析不到卡片矩形时（分享链接直达、进程重建恢复、由非列表入口进入），
 * 转场回退为逐层复刻 miuix-nav `NavTransitions.MiuixDefault` 的默认全宽侧滑，
 * 保证除「列表 -> 作品详情」外的所有页面都与 Miuix 官方导航默认行为一致。
 *
 * [containerGeometry] 为页面容器几何的即时读取源：渲染闭包可能被长期持有，
 * 容器几何若以快照捕获会被过期缓存固化（冷启动首帧 bounds 尚未回填时创建的闭包
 * 会一直持有 `Rect.Zero`），导致列表层锚点解析永远失败；因此几何必须每帧即时读取。
 *
 * @param registry 卡片几何信息源，由 [LocalSharedBoundsRegistry] 提供。
 * @param containerGeometry 页面容器几何的即时读取源。
 * @param coverParallax 底层页是否做视差位移；ColorOS 转场为覆盖语义，传 false 关闭。
 * @return 可直接交给 Children(animation = ...) 使用的 [StackAnimation]。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun <C : Any, T : Any> miuixSeekableStackAnimation(
    registry: SharedBoundsRegistry,
    containerGeometry: () -> PageContainerGeometry,
    coverParallax: Boolean = true,
    isLightBackground: Boolean = false,
): StackAnimation<C, T> =
    SeekableStackAnimation { child, frontChild ->
        val rawTarget = (child.configuration as? Config)?.toTransitionTarget()
        val isFrontModal = (frontChild.configuration as? Config)?.isModalRoute() == true
        cardExpandFrame(
            rawTarget = rawTarget,
            containerGeometry = containerGeometry,
            registry = registry,
            coverParallax = coverParallax,
            isModal = isFrontModal,
            isLightBackground = isLightBackground,
        )
    }

/** 通用卡片收回预测性返回动画（支持插画详情与小说阅读器）。 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixCardExpandPredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    registry: SharedBoundsRegistry,
    target: TransitionTarget?,
    containerGeometry: () -> PageContainerGeometry,
    coverParallax: Boolean = true,
    isLightBackground: Boolean = false,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): PredictiveBackAnimatable {
    val geometry = containerGeometry()
    val anchor = registry.resolveTargetAnchor(
        targetCandidates = listOf(registry.resolveEffectiveTarget(target), target),
        containerBounds = geometry.bounds,
    ) ?: run {
        registry.updateTargetTransitionState(null, 0f)
        return miuixSlidePredictiveBackAnimatable(
            initialBackEvent = initialBackEvent,
            containerGeometry = containerGeometry,
            registry = registry,
            coverParallax = coverParallax,
            isLightBackground = isLightBackground,
            layoutDirection = layoutDirection,
        )
    }
    val sourceCard = anchor.card

    // 进度按弹簧平滑驱动：系统进度派发稀疏/跳变（澎湃 / MIUI）时页面不再瞬间跳位；
    // 通过 lazy 绘制期延迟求值，保证手势期间零重组且模糊 RenderEffect 缓存不被重建。
    return MiuixSmoothedPredictiveBackAnimatable.lazy(
        initialBackEvent = initialBackEvent,
        buildExitModifier = { progressProvider, _ ->
            Modifier.cardExpandLayer(
                expansionProvider = { predictiveBackCardExpandExpansion(progress = progressProvider()) },
                sourceBounds = sourceCard.rect,
                cardCornerRadiusDp = sourceCard.cornerRadiusDp,
                containerBoundsProvider = { containerGeometry().bounds },
                containerCornerRadiusProvider = { containerGeometry().cornerRadius },
                safeViewportProvider = { sourceCard.resolveSafeViewport() },
            )
        },
        buildEnterModifier = { progressProvider, _ ->
            Modifier
                .navigationDepthBlur {
                    val expansion = predictiveBackCardExpandExpansion(progress = progressProvider())
                    resolveNavigationBlurBlendFrame(
                        progress = expansion,
                        isLightBackground = isLightBackground,
                    )
                }
                .cardExpandScrim(
                    alphaProvider = {
                        val expansion = predictiveBackCardExpandExpansion(progress = progressProvider())
                        cardExpandScrimAlpha(expansion)
                    },
                )
        },
        onProgressChanged = { progress ->
            val current = containerGeometry()
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            registry.updateTargetTransitionState(
                target = anchor.target,
                expansion = expansion,
                sourceBounds = sourceCard.rect,
                containerBounds = current.bounds,
                isExiting = true,
            )
        },
    )
}

/**
 * 创建 MIUIX / HyperOS「卡片收回」预测性返回动画。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixCardExpandPredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    registry: SharedBoundsRegistry,
    illustId: Long?,
    containerGeometry: () -> PageContainerGeometry,
    coverParallax: Boolean = true,
    isLightBackground: Boolean = false,
): PredictiveBackAnimatable = miuixCardExpandPredictiveBackAnimatable(
    initialBackEvent = initialBackEvent,
    registry = registry,
    target = illustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) },
    containerGeometry = containerGeometry,
    coverParallax = coverParallax,
    isLightBackground = isLightBackground,
)

/**
 * 创建 Miuix 默认全宽平移预测性返回手势（MiuixDefault Predictive Back Animatable）。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixSlidePredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    containerGeometry: () -> PageContainerGeometry,
    registry: SharedBoundsRegistry? = null,
    coverParallax: Boolean = true,
    isLightBackground: Boolean = false,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): PredictiveBackAnimatable {
    return MiuixSmoothedPredictiveBackAnimatable.lazy(
        initialBackEvent = initialBackEvent,
        buildExitModifier = { progressProvider, _ ->
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = true,
                fractionProvider = progressProvider,
                widthPxProvider = { containerGeometry().widthPx },
                cornerShapeProvider = {
                    val r = containerGeometry().cornerRadius
                    if (r > 0.dp) RoundedCornerShape(r) else null
                },
                registry = registry,
                isLightBackground = isLightBackground,
                layoutDirection = layoutDirection,
            )
        },
        buildEnterModifier = { progressProvider, _ ->
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = false,
                fractionProvider = { (1f - progressProvider()).coerceIn(0f, 1f) },
                widthPxProvider = { containerGeometry().widthPx },
                cornerShapeProvider = {
                    val r = containerGeometry().cornerRadius
                    if (r > 0.dp) RoundedCornerShape(r) else null
                },
                registry = registry,
                parallaxFraction = if (coverParallax) MIUIX_DEFAULT_COVER_PARALLAX_FRACTION else 0f,
                isLightBackground = isLightBackground,
                layoutDirection = layoutDirection,
            )
        },
    )
}

/** 模态页自顶向下预测性返回 / 顶部拖拽解散动画器（底层页保持静止、渐褪模糊与遮罩）。 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixModalPredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    containerGeometry: () -> PageContainerGeometry,
    isLightBackground: Boolean = false,
): PredictiveBackAnimatable {
    val topCorner = containerGeometry().cornerRadius.coerceAtLeast(24.dp)
    return MiuixSmoothedPredictiveBackAnimatable.lazy(
        initialBackEvent = initialBackEvent,
        buildExitModifier = { progressProvider, _ ->
            Modifier.miuixModalLayer(
                isTopLayer = true,
                fractionProvider = progressProvider,
                heightPxProvider = { resolveModalLayerHeightPx(containerGeometry()) },
                topCornerRadius = topCorner,
                isLightBackground = isLightBackground,
            )
        },
        buildEnterModifier = { progressProvider, _ ->
            Modifier.miuixModalLayer(
                isTopLayer = false,
                fractionProvider = { (1f - progressProvider()).coerceIn(0f, 1f) },
                heightPxProvider = { resolveModalLayerHeightPx(containerGeometry()) },
                topCornerRadius = topCorner,
                isLightBackground = isLightBackground,
            )
        },
    )
}
