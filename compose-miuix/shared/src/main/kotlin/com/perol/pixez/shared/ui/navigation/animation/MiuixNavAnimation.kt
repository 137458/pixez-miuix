package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.essenty.backhandler.BackEvent
import com.perol.pixez.shared.ui.navigation.Config
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
): StackAnimation<C, T> =
    SeekableStackAnimation { child ->
        // 路由参数只在 configuration 上，同时覆盖插画详情页与小说阅读器两种卡片展开目标。
        val rawTarget = (child.configuration as? Config)?.toTransitionTarget()
        cardExpandFrame(
            rawTarget = rawTarget,
            containerGeometry = containerGeometry,
            registry = registry,
            coverParallax = coverParallax,
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
        )
    }
    val sourceCard = anchor.card

    // 进度按弹簧平滑驱动：系统进度派发稀疏/跳变（澎湃 / MIUI）时页面不再瞬间跳位。
    return MiuixSmoothedPredictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        getExitModifier = { progress, _ ->
            val current = containerGeometry()
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            registry.updateTargetTransitionState(
                target = anchor.target,
                expansion = expansion,
                sourceBounds = sourceCard.rect,
                containerBounds = current.bounds,
            )
            Modifier.cardExpandLayer(
                expansion = expansion,
                sourceBounds = sourceCard.rect,
                cardCornerRadiusDp = sourceCard.cornerRadiusDp,
                containerBounds = current.bounds,
                containerCornerRadius = current.cornerRadius,
            )
        },
        getEnterModifier = { progress, _ ->
            val expansion = predictiveBackCardExpandExpansion(progress = progress)
            Modifier.cardExpandScrim(alpha = cardExpandScrimAlpha(expansion))
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
): PredictiveBackAnimatable = miuixCardExpandPredictiveBackAnimatable(
    initialBackEvent = initialBackEvent,
    registry = registry,
    target = illustId?.let { TransitionTarget(TransitionTargetKind.Illust, it) },
    containerGeometry = containerGeometry,
    coverParallax = coverParallax,
)

/**
 * 创建 Miuix 默认全宽平移预测性返回手势（MiuixDefault Predictive Back Animatable）。
 *
 * 作为卡片展开方案的兜底：手势力来源页没有卡片几何信息（非作品详情页、分享链接直达、
 * 进程重建恢复）时使用。映射逐层复刻 miuix-nav `NavTransitions.MiuixDefault` 手势路径
 * （与弹簧收敛路径共用同一 d -> visual 关系）：
 *
 * 1. **顶层页面全行程平移**：随手势从 0 平移至容器宽度（100% 屏幕宽度滑出），
 *    滑动中贴合设备屏幕物理圆角裁切，无边框阴影。
 * 2. **被露出页面 25% 视差与轻微淡出**：从 -25% 屏幕宽度与 alpha 0.9 随手势还原至 0 与 1.0；
 *    覆盖语义（ColorOS）下底层页静止，只保留淡出与暗色遮罩。
 * 3. **线性暗色遮罩**：被露出页面随覆盖进度叠加最深 0.5 的遮罩，手势结束完全消退。
 *
 * @param initialBackEvent 手势起始事件。
 * @param containerGeometry 页面容器几何的即时读取源，提供全行程位移宽度与设备圆角。
 * @param registry 卡片几何信息源，用于同步底层列表视差位移。
 * @param coverParallax 底层页是否做视差位移；为 false 时位移恒为 0（覆盖语义）。
 */
@OptIn(ExperimentalDecomposeApi::class)
fun miuixSlidePredictiveBackAnimatable(
    initialBackEvent: BackEvent,
    containerGeometry: () -> PageContainerGeometry,
    registry: SharedBoundsRegistry? = null,
    coverParallax: Boolean = true,
): PredictiveBackAnimatable {
    return MiuixSmoothedPredictiveBackAnimatable(
        initialBackEvent = initialBackEvent,
        getExitModifier = { progress, _ ->
            val geometry = containerGeometry()
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = true,
                fraction = progress,
                widthPx = geometry.widthPx,
                cornerRadius = geometry.cornerRadius,
                registry = registry,
            )
        },
        getEnterModifier = { progress, _ ->
            val geometry = containerGeometry()
            val coveredFraction = (1f - progress).coerceIn(0f, 1f)
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = false,
                fraction = coveredFraction,
                widthPx = geometry.widthPx,
                cornerRadius = geometry.cornerRadius,
                registry = registry,
                parallaxFraction = if (coverParallax) MIUIX_DEFAULT_COVER_PARALLAX_FRACTION else 0f,
            )
        },
    )
}
