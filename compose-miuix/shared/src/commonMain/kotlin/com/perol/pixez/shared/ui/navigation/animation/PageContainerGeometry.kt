package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 页面容器的几何快照：卡片展开转场进行归一化与位移兜底所需的全部尺寸信息。
 *
 * 这三项历来作为一组参数在容器测量、转场动画器构造与预测性返回选择器之间传递，
 * 收敛为一个类型后，新增消费者只依赖这一个值，不必再逐个搬运裸 Rect / px / Dp。
 *
 * @param bounds 容器在窗口坐标系中的矩形，用于把卡片矩形归一化为容器内相对几何。
 * @param widthPx 容器物理像素宽度，用于平移兜底方案的全行程位移。
 * @param cornerRadius 设备屏幕物理圆角，收缩态下用于裁切顶层页面。
 */
@Immutable
data class PageContainerGeometry(
    val bounds: Rect = Rect.Zero,
    val widthPx: Float = 0f,
    val cornerRadius: Dp = 0.dp,
) {
    /** 容器已完成测量、几何可用于转场时为 true。 */
    val isMeasured: Boolean get() = bounds.width > 0f && bounds.height > 0f

    /** 仅替换窗口矩形，保留其余测量结果。 */
    fun withBounds(newBounds: Rect): PageContainerGeometry = copy(bounds = newBounds)
}
