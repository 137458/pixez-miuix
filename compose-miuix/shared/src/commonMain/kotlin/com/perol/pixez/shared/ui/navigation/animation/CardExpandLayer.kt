package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront

/**
 * 「卡片展开」转场在容器内坐标系的几何描述。
 *
 * 所有分量都以 [Rect]（容器自身的窗口矩形）为基准归一化到 0..1，
 * 因此可直接作为 [graphicsLayer] 的 [TransformOrigin] 与缩放系数使用，
 * 与具体像素密度、容器尺寸解耦。
 *
 * @param scaleX 收缩态（进度 0）下顶层页面在容器宽度上的占比。
 * @param scaleY 收缩态（进度 0）下顶层页面在容器高度上的占比。
 * @param pivotX 收缩态下顶层页面缩放锚点的水平位置（容器宽度归一化）。
 * @param pivotY 收缩态下顶层页面缩放锚点的垂直位置（容器高度归一化）。
 */
internal data class CardExpandGeometry(
    val scaleX: Float,
    val scaleY: Float,
    val transX: Float,
    val transY: Float,
)

/**
 * 顶层页面在给定展开度 [expansion] 下的完整几何与视觉变换参数。
 */
internal data class CardExpandTransformState(
    val uniformScale: Float,
    val transX: Float,
    val transY: Float,
    val visibleWidthFraction: Float,
    val visibleHeightFraction: Float,
    val localCornerRadiusDp: Float,
    val contentAlpha: Float,
    val shadowElevation: Float,
)

/**
 * 计算卡片展开转场的几何参数。
 *
 * 卡片矩形 [sourceBounds] 在容器窗口内的相对位置作为平移起点，
 * 宽高比作为缩放起点，以此实现 100% 严丝合缝贴合卡片位置。
 *
 * @param sourceBounds 卡片在窗口坐标系下的矩形。
 * @param containerBounds 容器在窗口坐标系下的矩形。
 * @return 几何参数；容器尺寸非法时返回 null，调用方应跳过动画。
 */
internal fun resolveCardExpandGeometry(
    sourceBounds: Rect,
    containerBounds: Rect,
): CardExpandGeometry? {
    if (containerBounds.width <= 0f || containerBounds.height <= 0f) return null

    val scaleX = (sourceBounds.width / containerBounds.width).coerceIn(MIN_SCALE, 1f)
    val scaleY = (sourceBounds.height / containerBounds.height).coerceIn(MIN_SCALE, 1f)
    val transX = sourceBounds.left - containerBounds.left
    val transY = sourceBounds.top - containerBounds.top

    return CardExpandGeometry(
        scaleX = scaleX,
        scaleY = scaleY,
        transX = transX,
        transY = transY,
    )
}

/**
 * 计算给定展开度 [expansion] 下顶层页面的变换状态（含圆角缩放逆补偿与宽高双向裁切）。
 */
internal fun resolveCardExpandTransform(
    expansion: Float,
    sourceBounds: Rect,
    containerBounds: Rect,
    cardCornerRadiusDp: Float = 16f,
    containerCornerRadiusDp: Float = 0f,
): CardExpandTransformState? {
    val geometry = resolveCardExpandGeometry(sourceBounds, containerBounds) ?: return null
    val progress = expansion.coerceIn(0f, 1f)

    // 底层列表围绕 sourceBounds.center 按 backdropScale 纵深微缩放，
    // 因此源卡片在当前展开度下的实时中心不变、宽高与左上角随 backdropScale 同相缩放；
    // 在 progress = 0（退出动画终点）时 backdropScale = 1.0，实时矩形 100% 收敛回静止态 sourceBounds。
    val backdropScale = lerp(1f, BACKDROP_MIN_SCALE, progress)
    val liveWidth = sourceBounds.width * backdropScale
    val liveHeight = sourceBounds.height * backdropScale
    val liveLeft = sourceBounds.center.x - liveWidth * 0.5f
    val liveTop = sourceBounds.center.y - liveHeight * 0.5f

    val liveScaleX = (liveWidth / containerBounds.width).coerceIn(MIN_SCALE, 1f)
    val liveScaleY = (liveHeight / containerBounds.height).coerceIn(MIN_SCALE, 1f)

    // 详情页顶部大图与列表卡片封面图均为 fillMaxWidth()，
    // 始终以水平宽度比 liveScaleX 作为基准等比缩放系数，确保退出结束位置（expansion = 0）
    // 详情页宽度、顶部大图宽度及左上角 (transX, transY) 100% 严丝合缝贴合列表卡片，消除竖图水平放大与左偏跳变。
    val baseScale = liveScaleX
    val unscaledHeightFraction = (liveScaleY / baseScale).coerceIn(0.05f, 1f)

    val uniformScale = lerp(baseScale, 1f, progress)
    val visibleWidthFraction = 1f
    val visibleHeightFraction = lerp(unscaledHeightFraction, 1f, progress)

    val startTransX = liveLeft - containerBounds.left
    val startTransY = liveTop - containerBounds.top
    val transX = lerp(startTransX, 0f, progress)
    val transY = lerp(startTransY, 0f, progress)

    // 屏幕物理圆角由卡片圆角（16dp）平滑插值到设备屏幕物理圆角；
    // 本地 Shape 圆角需除以 uniformScale 逆向补偿，避免 graphicsLayer 缩小后屏幕圆角缩水成尖角。
    val screenCornerRadiusDp = lerp(cardCornerRadiusDp, containerCornerRadiusDp, progress)
    val localCornerRadiusDp = screenCornerRadiusDp / uniformScale.coerceAtLeast(MIN_SCALE)

    return CardExpandTransformState(
        uniformScale = uniformScale,
        transX = transX,
        transY = transY,
        visibleWidthFraction = visibleWidthFraction,
        visibleHeightFraction = visibleHeightFraction,
        localCornerRadiusDp = localCornerRadiusDp,
        contentAlpha = cardExpandContentAlpha(progress),
        shadowElevation = lerp(EXPAND_SHADOW_ELEVATION, 0f, progress),
    )
}

/**
 * 底层页面（作品列表）在给定展开度下的纵深缩放与圆角裁切状态。
 */
internal data class BackdropLayerState(
    val scale: Float,
    val transformOrigin: TransformOrigin,
    val localCornerRadiusDp: Float,
)

/**
 * 计算底层列表页面在给定展开度下的缩放比例、缩放锚点与逆向补偿后的本地圆角。
 *
 * 当底层页面向屏幕内部缩小（[expansion] > 0）脱离物理屏幕边缘时，
 * 自动施加圆角（优先取设备屏幕圆角，无屏幕圆角时平滑过渡至 [BACKDROP_FALLBACK_CORNER_RADIUS_DP]），
 * 消除底层列表缩小后四边露出的 90° 直角。
 */
internal fun resolveBackdropLayerState(
    expansion: Float,
    sourceBounds: Rect?,
    containerBounds: Rect,
    containerCornerRadiusDp: Float = 0f,
): BackdropLayerState {
    val progress = expansion.coerceIn(0f, 1f)
    val backdropScale = lerp(1f, BACKDROP_MIN_SCALE, progress)
    val origin = resolveBackdropTransformOrigin(sourceBounds, containerBounds)
    if (progress <= 0.001f) {
        return BackdropLayerState(
            scale = 1f,
            transformOrigin = origin,
            localCornerRadiusDp = 0f,
        )
    }
    val targetCornerRadiusDp = maxOf(containerCornerRadiusDp, BACKDROP_FALLBACK_CORNER_RADIUS_DP)
    val screenCornerRadiusDp = if (containerCornerRadiusDp > 0f) {
        lerp(containerCornerRadiusDp, targetCornerRadiusDp, progress)
    } else {
        val cornerRamp = (progress / 0.15f).coerceIn(0f, 1f)
        lerp(0f, targetCornerRadiusDp, cornerRamp)
    }
    val localCornerRadiusDp = screenCornerRadiusDp / backdropScale.coerceAtLeast(MIN_SCALE)
    return BackdropLayerState(
        scale = backdropScale,
        transformOrigin = origin,
        localCornerRadiusDp = localCornerRadiusDp,
    )
}

/**
 * 计算底层页面纵深微缩放的锚点（归一化到 0..1），使其围绕源卡片中心退让与聚拢。
 */
internal fun resolveBackdropTransformOrigin(
    sourceBounds: Rect?,
    containerBounds: Rect,
): TransformOrigin {
    if (sourceBounds == null || containerBounds.width <= 0f || containerBounds.height <= 0f) {
        return TransformOrigin.Center
    }
    val pivotX = ((sourceBounds.center.x - containerBounds.left) / containerBounds.width).coerceIn(0f, 1f)
    val pivotY = ((sourceBounds.center.y - containerBounds.top) / containerBounds.height).coerceIn(0f, 1f)
    return TransformOrigin(pivotX, pivotY)
}

/**
 * 计算转场过程中列表底层源卡片的不透明度，实现与顶层详情页的无重影交叉交接。
 *
 * 静止态（expansion == 0 或 1）保持完全不透明；
 * 展开/收回途中当顶层详情页已渐显接管后（expansion >= [CONTENT_FADE_THRESHOLD]），
 * 将列表原位置上的源卡片隐藏（alpha = 0），彻底消除「移动中的详情页 + 原地残留卡片」的分身重影。
 */
internal fun cardExpandSourceCardAlpha(expansion: Float): Float {
    val progress = expansion.coerceIn(0f, 1f)
    if (progress <= 0.001f || progress >= 0.999f) return 1f
    return (1f - (progress / CONTENT_FADE_THRESHOLD)).coerceIn(0f, 1f)
}

/** 收缩态缩放系数下限，避免卡片尺寸异常时页面不可见。 */
private const val MIN_SCALE = 0.05f

/** 收缩态顶层页面外的边界阴影高度（px）。 */
private const val EXPAND_SHADOW_ELEVATION = 16f

/** 底层页面在展开态下叠加的暗色遮罩最大不透明度。 */
private const val BACKDROP_SCRIM_ALPHA = 0.24f

/** 底层页面在顶层完全展开时的纵深微缩放比例。 */
private const val BACKDROP_MIN_SCALE = 0.96f

/** 底层页面缩小脱离屏幕边缘时的最小圆角半径（dp），防止在未上报屏幕圆角的设备上露出四边直角。 */
private const val BACKDROP_FALLBACK_CORNER_RADIUS_DP = 28f

/** 顶层页面在卡片展开初段/收缩末段完成淡入淡出的进度阈值。 */
private const val CONTENT_FADE_THRESHOLD = 0.20f

/**
 * 计算顶层页面在给定展开度 [expansion] 下的不透明度。
 *
 * 在展开前段（0..[CONTENT_FADE_THRESHOLD]）由 0 平滑淡入到 1，
 * 收回末段由 1 平滑淡出到 0，消除收缩态下整页内容与列表卡片切换时的生硬跳变。
 */
internal fun cardExpandContentAlpha(expansion: Float): Float {
    val progress = expansion.coerceIn(0f, 1f)
    return (progress / CONTENT_FADE_THRESHOLD).coerceIn(0f, 1f)
}

/**
 * 为顶层页面叠加「卡片展开 / 收回」视觉层（Container Transform）。
 *
 * 采用**宽度对齐等比缩放 + 动态高度视口裁切 + 圆角缩放逆向补偿 + 透明度交接**：
 * - 水平与垂直方向统一按 `liveScaleX` 等比插值，确保退出结束位置与列表卡片封面宽度和左上角 100% 重合；
 * - 本地圆角半径按 `screenCornerRadius / uniformScale` 逆向补偿，使缩放后的视觉圆角与卡片 16.dp 严丝合缝；
 * - 配合 [cardExpandContentAlpha] 与 [cardExpandSourceCardAlpha] 在初段/末段无缝交叉交接。
 *
 * @param expansion 展开度，0f 表示收缩在卡片内、1f 表示铺满容器；越界值会被钳制。
 * @param sourceBounds 卡片窗口矩形，可为 null。
 * @param containerBounds 容器窗口矩形。
 * @param containerCornerRadius 设备屏幕物理圆角，收缩态下用于裁切顶层页面。
 */
internal fun Modifier.cardExpandLayer(
    expansion: Float,
    sourceBounds: Rect?,
    containerBounds: Rect,
    containerCornerRadius: Dp,
): Modifier {
    if (sourceBounds == null) return this
    val state = resolveCardExpandTransform(
        expansion = expansion,
        sourceBounds = sourceBounds,
        containerBounds = containerBounds,
        cardCornerRadiusDp = 16f,
        containerCornerRadiusDp = containerCornerRadius.value,
    ) ?: return this

    return this.graphicsLayer {
        this.transformOrigin = TransformOrigin(0f, 0f)
        this.scaleX = state.uniformScale
        this.scaleY = state.uniformScale
        this.translationX = state.transX
        this.translationY = state.transY
        this.alpha = state.contentAlpha
        shape = ClippedContainerShape(
            widthFraction = state.visibleWidthFraction,
            heightFraction = state.visibleHeightFraction,
            cornerRadius = state.localCornerRadiusDp.dp,
        )
        clip = true
        this.shadowElevation = state.shadowElevation
    }
}

/**
 * 支持水平居中与垂直顶部比例裁切的圆角矩形 Shape。
 *
 * 用于在等比缩放下将顶层页面容器裁切至卡片当前对应的真实宽高并保留补偿后的圆角。
 */
private data class ClippedContainerShape(
    val widthFraction: Float,
    val heightFraction: Float,
    val cornerRadius: Dp,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val clampedWidthFraction = widthFraction.coerceIn(0.05f, 1f)
        val clampedHeightFraction = heightFraction.coerceIn(0.05f, 1f)
        val horizontalInset = size.width * (1f - clampedWidthFraction) * 0.5f
        val clippedHeight = (size.height * clampedHeightFraction).coerceAtMost(size.height)
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                left = horizontalInset,
                top = 0f,
                right = (size.width - horizontalInset).coerceAtLeast(horizontalInset + 1f),
                bottom = clippedHeight,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
            ),
        )
    }
}

/**
 * 在底层页面内容之上叠加暗色遮罩、围绕源卡片中心的纵深微缩放以及圆角裁切。
 *
 * @param alpha 遮罩不透明度，0f 表示完全透明（不绘制）；越界值会被钳制。
 * @param expansion 顶层页面的展开度（0f..1f），用于同步底层纵深缩放与圆角裁切。
 * @param sourceBounds 源卡片矩形，用于将底层缩放锚点对齐到卡片中心。
 * @param containerBounds 容器窗口矩形。
 * @param containerCornerRadius 设备屏幕物理圆角，用于底层页面缩小时平滑裁切四角避免露出直角。
 */
internal fun Modifier.cardExpandScrim(
    alpha: Float,
    expansion: Float = (alpha / BACKDROP_SCRIM_ALPHA).coerceIn(0f, 1f),
    sourceBounds: Rect? = null,
    containerBounds: Rect = Rect.Zero,
    containerCornerRadius: Dp = 0.dp,
): Modifier {
    val scrimAlpha = alpha.coerceIn(0f, 1f)
    val backdropState = resolveBackdropLayerState(
        expansion = expansion,
        sourceBounds = sourceBounds,
        containerBounds = containerBounds,
        containerCornerRadiusDp = containerCornerRadius.value,
    )
    return this
        .graphicsLayer {
            this.transformOrigin = backdropState.transformOrigin
            this.scaleX = backdropState.scale
            this.scaleY = backdropState.scale
            if (backdropState.localCornerRadiusDp > 0.1f) {
                this.shape = RoundedCornerShape(backdropState.localCornerRadiusDp.dp)
                this.clip = true
            }
        }
        .drawWithContent {
            drawContent()
            if (scrimAlpha > 0.001f) {
                drawRect(Color.Black.copy(alpha = scrimAlpha))
            }
        }
}

/**
 * 由顶层页面的展开度换算出底层遮罩应有的不透明度。
 *
 * 顶层页面铺满容器（expansion = 1）时遮罩最深，完全收回卡片（expansion = 0）时遮罩消失；
 * 与缩放进度取自同一个展开度，两层页面的纵深关系保持同步。
 *
 * @param expansion 顶层页面的展开度，1f 时遮罩最强、0f 时完全消失。
 * @param maxAlpha 遮罩在最深时的最大不透明度，默认取 [BACKDROP_SCRIM_ALPHA]。
 */
internal fun cardExpandScrimAlpha(
    expansion: Float,
    maxAlpha: Float = BACKDROP_SCRIM_ALPHA,
): Float = maxAlpha * expansion.coerceIn(0f, 1f)

/**
 * 一次转场中某一层页面所对应的「卡片展开」帧数据。
 *
 * @param expansion 顶层页面的展开度，0f 表示收缩在卡片内、1f 表示铺满容器。
 * 无论当前层是顶层卡片还是其下的列表，取到的都是同一个顶层展开度，
 * 保证两层的缩放与遮罩永远同相。
 * @param isTopLayer 当前层是否为需要播放缩放的那一层（即发起转场的作品详情页）。
 * 为 false 时该层不位移，只按 [expansion] 叠加消退遮罩。
 */
internal data class CardExpandFrame(
    val expansion: Float,
    val isTopLayer: Boolean,
)

/**
 * 把 Decompose 的转场帧换算为「卡片展开」帧数据。
 *
 * `StackAnimator` 的 `factor` 依方向取不同符号区间（见 `StackAnimator.kt` 文档）：
 * - [Direction.ENTER_FRONT]：1F -> 0F，push 时新顶层页面入场，应「由小放大」。
 * - [Direction.EXIT_FRONT]：0F -> 1F，pop 时原顶层页面离场，应「由大收缩」。
 * - [Direction.ENTER_BACK]：-1F -> 0F，pop 时被覆盖页面重新回到前台，保持铺满。
 * - [Direction.EXIT_BACK]：0F -> -1F，push 时原顶层页面退到后台，保持铺满。
 *
 * 顶层页面始终是「前层」([Direction.isFront])：push 时是入场的详情页，
 * pop 时是离场的详情页；两条路径下其展开度都等于 `1 - factor`：
 * [Direction.ENTER_FRONT] 的 factor 为 `a`、[Direction.EXIT_FRONT] 的为 `1 - a`
 * （`a` 为动画状态，由 1 走到 0），代入后正好得到 0 -> 1 与 1 -> 0。
 *
 * 另一层（列表）不承担转场进度，这里按同一时刻反推顶层展开度：
 * [Direction.EXIT_BACK] 的 factor 为 `a - 1`、[Direction.ENTER_BACK] 的为 `-a`，
 * 代入 `1 - (a - 1) = 2 - a` 与 `1 - (-a)` 后化简，两者均等于 `-factor`。
 *
 * @param direction 本次转场中当前层所处的方向。
 * @param factor Decompose 给出的转场帧值，取值区间随方向变化。
 * @return 当前层的展开度与是否为顶层缩放层；展开度已钳制到 0f..1f。
 */
internal fun resolveCardExpandFrame(
    direction: Direction,
    factor: Float,
    isTopLayer: Boolean = direction.isFront,
): CardExpandFrame =
    if (isTopLayer) {
        CardExpandFrame(expansion = (1f - factor).coerceIn(0f, 1f), isTopLayer = true)
    } else {
        CardExpandFrame(expansion = (-factor).coerceIn(0f, 1f), isTopLayer = false)
    }

/**
 * 把预测性返回手势进度换算为顶层页面的展开度。
 *
 * 手势刚开始（progress = 0）时详情页铺满容器，拖到底（progress = 1）时完全收进作品卡片，
 * 因此展开度与手势进度反向对应；取消手势时 progress 回落，展开度随之平滑还原。
 *
 * @param progress 预测性返回手势进度，0f..1f；越界值会被钳制。
 * @return 顶层页面的展开度，0f..1f。
 */
internal fun predictiveBackCardExpandExpansion(progress: Float): Float =
    (1f - progress).coerceIn(0f, 1f)

/** 线性插值。 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
