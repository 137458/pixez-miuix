package com.perol.pixez.shared.ui.navigation.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min

/**
 * 当前屏幕/主容器的转场安全可视区域状态。
 *
 * 用于记录未被顶栏（TopBar）、底栏（BottomBar）或侧边导航栏（NavigationRail）遮挡的内容可视区域，
 * 避免从详情页返回列表卡片时转场画面在尾帧盖住应用栏按钮。
 */
@Stable
internal class TransitionViewportState(
    private val parent: TransitionViewportState? = null,
) {
    var containerBounds by mutableStateOf<Rect?>(null)
    var topOccluderBottomPx by mutableStateOf<Float?>(null)
    var bottomOccluderTopPx by mutableStateOf<Float?>(null)

    fun resolveContainerBounds(): Rect? = containerBounds ?: parent?.resolveContainerBounds()

    fun resolveTopOccluderBottomPx(): Float? =
        topOccluderBottomPx ?: parent?.resolveTopOccluderBottomPx()

    fun resolveBottomOccluderTopPx(): Float? =
        bottomOccluderTopPx ?: parent?.resolveBottomOccluderTopPx()

    fun resolveSafeViewport(
        fallbackWidth: Float,
        fallbackHeight: Float,
    ): Rect? = computeSafeViewportBounds(
        containerBounds = resolveContainerBounds(),
        rootWidth = fallbackWidth,
        rootHeight = fallbackHeight,
        topOccluderBottomPx = resolveTopOccluderBottomPx(),
        bottomOccluderTopPx = resolveBottomOccluderTopPx(),
    )
}

internal val LocalTransitionViewportState = compositionLocalOf<TransitionViewportState?> { null }

/**
 * 为当前屏幕骨架创建独立的 [TransitionViewportState] 作用域，隔离同级页签或堆栈页面的顶栏高度。
 */
@Composable
internal fun ProvideTransitionViewportScope(
    content: @Composable () -> Unit,
) {
    val parent = LocalTransitionViewportState.current
    val state = remember(parent) { TransitionViewportState(parent = parent) }
    CompositionLocalProvider(LocalTransitionViewportState provides state, content = content)
}

/**
 * 标记主内容容器边界（已排除桌面/横屏模式下的侧边 NavigationRail）。
 */
@Composable
internal fun Modifier.transitionViewportContainer(): Modifier {
    val state = LocalTransitionViewportState.current ?: return this
    DisposableEffect(state) {
        onDispose { state.containerBounds = null }
    }
    return onGloballyPositioned { coordinates ->
        val next = if (coordinates.isAttached) coordinates.boundsInWindow() else null
        if (state.containerBounds != next) {
            state.containerBounds = next
        }
    }
}

/**
 * 标记会遮挡滚动列表内容的顶栏区域（取其窗口底部 Y 作为安全区上界）。
 */
@Composable
internal fun Modifier.transitionTopBarOccluder(): Modifier {
    val state = LocalTransitionViewportState.current ?: return this
    DisposableEffect(state) {
        onDispose { state.topOccluderBottomPx = null }
    }
    return onGloballyPositioned { coordinates ->
        val next = if (coordinates.isAttached) coordinates.boundsInWindow().bottom else null
        if (state.topOccluderBottomPx != next) {
            state.topOccluderBottomPx = next
        }
    }
}

/**
 * 标记会遮挡滚动列表内容的底栏区域（取其窗口顶部 Y 作为安全区下界）。
 */
@Composable
internal fun Modifier.transitionBottomBarOccluder(): Modifier {
    val state = LocalTransitionViewportState.current ?: return this
    DisposableEffect(state) {
        onDispose { state.bottomOccluderTopPx = null }
    }
    return onGloballyPositioned { coordinates ->
        val next = if (coordinates.isAttached) coordinates.boundsInWindow().top else null
        if (state.bottomOccluderTopPx != next) {
            state.bottomOccluderTopPx = next
        }
    }
}

/**
 * 根据容器边界、窗口尺寸与顶/底栏遮挡边界计算未受遮挡的内容安全视口（窗口坐标系）。
 */
internal fun computeSafeViewportBounds(
    containerBounds: Rect?,
    rootWidth: Float,
    rootHeight: Float,
    topOccluderBottomPx: Float?,
    bottomOccluderTopPx: Float?,
): Rect? {
    val base = containerBounds ?: if (rootWidth > 1f && rootHeight > 1f) {
        Rect(0f, 0f, rootWidth, rootHeight)
    } else {
        return null
    }
    val maxRight = if (rootWidth > 1f) min(base.right, rootWidth) else base.right
    val maxBottom = if (rootHeight > 1f) min(base.bottom, rootHeight) else base.bottom
    val left = max(0f, base.left)
    var top = max(0f, base.top)
    var bottom = maxBottom
    if (topOccluderBottomPx != null && topOccluderBottomPx.isFinite()) {
        top = max(top, topOccluderBottomPx)
    }
    if (bottomOccluderTopPx != null && bottomOccluderTopPx.isFinite()) {
        bottom = min(bottom, bottomOccluderTopPx)
    }
    val clamped = Rect(left = left, top = top, right = maxRight, bottom = bottom)
    return clamped.takeIf { it.width > 1f && it.height > 1f }
}

/**
 * 判断卡片是否完整落在未被顶/底栏与侧边栏遮挡的安全视口内。
 */
internal fun isRectContainedInViewport(
    rect: Rect,
    viewport: Rect,
    tolerancePx: Float = 1f,
): Boolean {
    if (rect.width <= 1f || rect.height <= 1f) return false
    if (viewport.width <= 1f || viewport.height <= 1f) return false
    return rect.left >= viewport.left - tolerancePx &&
        rect.top >= viewport.top - tolerancePx &&
        rect.right <= viewport.right + tolerancePx &&
        rect.bottom <= viewport.bottom + tolerancePx
}

/**
 * 计算应用内 [cardExpandLayer] 在容器坐标系之前应使用的外层安全裁剪窗口矩形。
 *
 * 在 `expansion <= minEdgeProgress` 时已完全收束至 [safeViewport]，确保卡片淡出交接前
 * 不会有边缘探出到 TopBar / BottomBar 之上；在 `expansion >= 0.999f` 时返回 `null` 以保持全屏展开。
 */
internal fun resolveCardExpandViewportClip(
    safeViewport: Rect?,
    containerBounds: Rect,
    expansion: Float,
    minEdgeProgress: Float = 0.05f,
): Rect? {
    safeViewport ?: return null
    val progress = expansion.coerceIn(0f, 1f)
    if (progress >= 0.999f) return null
    val clampedSafe = safeViewport.intersect(containerBounds)
    if (clampedSafe.width <= 1f || clampedSafe.height <= 1f) return null
    val denominator = (1f - minEdgeProgress).coerceAtLeast(0.001f)
    val t = ((progress - minEdgeProgress) / denominator).coerceIn(0f, 1f)
    val clipped = Rect(
        left = lerp(clampedSafe.left, containerBounds.left, t),
        top = lerp(clampedSafe.top, containerBounds.top, t),
        right = lerp(clampedSafe.right, containerBounds.right, t),
        bottom = lerp(clampedSafe.bottom, containerBounds.bottom, t),
    )
    return clipped.takeIf { it.width > 1f && it.height > 1f }
}
