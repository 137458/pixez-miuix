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
    val pivotX: Float,
    val pivotY: Float,
)

/**
 * 计算卡片展开转场的归一化几何参数。
 *
 * 卡片矩形 [sourceBounds] 可能部分位于容器之外（例如列表被顶栏遮挡或滚动到容器外），
 * 这里对位置不做裁剪，只对尺寸做下限保护，避免缩放系数退化为 0 导致页面不可见。
 *
 * @param sourceBounds 卡片在窗口坐标系下的矩形。
 * @param containerBounds 容器在窗口坐标系下的矩形。
 * @return 归一化几何参数；容器尺寸非法时返回 null，调用方应跳过动画。
 */
internal fun resolveCardExpandGeometry(
    sourceBounds: Rect,
    containerBounds: Rect,
): CardExpandGeometry? {
    if (containerBounds.width <= 0f || containerBounds.height <= 0f) return null

    val scaleX = (sourceBounds.width / containerBounds.width).coerceIn(MIN_SCALE, 1f)
    val scaleY = (sourceBounds.height / containerBounds.height).coerceIn(MIN_SCALE, 1f)

    // 卡片左上角在容器内的相对位置（可能为负，表示卡片部分超出容器上边界）。
    val leftRatio = (sourceBounds.left - containerBounds.left) / containerBounds.width
    val topRatio = (sourceBounds.top - containerBounds.top) / containerBounds.height

    return CardExpandGeometry(
        scaleX = scaleX,
        scaleY = scaleY,
        // 以卡片中心为缩放锚点，使页面视觉上正好落在卡片位置。
        pivotX = leftRatio + scaleX / 2f,
        pivotY = topRatio + scaleY / 2f,
    )
}

/** 收缩态缩放系数下限，避免卡片尺寸异常时页面不可见。 */
private const val MIN_SCALE = 0.05f

/** 收缩态顶层页面外的边界阴影高度（px）。 */
private const val EXPAND_SHADOW_ELEVATION = 12f

/** 底层页面在收缩态下叠加的暗色遮罩最大不透明度。 */
private const val BACKDROP_SCRIM_ALPHA = 0.18f

/**
 * 为顶层页面叠加「卡片展开 / 收回」视觉层。
 *
 * [expansion] 为 0 时页面完全收在卡片内（带圆角与阴影），为 1 时铺满容器且无变换；
 * 展开与收回共用同一条 0..1 的展开度，不再由调用方传入「方向」，
 * 方向与帧值的换算统一收敛到 [resolveCardExpandFrame]、[predictiveBackCardExpandExpansion]。
 * [sourceBounds] 缺失或容器几何非法时直接返回原 [Modifier]，保证不引入任何副作用。
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
    val geometry = resolveCardExpandGeometry(sourceBounds, containerBounds) ?: return this

    val progress = expansion.coerceIn(0f, 1f)

    val scaleX = lerp(geometry.scaleX, 1f, progress)
    val scaleY = lerp(geometry.scaleY, 1f, progress)
    val pivotX = lerp(geometry.pivotX, 0.5f, progress)
    val pivotY = lerp(geometry.pivotY, 0.5f, progress)
    val cornerRadius = containerCornerRadius * (1f - progress)
    val shadowElevation = EXPAND_SHADOW_ELEVATION * (1f - progress)

    // 只缩放与裁切：纵深关系由底层页面的 cardExpandScrim 表达，
    // 这里若再叠一层暗色遮罩，会正好压暗用户视线聚焦的「正在生长」的内容。
    return this.graphicsLayer {
        this.scaleX = scaleX
        this.scaleY = scaleY
        transformOrigin = TransformOrigin(pivotX, pivotY)
        // 必须始终设置 shape：graphicsLayer 的 shadowElevation 只在图层具备 shape
        // （或非透明背景）时才会真正绘制阴影；圆角收敛到 0 时仍需保留矩形 shape，
        // 否则展开末段的边界阴影会凭空消失。
        shape = RoundedCornerShape(cornerRadius)
        clip = true
        this.shadowElevation = shadowElevation
    }
}

/**
 * 在页面内容之上叠加一层暗色遮罩。
 *
 * 用于底层页面：卡片展开/收回过程中，底层页面不位移，仅靠这层遮罩
 * 建立「顶层卡片浮在其上」的纵深关系，避免两个页面视觉上糊在一起。
 *
 * @param alpha 遮罩不透明度，0f 表示完全透明（不绘制）；越界值会被钳制。
 */
internal fun Modifier.cardExpandScrim(alpha: Float): Modifier {
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
 * 顶层页面收缩在卡片内时遮罩最深，铺满容器时完全消失；与缩放进度取自
 * 同一个展开度，两层页面的纵深关系才不会脱节。
 *
 * @param expansion 顶层页面的展开度，0f 时遮罩最强、1f 时完全消失。
 * @param maxAlpha 遮罩在最深时的最大不透明度，默认取 [BACKDROP_SCRIM_ALPHA]。
 */
internal fun cardExpandScrimAlpha(
    expansion: Float,
    maxAlpha: Float = BACKDROP_SCRIM_ALPHA,
): Float = maxAlpha * (1f - expansion.coerceIn(0f, 1f))

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
internal fun resolveCardExpandFrame(direction: Direction, factor: Float): CardExpandFrame =
    if (direction.isFront) {
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
