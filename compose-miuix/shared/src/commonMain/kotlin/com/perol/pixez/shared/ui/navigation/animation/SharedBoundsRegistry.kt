package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private val bounds = mutableStateMapOf<Int, Rect>()

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

    /**
     * 登记某个作品卡片的窗口坐标矩形。
     *
     * @param illustId 作品 ID，与 [com.perol.pixez.shared.ui.navigation.RootComponent.Config.IllustDetail] 一一对应。
     * @param rect 卡片在窗口坐标系下的矩形；为空表示卡片已离开组合，移除登记。
     */
    fun put(illustId: Int, rect: Rect?) {
        if (rect == null || rect.width <= 0f || rect.height <= 0f) {
            bounds.remove(illustId)
        } else {
            bounds[illustId] = rect
        }
    }

    /**
     * 取出某个作品卡片最近一次登记的窗口坐标矩形。
     *
     * @param illustId 作品 ID。
     * @return 命中则返回矩形，否则返回 null（调用方应回退为默认转场）。
     */
    fun get(illustId: Int): Rect? = bounds[illustId]

    /**
     * 取出卡片在当前容器可视区域内的有效矩形；
     * 若卡片大部分（超过 75%）已滚出容器可视范围则返回 null，使转场优雅回退为视差侧滑。
     */
    fun getVisibleInContainer(illustId: Int, containerBounds: Rect): Rect? {
        val rect = bounds[illustId] ?: return null
        if (rect.width <= 0f || rect.height <= 0f) return null
        if (containerBounds.width <= 0f || containerBounds.height <= 0f) return rect

        val intersectLeft = maxOf(rect.left, containerBounds.left)
        val intersectTop = maxOf(rect.top, containerBounds.top)
        val intersectRight = minOf(rect.right, containerBounds.right)
        val intersectBottom = minOf(rect.bottom, containerBounds.bottom)

        val visibleWidth = (intersectRight - intersectLeft).coerceAtLeast(0f)
        val visibleHeight = (intersectBottom - intersectTop).coerceAtLeast(0f)

        val widthVisibleRatio = visibleWidth / rect.width
        val heightVisibleRatio = visibleHeight / rect.height

        return rect.takeIf { widthVisibleRatio >= MIN_VISIBLE_RATIO && heightVisibleRatio >= MIN_VISIBLE_RATIO }
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
     */
    fun updateTransitionState(illustId: Int?, expansion: Float) {
        val clamped = expansion.coerceIn(0f, 1f)
        if (illustId == null || clamped <= 0.001f || clamped >= 0.999f) {
            activeTransitionIllustId = null
            activeTransitionExpansion = 0f
        } else {
            activeTransitionIllustId = illustId
            activeTransitionExpansion = clamped
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
    }

    private companion object {
        /** 卡片在容器内仍可执行展开/收回动画的最小可见宽高比例。 */
        const val MIN_VISIBLE_RATIO = 0.25f
    }
}

/**
 * 提供当前组合树使用的 [SharedBoundsRegistry]。
 *
 * 默认值为一个游离实例，保证在未显式提供（如预览、测试）时调用方不会崩溃，
 * 只是永远取不到卡片矩形、从而回退为默认转场。
 */
val LocalSharedBoundsRegistry = compositionLocalOf { SharedBoundsRegistry() }
