package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp

/**
 * 让作品列表卡片把自己的窗口坐标矩形与视觉圆角登记到 [LocalSharedBoundsRegistry]，
 * 并在该卡片处于「展开/收回」转场激活态时通过图层不透明度与顶层详情页做无重影交叉交接。
 *
 * 登记时机：卡片每次布局完成后刷新，保证滚动、分栏变化与窗口尺寸变化后
 * 详情页展开动画始终以卡片「当下」的真实位置为起点。
 * 转场激活期新进入组合的卡片会由 [SharedBoundsRegistry] 按底层纵深缩放逆变换归一，
 * 此处只负责如实上报当前观测坐标。
 *
 * @param illustId 卡片的作品 ID，与详情页配置一一对应；为 null 或非正数时不登记。
 * @param cornerRadius 卡片自身视觉圆角，转场收回终点圆角按它做像素级对齐。
 */
internal fun Modifier.illustTransitionBounds(illustId: Int?, cornerRadius: Dp): Modifier = composed {
    val registry = LocalSharedBoundsRegistry.current
    val currentIllustId by rememberUpdatedState(illustId)
    val currentCornerRadius by rememberUpdatedState(cornerRadius)
    remember(registry, illustId) {
        Modifier
            .onGloballyPositioned { coordinates ->
                val id = currentIllustId
                if (id != null && id > 0 && coordinates.isAttached) {
                    registry.put(id, coordinates.boundsInWindow(), currentCornerRadius.value)
                }
            }
            .graphicsLayer {
                this.alpha = if (registry.activeTransitionIllustId == currentIllustId) {
                    cardExpandSourceCardAlpha(registry.activeTransitionExpansion)
                } else {
                    1f
                }
            }
    }
}
