package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect

/**
 * 作品列表卡片到作品详情页「卡片展开式」转场的几何信息源。
 *
 * 列表中的作品卡片在布局完成后把自己的窗口坐标矩形登记进来，
 * 转场动画据此把详情页从卡片位置放大展开、返回时收回卡片；
 * 未登记（如分享链接直达、进程重建恢复）时回退为纯左右平移。
 */
@Stable
class SharedBoundsRegistry {

    private val bounds = mutableStateMapOf<Int, Rect>()

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
    }
}

/**
 * 提供当前组合树使用的 [SharedBoundsRegistry]。
 *
 * 默认值为一个游离实例，保证在未显式提供（如预览、测试）时调用方不会崩溃，
 * 只是永远取不到卡片矩形、从而回退为默认转场。
 */
val LocalSharedBoundsRegistry = compositionLocalOf { SharedBoundsRegistry() }
