package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * 作品列表卡片到作品详情页「卡片展开式」转场的几何信息源。
 *
 * 列表中的作品卡片在布局完成后把自己的窗口坐标矩形登记进来，
 * 转场动画据此把详情页从卡片位置放大展开、返回时收回卡片；
 * 未登记（如分享链接直达、进程重建恢复）或已滚出容器可视范围时回退为视差侧滑。
 */
@Stable
class SharedBoundsRegistry {

    private val bounds = mutableStateMapOf<Int, IllustCardBounds>()

    /**
     * 当前详情页实际展示的作品 ID（支持详情页内左右滑动切换作品后按真实展示的作品定位收回卡片）。
     */
    var activeDetailIllustId: Int? by mutableStateOf(null)

    /**
     * 当前正在执行卡片展开/收回转场的目标作品 ID。
     */
    var activeTransitionIllustId: Int? by mutableStateOf(null)
        private set

    /**
     * 当前正在执行的卡片展开度（0f..1f），用于驱动列表源卡片交叉淡出避免分身重影。
     */
    var activeTransitionExpansion: Float by mutableFloatStateOf(0f)
        private set

    /** 转场激活期底层纵深缩放的窗口坐标锚点（= 源卡片中心）。 */
    private var backdropPivot = Offset.Zero

    /** 转场激活期底层纵深缩放的当前比例；非激活态保持 1f。 */
    private var backdropScale = 1f

    /**
     * 登记某个作品卡片的窗口坐标矩形与卡片圆角。
     *
     * @param illustId 作品 ID，与 [com.perol.pixez.shared.ui.navigation.RootComponent.Config.IllustDetail] 一一对应。
     * @param rect 卡片在窗口坐标系下的矩形；为空表示卡片已离开组合，移除登记。
     * @param cornerRadiusDp 卡片自身视觉圆角（dp），转场收回时按它对齐终点圆角。
     */
    fun put(illustId: Int, rect: Rect?, cornerRadiusDp: Float = DEFAULT_CARD_CORNER_RADIUS_DP) {
        if (rect == null || rect.width <= 0f || rect.height <= 0f) {
            bounds.remove(illustId)
            return
        }
        // 转场进行中底层列表处于 graphicsLayer 缩放态，此时 boundsInWindow() 为缩放偏移后的瞬时坐标：
        // 已有静止态真实坐标的卡片禁止覆盖；首次登记的卡片则按当前纵深缩放逆变换归一为静止态坐标，
        // 否则污染坐标会驻留到下次真实重排，导致退出动画终点跳变。
        if (activeTransitionIllustId != null) {
            if (bounds.containsKey(illustId)) {
                return
            }
            bounds[illustId] = IllustCardBounds(rect = rect.toStationaryBounds(), cornerRadiusDp = cornerRadiusDp)
            return
        }
        bounds[illustId] = IllustCardBounds(rect = rect, cornerRadiusDp = cornerRadiusDp)
    }

    /**
     * 取出某个作品卡片最近一次登记的窗口坐标矩形。
     *
     * @param illustId 作品 ID。
     * @return 命中则返回矩形，否则返回 null（调用方应回退为默认转场）。
     */
    fun get(illustId: Int): Rect? = bounds[illustId]?.rect

    /**
     * 取出卡片在当前容器可视区域内的有效登记；
     * 若卡片大部分（超过 75%）已滚出容器可视范围则返回 null，使转场优雅回退为视差侧滑。
     */
    fun getVisibleInContainer(illustId: Int, containerBounds: Rect): IllustCardBounds? {
        val card = bounds[illustId] ?: return null
        val rect = card.rect
        if (rect.width <= 0f || rect.height <= 0f) return null
        if (containerBounds.width <= 0f || containerBounds.height <= 0f) return card

        val intersectLeft = maxOf(rect.left, containerBounds.left)
        val intersectTop = maxOf(rect.top, containerBounds.top)
        val intersectRight = minOf(rect.right, containerBounds.right)
        val intersectBottom = minOf(rect.bottom, containerBounds.bottom)

        val visibleWidth = (intersectRight - intersectLeft).coerceAtLeast(0f)
        val visibleHeight = (intersectBottom - intersectTop).coerceAtLeast(0f)

        val widthVisibleRatio = visibleWidth / rect.width
        val heightVisibleRatio = visibleHeight / rect.height

        return card.takeIf { widthVisibleRatio >= MIN_VISIBLE_RATIO && heightVisibleRatio >= MIN_VISIBLE_RATIO }
    }

    /**
     * 解析转场应该使用的有效作品 ID（优先取详情页内滑动切换后的当前作品 ID）。
     */
    fun resolveEffectiveIllustId(routeIllustId: Int?): Int? {
        if (routeIllustId == null) return null
        return activeDetailIllustId ?: routeIllustId
    }

    /**
     * 更新当前转场的激活状态；当展开度到达端点（0 或 1）时自动释放激活标记。
     *
     * 激活期间同时记录底层页面的纵深缩放状态（锚点 = 源卡片中心、比例 = [BACKDROP_MIN_SCALE]
     * 随展开度插值），供 [put] 把转场中新登记的瞬时坐标逆变换回静止态真实坐标。
     *
     * @param sourceBounds 发起转场的源卡片静止态矩形，用于推算底层纵深缩放锚点。
     * @param containerBounds 页面容器窗口矩形，用于把锚点换算到窗口坐标系。
     */
    fun updateTransitionState(
        illustId: Int?,
        expansion: Float,
        sourceBounds: Rect? = null,
        containerBounds: Rect = Rect.Zero,
    ) {
        val clamped = expansion.coerceIn(0f, 1f)
        if (illustId == null || clamped <= 0.001f || clamped >= 0.999f) {
            activeTransitionIllustId = null
            activeTransitionExpansion = 0f
        } else {
            activeTransitionIllustId = illustId
            activeTransitionExpansion = clamped
            if (sourceBounds != null && containerBounds.width > 0f && containerBounds.height > 0f) {
                backdropPivot = sourceBounds.center
                backdropScale = lerp(1f, BACKDROP_MIN_SCALE, clamped)
            } else {
                backdropScale = 1f
            }
        }
    }

    /**
     * 移除某个作品卡片的登记，避免已销毁页面的陈旧几何信息被复用。
     */
    fun remove(illustId: Int) {
        bounds.remove(illustId)
    }

    /**
     * 清空全部登记（用于整栈重置等场景）。
     */
    fun clear() {
        bounds.clear()
        activeDetailIllustId = null
        activeTransitionIllustId = null
        activeTransitionExpansion = 0f
        backdropScale = 1f
    }

    /**
     * 把转场激活期上报的瞬时坐标按底层纵深缩放逆变换回静止态坐标。
     *
     * 底层页面围绕源卡片中心（窗口坐标 [backdropPivot]）以 [backdropScale] 缩放，
     * 观测坐标 q 与静止坐标 p 满足 q = pivot + (p - pivot) * scale，反解即得 p。
     */
    private fun Rect.toStationaryBounds(): Rect {
        if (backdropScale >= 1f || backdropScale <= 0f) return this
        val pivot = backdropPivot
        return Rect(
            left = pivot.x + (left - pivot.x) / backdropScale,
            top = pivot.y + (top - pivot.y) / backdropScale,
            right = pivot.x + (right - pivot.x) / backdropScale,
            bottom = pivot.y + (bottom - pivot.y) / backdropScale,
        )
    }

    private companion object {
        /** 卡片在容器内仍可执行展开/收回动画的最小可见宽高比例。 */
        const val MIN_VISIBLE_RATIO = 0.25f
    }
}

/** 未显式上报圆角时的卡片默认圆角（dp），与 MIUIX Card 默认圆角一致。 */
internal const val DEFAULT_CARD_CORNER_RADIUS_DP = 16f

/** 线性插值。 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/**
 * 一次卡片展开/收回转场所依据的单张卡片静止态几何。
 *
 * @param rect 卡片在窗口坐标系下的静止态矩形。
 * @param cornerRadiusDp 卡片自身视觉圆角（dp）：列表卡片 16dp（MIUIX Card 默认）、
 * 浏览历史卡片 12dp，转场收回终点按各自圆角做像素级对齐。
 */
@Stable
data class IllustCardBounds(
    val rect: Rect,
    val cornerRadiusDp: Float,
)

/**
 * 提供当前组合树使用的 [SharedBoundsRegistry]。
 *
 * 默认值为一个游离实例，保证在未显式提供（如预览、测试）时调用方不会崩溃，
 * 只是永远取不到卡片矩形、从而回退为默认转场。
 */
val LocalSharedBoundsRegistry = compositionLocalOf { SharedBoundsRegistry() }
