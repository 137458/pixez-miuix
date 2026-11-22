package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront

/** Miuix 默认转场：被覆盖页面朝前缘的视差位移比例（相对于容器宽度）。 */
internal const val MIUIX_DEFAULT_COVER_PARALLAX_FRACTION = 0.25f

/** Miuix 默认转场：被覆盖页面的不透明度衰减幅度（完全覆盖时 alpha = 1 - 0.1）。 */
internal const val MIUIX_DEFAULT_COVER_ALPHA_FALLOFF = 0.1f

/** Miuix 默认转场：过渡期间叠在被露出页面上的暗色遮罩最大不透明度。 */
internal const val MIUIX_DEFAULT_DIM_MAX_ALPHA = 0.5f

/**
 * Miuix / HyperOS 统一出入栈转场的收敛弹簧：临界阻尼（不回弹），
 * 收敛至可见阈值约 340ms，与原固定时长转场的观感对齐；
 * 相比 tween 的优势是转场被打断时能携带当前速度平滑转向（速度连续）。
 */
internal val MiuixTransitionSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 450f,
    visibilityThreshold = 0.0025f,
)

/**
 * 一次转场中某一层页面对应的 Miuix 默认侧滑帧数据。
 *
 * @param isTopLayer 当前层是否为进/出栈顶的那一层（全宽滑动）。
 * @param fraction 顶层页：相对栈顶位置的位移比例（0 贴顶、1 完全滑出到尾缘）；
 * 底层页：被覆盖进度（0 贴顶、1 完全被上层盖住）。
 */
internal data class MiuixDefaultSlideFrame(
    val isTopLayer: Boolean,
    val fraction: Float,
)

/**
 * 把 Decompose 的转场帧换算为 Miuix 默认侧滑帧数据。
 *
 * 与 [resolveCardExpandFrame] 同一套 factor 区间（Decompose StackAnimator factor 语义），
 * 映射自 miuix-nav `NavTransitions.MiuixDefault` 的 d -> visual 关系：
 * - 进/出栈顶（d <= 0）：全宽滑动，fraction = -d；
 * - 被覆盖（d > 0）：25% 视差 + 轻微淡出，fraction = d。
 *
 * 同一时刻顶层与底层帧互补（fraction 之和为 1），两层画面严格同相。
 */
internal fun resolveMiuixDefaultSlideFrame(direction: Direction, factor: Float): MiuixDefaultSlideFrame =
    if (direction.isFront) {
        MiuixDefaultSlideFrame(isTopLayer = true, fraction = factor.coerceIn(0f, 1f))
    } else {
        MiuixDefaultSlideFrame(isTopLayer = false, fraction = (-factor).coerceIn(0f, 1f))
    }

/** 单层页面的转场帧渲染闭包：按绘制期延迟 [factorProvider] 与方向产出该层的视觉修饰符。 */
internal typealias MiuixTransitionFrame =
    @Composable (factorProvider: () -> Float, direction: Direction, content: @Composable (Modifier) -> Unit) -> Unit

/**
 * 构造 MIUIX / HyperOS「卡片展开 / Miuix 默认侧滑」的帧渲染闭包。
 *
 * 帧渲染与动画驱动解耦：闭包只负责把 `(factorProvider, direction)` 映射为绘制期延迟求值的视觉修饰符，
 * 避免每帧动画在组合期重建修饰符与触发外层重组；动画驱动（自研 [SeekableStackAnimation]）只负责推进进度。
 *
 * 锚点解析、几何即时读取与回退规则见 [miuixSeekableStackAnimation] 文档。
 */
internal fun cardExpandFrame(
    rawIllustId: Long? = null,
    sourceBounds: Rect? = null,
    cardCornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP,
    containerGeometry: () -> PageContainerGeometry,
    registry: SharedBoundsRegistry? = null,
    coverParallax: Boolean = true,
): MiuixTransitionFrame = { factorProvider, direction, content ->
    val containerBounds = containerGeometry().bounds
    val candidates = registry?.resolveTransitionIllustIdCandidates(rawIllustId, direction) ?: listOf(rawIllustId)
    // 锚点优先取当前展示的作品（详情页内滑动切换后），其卡片已不在列表时退回本次打开的作品，
    // 保证收回终点始终落在用户点进来的那张卡片上；两者都不可用才回退默认侧滑。
    val anchor = registry?.resolveAnchor(candidates, containerBounds)
    // 无卡片几何、或卡片/容器尺寸非法（见 hasUsableCardExpandGeometry）时回退默认侧滑。
    // 纵横比不参与否决：等比缩放 + 双向视口裁切保证任意横纵比卡片在收回终点都能落位，故不做纵横比预筛。
    val activeSourceBounds = anchor?.card?.rect
        ?: sourceBounds?.takeIf { hasUsableCardExpandGeometry(it, containerBounds) }
    val activeCornerRadiusDp = anchor?.card?.cornerRadiusDp ?: cardCornerRadiusDp

    val geometry = containerGeometry()
    val cornerShape = remember(geometry.cornerRadius) {
        if (geometry.cornerRadius > 0.dp) RoundedCornerShape(geometry.cornerRadius) else null
    }
    val layerModifier: Modifier
    if (activeSourceBounds == null) {
        val isTopLayer = direction.isFront
        LaunchedEffect(registry, isTopLayer) {
            if (isTopLayer) {
                registry?.updateTransitionState(null, 0f)
            }
        }
        val widthPx = geometry.widthPx.takeIf { it > 0f } ?: containerBounds.width.takeIf { it > 0f } ?: 1080f
        val parallaxFraction = if (coverParallax) MIUIX_DEFAULT_COVER_PARALLAX_FRACTION else 0f
        layerModifier = remember(isTopLayer, factorProvider, direction, widthPx, cornerShape, registry, parallaxFraction) {
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = isTopLayer,
                fractionProvider = { resolveMiuixDefaultSlideFrame(direction = direction, factor = factorProvider()).fraction },
                widthPx = widthPx,
                cornerShape = cornerShape,
                registry = registry,
                parallaxFraction = parallaxFraction,
            )
        }
    } else {
        val targetIllustId = anchor?.illustId ?: candidates.firstOrNull { it != null }
        val isExiting = direction == Direction.EXIT_FRONT || direction == Direction.ENTER_BACK
        LaunchedEffect(registry, targetIllustId, activeSourceBounds, direction, factorProvider, isExiting) {
            if (registry == null) return@LaunchedEffect
            snapshotFlow { factorProvider() }.collect { factor ->
                val frame = resolveCardExpandFrame(
                    direction = direction,
                    factor = factor,
                    isTopLayer = direction.isFront,
                )
                registry.updateTransitionState(
                    illustId = targetIllustId,
                    expansion = frame.expansion,
                    sourceBounds = activeSourceBounds,
                    containerBounds = containerGeometry().bounds,
                    isExiting = isExiting,
                )
            }
        }
        val isTopLayer = direction.isFront
        layerModifier = remember(isTopLayer, factorProvider, direction, activeSourceBounds, activeCornerRadiusDp, containerGeometry) {
            if (isTopLayer) {
                Modifier.cardExpandLayer(
                    expansionProvider = {
                        resolveCardExpandFrame(direction = direction, factor = factorProvider(), isTopLayer = true).expansion
                    },
                    sourceBounds = activeSourceBounds,
                    cardCornerRadiusDp = activeCornerRadiusDp,
                    containerBoundsProvider = { containerGeometry().bounds },
                    containerCornerRadiusProvider = { containerGeometry().cornerRadius },
                )
            } else {
                Modifier.cardExpandScrim(
                    alphaProvider = {
                        val frame = resolveCardExpandFrame(direction = direction, factor = factorProvider(), isTopLayer = false)
                        cardExpandScrimAlpha(frame.expansion)
                    },
                )
            }
        }
    }
    // content 必须保持单一调用点：content 内部承载 movableContent 包裹的页面内容，
    // 若按「有无卡片几何」分支在两个调用点之间切换，锚点出现/消失（如转场收尾帧
    // 底层从卡片展开分支落回默认侧滑）会让 movable 在同帧经历销毁重建，
    // 连同层表另一层的移除叠加后触发 SaveableStateProvider 的「Key used multiple times」崩溃。
    content(layerModifier)
}

/**
 * 为顶层或底层页面应用 Miuix 默认全宽侧滑与视差遮罩图层，并同步底层列表视差位移状态。
 *
 * 始终挂载稳定的 `graphicsLayer`（与底层 `drawWithContent`）节点，稳态下仅执行恒等变换，
 * 避免在转场起止帧动态增删节点触发页面子树重排与 Backdrop 离屏重录。
 *
 * [parallaxFraction] 为底层页位移比例，0 表示覆盖语义（底层页静止、仅叠暗色遮罩）。
 * 比例必须与 [SharedBoundsRegistry.updateSlideParallax] 收到的值一致，否则收回终点会偏移。
 */
internal fun Modifier.miuixDefaultSlideLayer(
    isTopLayer: Boolean,
    fraction: Float,
    widthPx: Float,
    cornerRadius: Dp,
    registry: SharedBoundsRegistry? = null,
    parallaxFraction: Float = MIUIX_DEFAULT_COVER_PARALLAX_FRACTION,
): Modifier = miuixDefaultSlideLayer(
    isTopLayer = isTopLayer,
    fractionProvider = { fraction },
    widthPx = widthPx,
    cornerShape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else null,
    registry = registry,
    parallaxFraction = parallaxFraction,
)

private fun Modifier.miuixDefaultSlideLayer(
    isTopLayer: Boolean,
    fractionProvider: () -> Float,
    widthPx: Float,
    cornerShape: RoundedCornerShape?,
    registry: SharedBoundsRegistry? = null,
    parallaxFraction: Float = MIUIX_DEFAULT_COVER_PARALLAX_FRACTION,
): Modifier = if (isTopLayer) {
    this.graphicsLayer {
        val clampedFraction = fractionProvider().coerceIn(0f, 1f)
        if (clampedFraction <= 0.001f) {
            translationX = 0f
            clip = false
        } else {
            translationX = widthPx * clampedFraction
            if (cornerShape != null) {
                shape = cornerShape
                clip = true
            } else {
                clip = false
            }
        }
    }
} else {
    this
        .graphicsLayer {
            val clampedFraction = fractionProvider().coerceIn(0f, 1f)
            registry?.updateSlideParallax(
                widthPx = widthPx,
                fraction = clampedFraction,
                parallaxFraction = parallaxFraction,
            )
            if (clampedFraction <= 0.001f) {
                translationX = 0f
                alpha = 1f
            } else {
                translationX = -widthPx * parallaxFraction * clampedFraction
                alpha = 1f - MIUIX_DEFAULT_COVER_ALPHA_FALLOFF * clampedFraction
            }
        }
        .drawWithContent {
            drawContent()
            val clampedFraction = fractionProvider().coerceIn(0f, 1f)
            val dimAlpha = MIUIX_DEFAULT_DIM_MAX_ALPHA * clampedFraction
            if (dimAlpha > 0.001f) {
                drawRect(Color.Black.copy(alpha = dimAlpha))
            }
        }
}
