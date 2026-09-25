package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator

/**
 * MIUIX / HyperOS 二级页面转场的统一时长（毫秒）。
 */
private const val TRANSITION_DURATION_MILLIS = 340

/**
 * 构造 MIUIX / HyperOS 统一的二级页面转场动画器。
 *
 * 顶层页面被视为一张从作品列表卡片「生长」出来的卡片：打开详情页时按卡片矩形
 * 的尺寸与位置缩放展开，关闭时反向收缩归位；圆角由设备屏幕物理圆角
 * containerCornerRadius 渐变到 0，形成连贯的空间连续性。
 *
 * [sourceBounds] 为 null 时（画师页、设置页这类本就没有来源卡片的二级页面，
 * 以及分享链接直达、进程重建恢复等入口）退化为 MIUIX 经典左右平移，
 * 保证「所有二级页面都有统一出入场动效」这一目标不出现空白。
 *
 * 两种方案共用同一时长与缓动曲线，切换路径时不会出现手感断层。
 *
 * 注意：本函数不是 @Composable —— stackAnimation 的动画器工厂在合成作用域外调用，
 * 所有插值都在 stackAnimator 内部按帧驱动。
 *
 * @param sourceBounds 发起转场的卡片窗口矩形，为 null 时使用平移兜底。
 * @param containerBounds 页面容器自身的窗口矩形，用于归一化卡片几何。
 * @param containerCornerRadius 设备屏幕物理圆角，收缩态下用于裁切顶层页面。
 * @return 可直接交给 stackAnimation 使用的 [StackAnimator]。
 */
internal fun cardExpandStackAnimator(
    sourceBounds: Rect?,
    containerBounds: Rect,
    containerCornerRadius: Dp,
): StackAnimator {
    val duration: FiniteAnimationSpec<Float> = tween(
        durationMillis = TRANSITION_DURATION_MILLIS,
        easing = HyperOSDecelerateEasing,
    )
    if (sourceBounds == null) {
        // 无来源卡片：复用 Decompose 官方平移实现，避免自维护一套位移推导。
        return slide(animationSpec = duration)
    }

    return stackAnimator(animationSpec = duration) { factor, direction, content ->
        val frame = resolveCardExpandFrame(direction = direction, factor = factor)
        content(
            if (frame.isTopLayer) {
                // 发起转场的作品详情页：按卡片展开度缩放、裁圆角并投出边界阴影。
                Modifier.cardExpandLayer(
                    expansion = frame.expansion,
                    sourceBounds = sourceBounds,
                    containerBounds = containerBounds,
                    containerCornerRadius = containerCornerRadius,
                )
            } else {
                // 被覆盖的作品列表：不位移，只按同一展开度叠加消退遮罩建立纵深。
                Modifier.cardExpandScrim(alpha = cardExpandScrimAlpha(frame.expansion))
            },
        )
    }
}
