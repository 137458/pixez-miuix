package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator

/**
 * MIUIX / HyperOS「卡片展开」二级页面转场的统一时长（毫秒）。
 */
private const val TRANSITION_DURATION_MILLIS = 340

/** Miuix 默认转场：被覆盖页面朝前缘的视差位移比例（相对于容器宽度）。 */
internal const val MIUIX_DEFAULT_COVER_PARALLAX_FRACTION = 0.25f

/** Miuix 默认转场：被覆盖页面的不透明度衰减幅度（完全覆盖时 alpha = 1 - 0.1）。 */
internal const val MIUIX_DEFAULT_COVER_ALPHA_FALLOFF = 0.1f

/** Miuix 默认转场：过渡期间叠在被露出页面上的暗色遮罩最大不透明度。 */
internal const val MIUIX_DEFAULT_DIM_MAX_ALPHA = 0.5f

/**
 * Miuix 默认转场的收敛弹簧，参数取自 miuix-nav `NavDriverSpec` 默认值：
 * 临界阻尼（不回弹）+ 低刚度（深度轴 146），单步进/出栈约 500ms 收敛，
 * 与官方导航「既定手感」一致。
 */
internal val MiuixDefaultSlideSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 146f,
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
 * 与 [resolveCardExpandFrame] 同一套 factor 区间（见 `StackAnimator.kt` 文档），
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

/**
 * 构造 MIUIX / HyperOS 统一的二级页面转场动画器。
 *
 * 顶层页面被视为一张从作品列表卡片「生长」出来的容器：打开详情页时按卡片矩形
 * 的尺寸与位置等比展开并平滑淡入，关闭时反向收缩归位；圆角由卡片圆角
 * 渐变到设备屏幕物理圆角 [containerCornerRadius]，形成连贯的空间连续性。
 *
 * [sourceBounds] 为 null 时（画师页、设置页这类本就没有来源卡片的二级页面，
 * 以及分享链接直达、进程重建恢复等入口），或卡片几何在当前容器下无法落点
 * （见 [isCardExpandLandable]：横向窗口里的竖图卡片等比缩放后盖不住卡片全高）
 * 逐层复刻 Miuix 官方默认转场 `NavTransitions.MiuixDefault`：顶层页面全宽滑入/滑出
 * （滑动中贴合设备屏幕物理圆角裁切），被覆盖页面 25% 视差左移、轻微淡出并叠加线性加深的暗色遮罩，
 * 驱动弹簧取 miuix-nav `NavDriverSpec` 默认参数。
 *
 * [containerGeometry] 为即时读取源：Decompose 的 `MovableStackAnimation` 按 child 缓存动画器实例，
 * 几何以快照捕获会被过期缓存固化（冷启动首帧 bounds 未回填时创建的动画器一直持有 `Rect.Zero`），
 * 必须每帧即时读取。
 *
 * @param sourceBounds 发起转场的卡片窗口矩形，为 null 时使用 Miuix 默认全宽侧滑。
 * @param cardCornerRadiusDp 源卡片自身视觉圆角（dp），收回终点按它做像素级对齐。
 * @param containerGeometry 页面容器几何的即时读取源，用于归一化卡片几何。
 * @param forceSlidePath 强制两层都走 Miuix 默认侧滑路径（宽屏用）：宽屏容器纵横比与卡片差距大，
 *   卡片展开会畸变；若只让顶层走侧滑而底层仍按锚点走卡片遮罩，两层路径混用会出现
 *   底层纵深缩放与顶层全宽平移互相矛盾的渲染。registry 仍保留用于底层视差位移登记。
 * @return 可直接交给 stackAnimation 使用的 [StackAnimator]。
 */
internal fun cardExpandStackAnimator(
    rawIllustId: Long? = null,
    sourceBounds: Rect? = null,
    cardCornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP,
    containerGeometry: () -> PageContainerGeometry,
    registry: SharedBoundsRegistry? = null,
    forceSlidePath: Boolean = false,
): StackAnimator {
    val duration: FiniteAnimationSpec<Float> = tween(
        durationMillis = TRANSITION_DURATION_MILLIS,
        easing = HyperOSDecelerateEasing,
    )
    return stackAnimator(animationSpec = duration) { factor, direction, content ->
        val containerBounds = containerGeometry().bounds
        val candidates = if (forceSlidePath) {
            emptyList()
        } else {
            registry?.resolveTransitionIllustIdCandidates(rawIllustId, direction) ?: listOf(rawIllustId)
        }
        // 锚点优先取当前展示的作品（详情页内滑动切换后），其卡片已不在列表时退回本次打开的作品，
        // 保证收回终点始终落在用户点进来的那张卡片上；两者都不可用才回退默认侧滑。
        val anchor = if (forceSlidePath) null else registry?.resolveAnchor(candidates, containerBounds)
        // 容器纵横比与卡片纵横比差距过大（典型如桌面端横向窗口）时收回终点无法覆盖卡片全高，
        // 逐帧表现为窗口比卡片矮、收尾时卡片下半部分突兀补入；该几何无法落点，与「无卡片几何」同样回退默认侧滑。
        val activeSourceBounds = anchor?.card?.rect
            ?: sourceBounds?.takeIf { !forceSlidePath && isCardExpandLandable(it, containerBounds) }
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
