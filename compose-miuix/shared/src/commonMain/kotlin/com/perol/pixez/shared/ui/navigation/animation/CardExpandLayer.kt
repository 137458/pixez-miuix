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
 * 进度 [fraction] 为 0 时页面完全收在卡片内（带圆角与阴影），为 1 时铺满容器且无变换。
 * [sourceBounds] 缺失或容器几何非法时直接返回原 [Modifier]，保证不引入任何副作用。
 *
 * @param expand true 表示播放「由小放大」（进入二级页），false 表示播放「由大收缩」（返回）。
 * @param fraction 转场进度，0f..1f。
 * @param sourceBounds 卡片窗口矩形，可为 null。
 * @param containerBounds 容器窗口矩形。
 * @param containerCornerRadius 设备屏幕物理圆角，收缩态下用于裁切顶层页面。
 */
internal fun Modifier.cardExpandLayer(
    expand: Boolean,
    fraction: Float,
    sourceBounds: Rect?,
    containerBounds: Rect,
    containerCornerRadius: Dp,
): Modifier {
    if (sourceBounds == null) return this
    val geometry = resolveCardExpandGeometry(sourceBounds, containerBounds) ?: return this

    // 展开与收回共用同一条 0->1 进度：展开时 0 是卡片、1 是整屏；收回时正好相反。
    val progress = if (expand) fraction else 1f - fraction

    val scaleX = lerp(geometry.scaleX, 1f, progress)
    val scaleY = lerp(geometry.scaleY, 1f, progress)
    val pivotX = lerp(geometry.pivotX, 0.5f, progress)
    val pivotY = lerp(geometry.pivotY, 0.5f, progress)
    val cornerRadius = containerCornerRadius * (1f - progress)
    val shadowElevation = EXPAND_SHADOW_ELEVATION * (1f - progress)
    val scrimAlpha = BACKDROP_SCRIM_ALPHA * (1f - progress)

    return this
        .graphicsLayer {
            this.scaleX = scaleX
            this.scaleY = scaleY
            transformOrigin = TransformOrigin(pivotX, pivotY)
            if (cornerRadius > 0.dp) {
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
            this.shadowElevation = shadowElevation
        }
        .drawWithContent {
            drawContent()
            if (scrimAlpha > 0.001f) {
                drawRect(Color.Black.copy(alpha = scrimAlpha))
            }
        }
}

/**
 * 在页面内容之上叠加一层随进度消退的暗色遮罩。
 *
 * 用于底层页面：卡片展开/收回过程中，底层页面不位移，仅靠这层遮罩
 * 建立「顶层卡片浮在其上」的纵深关系，避免两个页面视觉上糊在一起。
 *
 * @param progress 转场进度，0f 时遮罩最强、1f 时完全消失。
 * @param maxAlpha 遮罩在最深时的最大不透明度，默认取 [BACKDROP_SCRIM_ALPHA]。
 */
internal fun Modifier.cardExpandScrim(
    progress: Float,
    maxAlpha: Float = BACKDROP_SCRIM_ALPHA,
): Modifier {
    val scrimAlpha = maxAlpha * (1f - progress)
    return this.drawWithContent {
        drawContent()
        if (scrimAlpha > 0.001f) {
            drawRect(Color.Black.copy(alpha = scrimAlpha))
        }
    }
}

/** 线性插值。 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
