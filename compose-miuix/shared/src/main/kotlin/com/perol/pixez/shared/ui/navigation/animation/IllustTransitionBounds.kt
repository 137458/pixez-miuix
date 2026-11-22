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
import androidx.compose.ui.unit.dp

/**
 * 让作品列表卡片把自己的窗口坐标矩形与视觉圆角登记到 [LocalSharedBoundsRegistry]，
 * 并在该卡片处于「展开/收回」转场激活态时通过图层不透明度与顶层详情页做无重影交叉交接。
 */
internal fun Modifier.illustTransitionBounds(illustId: Long?, cornerRadius: Dp): Modifier =
    transitionTargetBounds(
        kind = TransitionTargetKind.Illust,
        id = illustId,
        cornerRadius = cornerRadius,
    )

/**
 * 让小说列表卡片把自己的窗口坐标矩形与视觉圆角登记到 [LocalSharedBoundsRegistry]，
 * 启用与插画一致的卡片展开/收回转场与预测性返回动画。
 */
internal fun Modifier.novelTransitionBounds(
    novelId: Long?,
    cornerRadius: Dp = 16.dp,
): Modifier = transitionTargetBounds(
    kind = TransitionTargetKind.Novel,
    id = novelId,
    cornerRadius = cornerRadius,
)

private fun Modifier.transitionTargetBounds(
    kind: TransitionTargetKind,
    id: Long?,
    cornerRadius: Dp,
): Modifier = composed {
    val registry = LocalSharedBoundsRegistry.current
    val currentId by rememberUpdatedState(id)
    val currentCornerRadius by rememberUpdatedState(cornerRadius)
    remember(registry, kind, id) {
        Modifier
            .onGloballyPositioned { coordinates ->
                val resolvedId = currentId
                if (resolvedId != null && resolvedId > 0 && coordinates.isAttached) {
                    registry.putTarget(
                        target = TransitionTarget(kind, resolvedId),
                        rect = coordinates.boundsInWindow(),
                        cornerRadiusDp = currentCornerRadius.value,
                    )
                }
            }
            .graphicsLayer {
                val resolvedId = currentId
                val active = registry.activeTransitionTarget
                this.alpha = if (
                    resolvedId != null &&
                    active != null &&
                    active.kind == kind &&
                    active.id == resolvedId &&
                    !registry.isExitingTransition
                ) {
                    cardExpandSourceCardAlpha(registry.activeTransitionExpansion)
                } else {
                    1f
                }
            }
    }
}
