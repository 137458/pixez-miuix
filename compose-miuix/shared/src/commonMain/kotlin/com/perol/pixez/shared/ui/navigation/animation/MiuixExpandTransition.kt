package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
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

/** 单层页面的转场帧渲染闭包：按 factor 与方向产出该层的视觉修饰符。 */
internal typealias MiuixTransitionFrame =
    @Composable (factor: Float, direction: Direction, content: @Composable (Modifier) -> Unit) -> Unit

/**
 * 构造 MIUIX / HyperOS「卡片展开 / Miuix 默认侧滑」的帧渲染闭包。
 *
 * 帧渲染与动画驱动解耦：闭包只负责把 (factor, direction) 映射为视觉修饰符，
 * 并把转场状态上报 [SharedBoundsRegistry]；动画驱动（Decompose stackAnimator 或
 * 自研 [SeekableStackAnimation]）各自负责 factor 的产生节奏。
 *
 * 锚点解析、几何即时读取与回退规则见 [miuixSeekableStackAnimation] 文档。
 */
internal fun cardExpandFrame(
    rawIllustId: Long? = null,
    sourceBounds: Rect? = null,
    cardCornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP,
    containerGeometry: () -> PageContainerGeometry,
    registry: SharedBoundsRegistry? = null,
): MiuixTransitionFrame = { factor, direction, content ->
    val containerBounds = containerGeometry().bounds
    val candidates = registry?.resolveTransitionIllustIdCandidates(rawIllustId, direction) ?: listOf(rawIllustId)
    // 锚点优先取当前展示的作品（详情页内滑动切换后），其卡片已不在列表时退回本次打开的作品，
    // 保证收回终点始终落在用户点进来的那张卡片上；两者都不可用才回退默认侧滑。
    val anchor = registry?.resolveAnchor(candidates, containerBounds)
    // 容器纵横比与卡片纵横比差距过大（典型如桌面端横向窗口）时收回终点无法覆盖卡片全高，
    // 逐帧表现为窗口比卡片矮、收尾时卡片下半部分突兀补入；该几何无法落点，与「无卡片几何」同样回退默认侧滑。
    val activeSourceBounds = anchor?.card?.rect
        ?: sourceBounds?.takeIf { isCardExpandLandable(it, containerBounds) }
    val activeCornerRadiusDp = anchor?.card?.cornerRadiusDp ?: cardCornerRadiusDp
    NavTransitionLog.d("animator") { "frame dir=$direction factor=$factor anchor=${anchor?.illustId} slidePath=${activeSourceBounds == null} rawId=$rawIllustId container=$containerBounds" }

    if (activeSourceBounds == null) {
        val frame = resolveMiuixDefaultSlideFrame(direction = direction, factor = factor)
        if (frame.isTopLayer) {
            registry?.updateTransitionState(null, 0f)
        }
        val geometry = containerGeometry()
        val widthPx = geometry.widthPx.takeIf { it > 0f } ?: containerBounds.width.takeIf { it > 0f } ?: 1080f
        content(
            Modifier.miuixDefaultSlideLayer(
                isTopLayer = frame.isTopLayer,
                fraction = frame.fraction,
                widthPx = widthPx,
                cornerRadius = geometry.cornerRadius,
                registry = registry,
            ),
        )
    } else {
        val geometry = containerGeometry()
        val frame = resolveCardExpandFrame(direction = direction, factor = factor, isTopLayer = direction.isFront)
        registry?.updateTransitionState(
            illustId = anchor?.illustId ?: candidates.firstOrNull { it != null },
            expansion = frame.expansion,
            sourceBounds = activeSourceBounds,
            containerBounds = geometry.bounds,
            isExiting = direction == Direction.EXIT_FRONT || direction == Direction.ENTER_BACK,
        )
        content(
            if (frame.isTopLayer) {
                Modifier.cardExpandLayer(
                    expansion = frame.expansion,
                    sourceBounds = activeSourceBounds,
                    cardCornerRadiusDp = activeCornerRadiusDp,
                    containerBounds = geometry.bounds,
                    containerCornerRadius = geometry.cornerRadius,
                )
            } else {
                Modifier.cardExpandScrim(
                    alpha = cardExpandScrimAlpha(frame.expansion),
                    expansion = frame.expansion,
                    sourceBounds = activeSourceBounds,
                    containerBounds = geometry.bounds,
                    containerCornerRadius = geometry.cornerRadius,
                )
            },
        )
    }
}

/**
 * 为顶层或底层页面应用 Miuix 默认全宽侧滑与视差遮罩图层，并同步底层列表视差位移状态。
 */
internal fun Modifier.miuixDefaultSlideLayer(
    isTopLayer: Boolean,
    fraction: Float,
    widthPx: Float,
    cornerRadius: Dp,
    registry: SharedBoundsRegistry? = null,
): Modifier {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    if (!isTopLayer) {
        registry?.updateSlideParallax(widthPx = widthPx, fraction = clampedFraction)
    }
    if (clampedFraction <= 0.001f) return this
    return if (isTopLayer) {
        this.graphicsLayer {
            translationX = widthPx * clampedFraction
            if (cornerRadius > 0.dp && clampedFraction > 0f) {
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
        }
    } else {
        val parallaxX = -widthPx * MIUIX_DEFAULT_COVER_PARALLAX_FRACTION * clampedFraction
        this
            .graphicsLayer {
                translationX = parallaxX
                alpha = 1f - MIUIX_DEFAULT_COVER_ALPHA_FALLOFF * clampedFraction
            }
            .drawWithContent {
                drawContent()
                val dimAlpha = MIUIX_DEFAULT_DIM_MAX_ALPHA * clampedFraction
                if (dimAlpha > 0.001f) {
                    drawRect(Color.Black.copy(alpha = dimAlpha))
                }
            }
    }
}
