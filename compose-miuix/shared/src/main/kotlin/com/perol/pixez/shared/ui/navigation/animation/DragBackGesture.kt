package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.backhandler.BackEvent
import kotlin.math.abs

/** 应用内拖拽返回的松手判定结果。 */
internal enum class DragBackOutcome { COMMIT, CANCEL }

/** 松手确认返回的最小拖拽进度（占屏宽比例）。 */
internal const val DRAG_BACK_COMMIT_PROGRESS = 0.35f

/** 松手确认返回的最小横向速度（px/s，越阈值即使用位移不足也确认，对齐 fling 手感）。 */
internal const val DRAG_BACK_COMMIT_VELOCITY_PX_PER_SEC = 2000f

/** 起手边缘感应区宽度（LTR 为物理左缘，RTL 为物理右缘）。 */
internal val DRAG_BACK_EDGE_ZONE: Dp = 24.dp

/** 模态页顶部下滑解散的起手感应区高度（覆盖状态栏与顶栏抓手区域）。 */
internal val MODAL_DISMISS_TOP_ZONE: Dp = 112.dp

/** 模态页下拉松手确认返回的最小进度（占屏高比例）与最小速度（px/s）。 */
internal const val MODAL_DISMISS_COMMIT_PROGRESS = 0.22f
internal const val MODAL_DISMISS_COMMIT_VELOCITY_PX_PER_SEC = 1400f

/**
 * 松手结果判定：进度过阈值或速度过阈值即确认返回，否则回弹。
 */
internal fun resolveDragBackOutcome(progress: Float, velocityPxPerSec: Float): DragBackOutcome =
    when {
        progress >= DRAG_BACK_COMMIT_PROGRESS -> DragBackOutcome.COMMIT
        velocityPxPerSec >= DRAG_BACK_COMMIT_VELOCITY_PX_PER_SEC -> DragBackOutcome.COMMIT
        else -> DragBackOutcome.CANCEL
    }

/** 模态页顶部下拉解散的松手判定：位移达屏高 22% 或向下甩动达 1400 px/s 即关闭。 */
internal fun resolveModalDismissOutcome(progress: Float, velocityYPxPerSec: Float): DragBackOutcome =
    when {
        progress >= MODAL_DISMISS_COMMIT_PROGRESS -> DragBackOutcome.COMMIT
        velocityYPxPerSec >= MODAL_DISMISS_COMMIT_VELOCITY_PX_PER_SEC -> DragBackOutcome.COMMIT
        else -> DragBackOutcome.CANCEL
    }

/** 计算给定布局方向下边缘拖拽返回的归一化进度（0..1）。 */
internal fun resolveDragBackProgress(
    downX: Float,
    currentX: Float,
    containerWidthPx: Float,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): Float {
    val width = containerWidthPx.takeIf { it > 0f } ?: 1f
    val dir = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
    return (((currentX - downX) * dir) / width).coerceIn(0f, 1f)
}

/** 计算给定布局方向下带符号的归一化速度比例（`> 0` 朝返回完成方向，`<= 0` 朝取消回弹方向）。 */
internal fun resolveDragBackSignedVelocityFraction(
    velocityX: Float,
    containerWidthPx: Float,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): Float {
    val width = containerWidthPx.takeIf { it > 0f } ?: 1f
    val dir = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
    return (velocityX * dir) / width
}

/**
 * 应用内拖拽线性关页手势：LTR 由屏幕左缘向右、RTL 由屏幕右缘向左拖拽驱动页面收回进度。
 *
 * 事件语义与系统预测性返回对齐：起手 → [onStart]（BackEvent progress=0）、
 * 拖动 → [onProgress]（progress 1:1 随手指）、松手 → 按位移/速度阈值 [onCommit] 或 [onCancel]。
 * 与系统手势事件流的互斥由 [MiuixPredictiveBackGestureHandler] 状态机保证。
 *
 * 本 modifier 为纯交互 UI（TDD 豁免），阈值与方向计算逻辑见 [resolveDragBackOutcome] / [resolveDragBackProgress]（有单测）。
 */
@ExperimentalDecomposeApi
internal fun Modifier.miuixDragBackGesture(
    key: Any = Unit,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    isEligible: () -> Boolean,
    onStart: (BackEvent) -> Unit,
    onProgress: (BackEvent) -> Unit,
    onCommit: (velocityFractionPerSec: Float) -> Unit,
    onCancel: (velocityFractionPerSec: Float) -> Unit,
): Modifier = pointerInput(key, layoutDirection) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val dir = if (isRtl) -1f else 1f
    val swipeEdge = if (isRtl) BackEvent.SwipeEdge.RIGHT else BackEvent.SwipeEdge.LEFT

    awaitEachGesture {
        // 全程 Initial pass：父层先于子层（pager/列表）看到事件。激活后消费事件即可
        // 拦截子层手势（Main pass 的 requireUnconsumed 检测会失败）；激活前不消费，
        // 列表滚动与 pager 翻页不受任何影响。
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!isEligible()) return@awaitEachGesture
        val edgeZonePx = DRAG_BACK_EDGE_ZONE.toPx()
        val widthPx = size.width.toFloat()
        val inStartEdge = if (isRtl) down.position.x >= widthPx - edgeZonePx else down.position.x <= edgeZonePx
        if (!inStartEdge) return@awaitEachGesture

        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var activated = false
        var settled = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (activated) {
                        val width = size.width.toFloat().takeIf { it > 0f } ?: 1f
                        val velocityX = tracker.calculateVelocity().x
                        val directedVelocityX = velocityX * dir
                        val outcome = resolveDragBackOutcome(
                            progress = resolveDragBackProgress(down.position.x, change.position.x, width, layoutDirection),
                            velocityPxPerSec = directedVelocityX,
                        )
                        val signedVelocityFraction = directedVelocityX / width
                        settled = true
                        when (outcome) {
                            DragBackOutcome.COMMIT -> onCommit(signedVelocityFraction)
                            DragBackOutcome.CANCEL -> onCancel(signedVelocityFraction)
                        }
                    }
                    break
                }
                if (!activated) {
                    if (isVerticalIntent(down.position, change.position, viewConfiguration.touchSlop)) {
                        return@awaitEachGesture
                    }
                    if ((change.position.x - down.position.x) * dir <= viewConfiguration.touchSlop) {
                        tracker.addPosition(change.uptimeMillis, change.position)
                        continue
                    }
                    activated = true
                    onStart(BackEvent(progress = 0f, swipeEdge = swipeEdge))
                }
                tracker.addPosition(change.uptimeMillis, change.position)
                onProgress(
                    BackEvent(
                        progress = resolveDragBackProgress(
                            down.position.x,
                            change.position.x,
                            size.width.toFloat(),
                            layoutDirection,
                        ),
                        swipeEdge = swipeEdge,
                    ),
                )
                event.changes.forEach { it.consume() }
            }
        } finally {
            // 指针异常取消/丢失（非正常抬手结算）时必须复位状态机，避免覆盖层永久吞掉全部输入。
            if (activated && !settled) {
                onCancel(0f)
            }
        }
    }
}

/**
 * 模态页顶部下滑解散手势：从顶栏及状态栏抓手区域 [MODAL_DISMISS_TOP_ZONE] 内向下滑动，
 * 以与系统预测性返回共享的状态机驱动模态页 1:1 跟手下滑与释放速度交接。
 */
@ExperimentalDecomposeApi
internal fun Modifier.miuixModalSwipeDismissGesture(
    key: Any = Unit,
    isEligible: () -> Boolean,
    onStart: (BackEvent) -> Unit,
    onProgress: (BackEvent) -> Unit,
    onCommit: (velocityFractionPerSec: Float) -> Unit,
    onCancel: (velocityFractionPerSec: Float) -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!isEligible()) return@awaitEachGesture
        val topZonePx = MODAL_DISMISS_TOP_ZONE.toPx()
        if (down.position.y > topZonePx) return@awaitEachGesture

        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var activated = false
        var settled = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (activated) {
                        val height = size.height.toFloat().takeIf { it > 0f } ?: 1f
                        val velocityY = tracker.calculateVelocity().y
                        val outcome = resolveModalDismissOutcome(
                            progress = modalDraggedProgress(down.position, change.position, height),
                            velocityYPxPerSec = velocityY,
                        )
                        val signedVelocityFraction = velocityY / height
                        settled = true
                        when (outcome) {
                            DragBackOutcome.COMMIT -> onCommit(signedVelocityFraction)
                            DragBackOutcome.CANCEL -> onCancel(signedVelocityFraction)
                        }
                    }
                    break
                }
                if (!activated) {
                    val dx = abs(change.position.x - down.position.x)
                    val dy = change.position.y - down.position.y
                    if (dx > abs(dy) && dx > viewConfiguration.touchSlop) {
                        return@awaitEachGesture
                    }
                    if (dy <= viewConfiguration.touchSlop) {
                        tracker.addPosition(change.uptimeMillis, change.position)
                        continue
                    }
                    activated = true
                    onStart(BackEvent(progress = 0f, swipeEdge = BackEvent.SwipeEdge.LEFT))
                }
                tracker.addPosition(change.uptimeMillis, change.position)
                onProgress(
                    BackEvent(
                        progress = modalDraggedProgress(down.position, change.position, size.height.toFloat()),
                        swipeEdge = BackEvent.SwipeEdge.LEFT,
                    ),
                )
                event.changes.forEach { it.consume() }
            }
        } finally {
            if (activated && !settled) {
                onCancel(0f)
            }
        }
    }
}

private fun isVerticalIntent(downPosition: Offset, currentPosition: Offset, touchSlop: Float): Boolean {
    val dx = abs(currentPosition.x - downPosition.x)
    val dy = abs(currentPosition.y - downPosition.y)
    return dy > dx && dy > touchSlop
}

private fun modalDraggedProgress(downPosition: Offset, currentPosition: Offset, containerHeightPx: Float): Float {
    val height = containerHeightPx.takeIf { it > 0f } ?: 1f
    return ((currentPosition.y - downPosition.y) / height).coerceIn(0f, 1f)
}
