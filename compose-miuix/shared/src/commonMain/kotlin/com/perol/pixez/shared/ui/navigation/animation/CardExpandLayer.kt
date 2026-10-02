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
 * 顶层页面在给定展开度 [expansion] 下的完整几何与视觉变换参数。
 */
internal data class CardExpandTransformState(
    val uniformScale: Float,
    val transX: Float,
    val transY: Float,
    val visibleWidthFraction: Float = 1f,
    val visibleHeightFraction: Float,
    val windowHorizontalBias: Float = 0f,
    val localCornerRadiusDp: Float,
    val contentAlpha: Float,
    val shadowElevation: Float,
)

/**
 * 计算给定展开度 [expansion] 下顶层页面的变换状态（含圆角缩放逆补偿与高度方向裁切）。
 *
 * 可见窗口的矩形严格等于 `lerp(实时卡片矩形, 容器矩形, expansion)`：
 * 水平方向由等比缩放系数保证宽度与左上角贴合实时卡片，垂直方向由高度裁切比例保证高度贴合实时卡片，
 * 因此窗口在展开全程都包住实时卡片，收回终点则与卡片静止态矩形像素级重合。
 *
 * @param sourceBounds 卡片在窗口坐标系下的静止态矩形。
 * @param containerBounds 容器在窗口坐标系下的矩形。
 * @param cardCornerRadiusDp 源卡片自身视觉圆角（dp）。
 * @return 变换状态；容器尺寸非法时返回 null，调用方应跳过动画。
 */
internal fun resolveCardExpandTransform(
    expansion: Float,
    sourceBounds: Rect,
    containerBounds: Rect,
    cardCornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP,
    containerCornerRadiusDp: Float = 0f,
): CardExpandTransformState? {
    if (containerBounds.width <= 0f || containerBounds.height <= 0f) return null
    val progress = expansion.coerceIn(0f, 1f)

    // 底层列表保持 1.0x 铺满全屏（BACKDROP_MIN_SCALE = 1.0），
    // 在 progress = 0（退出动画终点）时实时矩形 100% 收敛回静止态 sourceBounds。
    val backdropScale = lerp(1f, BACKDROP_MIN_SCALE, progress)
    val liveWidth = sourceBounds.width * backdropScale
    val liveHeight = sourceBounds.height * backdropScale
    val liveLeft = sourceBounds.center.x - liveWidth * 0.5f
    val liveTop = sourceBounds.center.y - liveHeight * 0.5f

    val liveScaleX = (liveWidth / containerBounds.width).coerceIn(MIN_SCALE, 1f)
    val liveScaleY = (liveHeight / containerBounds.height).coerceIn(MIN_SCALE, 1f)

    // 取水平与垂直缩放比的最大值作为等比缩放基准：
    // - 竖屏常规卡片（liveScaleX >= liveScaleY）：baseScale = liveScaleX，宽度 100% 贴合卡片，高度由 visibleHeightFraction 裁切；
    // - 横屏容器或超长竖图卡片（liveScaleY > liveScaleX）：baseScale = liveScaleY，高度 100% 贴合卡片，宽度由 visibleWidthFraction 裁切；
    // 从而在竖屏与横屏下均保持 1:1 无畸变等比缩放，且退出终点可见窗口四边与卡片矩形 100% 像素级重合。
    val baseScale = maxOf(liveScaleX, liveScaleY)

    val uniformScale = lerp(baseScale, 1f, progress)

    // 可见窗口宽高直接对「实时卡片矩形 -> 容器矩形」线性插值：
    // 整条轨迹严格等于 lerp(实时卡片矩形, 容器矩形, expansion)，窗口四边始终包住实时卡片。
    val windowWidth = lerp(liveWidth, containerBounds.width, progress)
    val windowHeight = lerp(liveHeight, containerBounds.height, progress)
    val visibleWidthFraction = (windowWidth / (containerBounds.width * uniformScale))
        .coerceIn(MIN_VISIBLE_HEIGHT_FRACTION, 1f)
    val visibleHeightFraction = (windowHeight / (containerBounds.height * uniformScale))
        .coerceIn(MIN_VISIBLE_HEIGHT_FRACTION, 1f)

    val startTransX = liveLeft - containerBounds.left
    val startTransY = liveTop - containerBounds.top

    // 水平方向发生视口裁切（高度驱动）时，裁切窗口改为在页面内水平居中：
    // 宽屏详情页大图以页面水平中心呈现，窗口若锚定页面左缘，收回时居中的作品会被单侧
    // 裁出画面；居中后窗口围绕页面中心对称收窄，平移同步反向补偿，落点仍严格等于卡片矩形。
    val windowHorizontalBias = if (visibleWidthFraction < 1f) 0.5f else 0f
    val clippedPageWidth = visibleWidthFraction * containerBounds.width
    val shapeLeftPage = (containerBounds.width - clippedPageWidth) * windowHorizontalBias
    val transX = lerp(startTransX, 0f, progress) - uniformScale * shapeLeftPage
    val transY = lerp(startTransY, 0f, progress)

    // 屏幕物理圆角由卡片自身圆角平滑插值到设备屏幕物理圆角；
    // 本地 Shape 圆角需除以 uniformScale 逆向补偿，避免 graphicsLayer 缩小后屏幕圆角缩水成尖角。
    val screenCornerRadiusDp = lerp(cardCornerRadiusDp, containerCornerRadiusDp, progress)
    val localCornerRadiusDp = screenCornerRadiusDp / uniformScale.coerceAtLeast(MIN_SCALE)

    return CardExpandTransformState(
        uniformScale = uniformScale,
        transX = transX,
        transY = transY,
        visibleWidthFraction = visibleWidthFraction,
        visibleHeightFraction = visibleHeightFraction,
        windowHorizontalBias = windowHorizontalBias,
        localCornerRadiusDp = localCornerRadiusDp,
        contentAlpha = cardExpandContentAlpha(progress),
        shadowElevation = lerp(EXPAND_SHADOW_ELEVATION, 0f, progress),
    )
}

/**
 * 判定给定容器能否承载「卡片展开」转场。
 *
 * 通过 `maxOf(liveScaleX, liveScaleY)` 等比缩放配合双向视口裁切（[CardExpandTransformState.visibleWidthFraction]
 * 与 [CardExpandTransformState.visibleHeightFraction]），竖屏与横屏容器下的任意比例卡片在收回终点均能与卡片四边像素级重合，
 * 因此只要容器与卡片尺寸合法即可执行卡片转场。
 */
internal fun isCardExpandLandable(sourceBounds: Rect, containerBounds: Rect): Boolean {
    if (containerBounds.width <= 0f || containerBounds.height <= 0f) return false
    if (sourceBounds.width <= 0f || sourceBounds.height <= 0f) return false
    return true
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
 */
internal fun resolveBackdropLayerState(
    expansion: Float,
    sourceBounds: Rect?,
    containerBounds: Rect,
    containerCornerRadiusDp: Float = 0f,
): BackdropLayerState {
    val origin = resolveBackdropTransformOrigin(sourceBounds, containerBounds)
    return BackdropLayerState(
        scale = 1f,
        transformOrigin = origin,
        localCornerRadiusDp = 0f,
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
 * 计算转场过程中列表底层源卡片的不透明度，实现与顶层详情页的无闪烁、无重影交接。
 *
 * - 在顶层淡入窗口 `[0, CONTENT_FADE_THRESHOLD]` 内，底层源卡片保持 `1.0f` 完全不透明，
 *   确保 Porter-Duff `SRC_OVER` 合成不透明度 `1 - (1 - sourceAlpha) * (1 - contentAlpha)` 恒等于 `1.0`，
 *   彻底根除两层同时半透明（`0.5 + 0.5` 合成后仅 `0.75`）导致列表浅色底板透出闪烁的问题；
 * - 当顶层完全不透明（`expansion >= CONTENT_FADE_THRESHOLD`）且正盖在源卡片上方时，
 *   源卡片在 `[CONTENT_FADE_THRESHOLD, SOURCE_CARD_HIDE_THRESHOLD]` 区间内在顶层遮蔽下快速隐藏至 `0f`，
 *   从而在顶层离开卡片原位向全屏展开的中后段彻底消除底层原位卡片重影。
 */
internal fun cardExpandSourceCardAlpha(expansion: Float): Float {
    val progress = expansion.coerceIn(0f, 1f)
    if (progress <= CONTENT_FADE_THRESHOLD || progress >= 0.999f) return 1f
    val hideWindow = (SOURCE_CARD_HIDE_THRESHOLD - CONTENT_FADE_THRESHOLD).coerceAtLeast(0.001f)
    return (1f - ((progress - CONTENT_FADE_THRESHOLD) / hideWindow)).coerceIn(0f, 1f)
}

/** 收缩态缩放系数下限，避免卡片尺寸异常时页面不可见。 */
private const val MIN_SCALE = 0.05f

/** 可见窗口高度比例下限，避免卡片高度退化时窗口被压成一条线。 */
private const val MIN_VISIBLE_HEIGHT_FRACTION = 0.05f

/** 收缩态顶层页面外的边界阴影高度（px）。 */
private const val EXPAND_SHADOW_ELEVATION = 16f

/** 底层页面在展开态下叠加的暗色遮罩最大不透明度。 */
private const val BACKDROP_SCRIM_ALPHA = 0.24f

/**
 * 底层页面在顶层完全展开时的缩放比例：保持 1.0f 铺满全屏，
 * 避免局部卡片展开/收回期间底层列表向内收缩退让而在列表四周露出浅色容器底色白边。
 */
internal const val BACKDROP_MIN_SCALE = 1.0f

/**
 * 顶层页面淡入/淡出的进度窗口。
 */
private const val CONTENT_FADE_THRESHOLD = 0.05f

/**
 * 底层源卡片在顶层已完全不透明（> [CONTENT_FADE_THRESHOLD]）后完成隐藏的展开度上限。
 */
private const val SOURCE_CARD_HIDE_THRESHOLD = 0.10f

/** 详情页非图片附属控件（悬浮顶栏与图片下方信息/标签卡片）开始淡入、完成淡出的展开度阈值。 */
private const val DETAIL_CHROME_FADE_START = 0.65f

/**
 * 计算顶层页面在给定展开度 [expansion] 下的不透明度。
 */
internal fun cardExpandContentAlpha(expansion: Float): Float {
    val progress = expansion.coerceIn(0f, 1f)
    return (progress / CONTENT_FADE_THRESHOLD).coerceIn(0f, 1f)
}

/**
 * 计算卡片转场期间详情页非图片附属控件（悬浮液态玻璃顶栏与大图下方的详情卡片）的不透明度。
 */
internal fun cardExpandDetailChromeAlpha(expansion: Float): Float {
    val progress = expansion.coerceIn(0f, 1f)
    return ((progress - DETAIL_CHROME_FADE_START) / (1f - DETAIL_CHROME_FADE_START)).coerceIn(0f, 1f)
}

/**
 * 为顶层页面叠加「卡片展开 / 收回」视觉层（Container Transform）。
 *
 * 始终挂载同一个 `graphicsLayer` 节点（稳态 `expansion >= 0.999f` 时使用恒等变换且关闭裁切），
 * 避免在转场起止帧动态挂载/卸载 `GraphicsLayerModifierNode` 触发子树重排与离屏 `RenderNode` 销毁闪烁。
 */
internal fun Modifier.cardExpandLayer(
    expansion: Float,
    sourceBounds: Rect?,
    cardCornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP,
    containerBounds: Rect,
    containerCornerRadius: Dp,
): Modifier {
    val state = if (sourceBounds != null && expansion < 0.999f) {
        resolveCardExpandTransform(
            expansion = expansion,
            sourceBounds = sourceBounds,
            containerBounds = containerBounds,
            cardCornerRadiusDp = cardCornerRadiusDp,
            containerCornerRadiusDp = containerCornerRadius.value,
        )
    } else {
        null
    }

    return this.graphicsLayer {
        if (state == null) {
            this.transformOrigin = TransformOrigin.Center
            this.scaleX = 1f
            this.scaleY = 1f
            this.translationX = 0f
            this.translationY = 0f
            this.alpha = 1f
            this.clip = false
            this.shadowElevation = 0f
        } else {
            this.transformOrigin = TransformOrigin(0f, 0f)
            this.scaleX = state.uniformScale
            this.scaleY = state.uniformScale
            this.translationX = state.transX
            this.translationY = state.transY
            this.alpha = state.contentAlpha
            shape = ClippedContainerShape(
                widthFraction = state.visibleWidthFraction,
                heightFraction = state.visibleHeightFraction,
                horizontalBias = state.windowHorizontalBias,
                cornerRadius = state.localCornerRadiusDp.dp,
            )
            clip = true
            this.shadowElevation = state.shadowElevation
        }
    }
}

/**
 * 支持水平与垂直双向比例裁切的圆角矩形 Shape。
 *
 * 用于在等比缩放下将顶层页面容器裁切至卡片当前对应的真实宽高并保留补偿后的圆角。
 * 水平裁切时窗口按 [horizontalBias] 在页面内偏移（0 贴左缘、0.5 居中），
 * 居中收窄保证宽屏下以页面中心呈现的大图在收回全程不被单侧裁出画面。
 */
private data class ClippedContainerShape(
    val widthFraction: Float = 1f,
    val heightFraction: Float,
    val horizontalBias: Float = 0f,
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
        val clippedWidth = (size.width * clampedWidthFraction).coerceAtMost(size.width)
        val clippedHeight = (size.height * clampedHeightFraction).coerceAtMost(size.height)
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                left = (size.width - clippedWidth) * horizontalBias.coerceIn(0f, 1f),
                top = 0f,
                right = (size.width - clippedWidth) * horizontalBias.coerceIn(0f, 1f) + clippedWidth,
                bottom = clippedHeight,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
            ),
        )
    }
}

/**
 * 在底层页面内容之上叠加全屏暗色遮罩。
 *
 * 始终保持 `drawWithContent` 节点挂载（`scrimAlpha <= 0.001f` 时仅调用 `drawContent()`），
 * 防止转场起止帧动态增删修饰符节点触发底层列表重测量与闪烁。
 */
internal fun Modifier.cardExpandScrim(
    alpha: Float,
    expansion: Float = (alpha / BACKDROP_SCRIM_ALPHA).coerceIn(0f, 1f),
    sourceBounds: Rect? = null,
    containerBounds: Rect = Rect.Zero,
    containerCornerRadius: Dp = 0.dp,
): Modifier {
    val scrimAlpha = alpha.coerceIn(0f, 1f)
    return this.drawWithContent {
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
    isTopLayer: Boolean,
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
