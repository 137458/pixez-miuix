package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 让作品列表卡片把自己的窗口坐标矩形登记到 [LocalSharedBoundsRegistry]，
 * 并在该卡片处于「展开/收回」转场激活态时通过图层不透明度与顶层详情页做无重影交叉交接。
 *
 * 登记时机：卡片每次布局完成后刷新，保证滚动、分栏变化与窗口尺寸变化后
 * 详情页展开动画始终以卡片「当下」的真实位置为起点。
 *
 * @param illustId 卡片的作品 ID，与详情页配置一一对应。
 */
internal fun Modifier.illustTransitionBounds(illustId: Int): Modifier = composed {
    val registry = LocalSharedBoundsRegistry.current
    val currentIllustId by rememberUpdatedState(illustId)
    remember(registry, illustId) {
        Modifier
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) {
                    registry.put(currentIllustId, coordinates.boundsInWindow())
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
