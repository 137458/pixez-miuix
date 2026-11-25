package com.perol.pixez.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.util.lerp
import com.perol.pixez.shared.ui.navigation.animation.HyperOSDecelerateEasing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val FULLSCREEN_OPEN_DURATION_MS = 260
private const val FULLSCREEN_CLOSE_DURATION_MS = 220
private const val FULLSCREEN_CANCEL_DURATION_MS = 200
private const val VIEWER_DISMISS_DOWN_RATIO = 0.14f
private const val VIEWER_DISMISS_UP_RATIO = 0.22f
private const val VIEWER_DISMISS_DOWN_VELOCITY = 1100f
private const val VIEWER_DISMISS_ANY_VELOCITY = 1800f

/** 记录详情页每页图片的窗口坐标矩形，供全屏看图器原位展开与回落。 */
@Stable
class DetailImageBoundsTracker internal constructor() {
    private val boundsByPage = mutableMapOf<Int, Rect>()
    var viewportBounds: Rect = Rect.Zero

    fun updatePageBounds(pageIndex: Int, rect: Rect) {
        boundsByPage[pageIndex] = rect
    }

    fun removePageBounds(pageIndex: Int) {
        boundsByPage.remove(pageIndex)
    }

    fun visibleBoundsFor(pageIndex: Int): Rect? {
        val rect = boundsByPage[pageIndex] ?: return null
        if (rect.width <= 1f || rect.height <= 1f) return null
        val vp = viewportBounds
        if (vp.width > 1f && vp.height > 1f) {
            val overlaps = rect.right > vp.left + 1f &&
                rect.left < vp.right - 1f &&
                rect.bottom > vp.top + 1f &&
                rect.top < vp.bottom - 1f
            if (!overlaps) return null
        }
        return rect
    }
}

/** 标记详情页图片元素的窗口坐标以供全屏看图器锚定。 */
@Composable
internal fun Modifier.detailImageBounds(
    tracker: DetailImageBoundsTracker?,
    pageIndex: Int,
): Modifier {
    if (tracker == null) return this
    DisposableEffect(tracker, pageIndex) {
        onDispose { tracker.removePageBounds(pageIndex) }
    }
    return onGloballyPositioned { coordinates ->
        if (coordinates.isAttached && coordinates.size.width > 0 && coordinates.size.height > 0) {
            val topLeft = coordinates.localToWindow(Offset.Zero)
            val bottomRight = coordinates.localToWindow(
                Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
            )
            val unclipped = Rect(topLeft, bottomRight)
            if (unclipped.width > 1f && unclipped.height > 1f) {
                tracker.updatePageBounds(pageIndex, unclipped)
            }
        }
    }
}

internal val LocalFullScreenViewerZoomedState = compositionLocalOf<MutableState<Boolean>?> { null }

internal enum class ViewerDismissOutcome { COMMIT, CANCEL }

/** 全屏看图器单帧几何与透明参数。 */
internal data class FullScreenViewerTransform(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val cornerRadiusPx: Float = 0f,
    val backgroundAlpha: Float = 1f,
    val contentAlpha: Float = 1f,
    val controlsAlpha: Float = 1f,
)

/** 计算全屏看图器纵向拖拽松手是否应触发关闭。 */
internal fun resolveViewerDismissOutcome(
    dragOffsetY: Float,
    velocityY: Float,
    containerHeightPx: Float,
): ViewerDismissOutcome {
    val height = containerHeightPx.takeIf { it > 0f } ?: 1000f
    return when {
        dragOffsetY >= height * VIEWER_DISMISS_DOWN_RATIO -> ViewerDismissOutcome.COMMIT
        dragOffsetY > 0f && velocityY >= VIEWER_DISMISS_DOWN_VELOCITY -> ViewerDismissOutcome.COMMIT
        abs(dragOffsetY) >= height * VIEWER_DISMISS_UP_RATIO -> ViewerDismissOutcome.COMMIT
        abs(velocityY) >= VIEWER_DISMISS_ANY_VELOCITY && abs(dragOffsetY) > 24f -> ViewerDismissOutcome.COMMIT
        else -> ViewerDismissOutcome.CANCEL
    }
}

/** 计算图片在全屏容器 `ContentScale.Fit` 下的拟合基准尺寸与回落目标几何。 */
internal fun resolveViewerOriginEndpoint(
    originRect: Rect,
    containerBounds: Rect,
    imageAspectRatio: Float?,
): FullScreenViewerTransform {
    val cw = containerBounds.width.takeIf { it > 1f } ?: return FullScreenViewerTransform(
        backgroundAlpha = 0f,
        contentAlpha = 0f,
        controlsAlpha = 0f,
    )
    val ch = containerBounds.height.takeIf { it > 1f } ?: return FullScreenViewerTransform(
        backgroundAlpha = 0f,
        contentAlpha = 0f,
        controlsAlpha = 0f,
    )
    val ratio = imageAspectRatio?.takeIf { it > 0f }
        ?: (originRect.width / originRect.height.coerceAtLeast(1f))
    val containerRatio = cw / ch
    val fittedWidth = if (ratio > containerRatio) cw else ch * ratio
    val targetScale = (originRect.width / fittedWidth.coerceAtLeast(1f)).coerceIn(0.15f, 1.5f)
    return FullScreenViewerTransform(
        translationX = originRect.center.x - containerBounds.center.x,
        translationY = originRect.center.y - containerBounds.center.y,
        scale = targetScale,
        cornerRadiusPx = 0f,
        backgroundAlpha = 0f,
        contentAlpha = 1f,
        controlsAlpha = 0f,
    )
}

internal fun lerpViewerTransform(
    start: FullScreenViewerTransform,
    end: FullScreenViewerTransform,
    fraction: Float,
): FullScreenViewerTransform {
    val t = fraction.coerceIn(0f, 1f)
    return FullScreenViewerTransform(
        translationX = lerp(start.translationX, end.translationX, t),
        translationY = lerp(start.translationY, end.translationY, t),
        scale = lerp(start.scale, end.scale, t),
        cornerRadiusPx = lerp(start.cornerRadiusPx, end.cornerRadiusPx, t),
        backgroundAlpha = lerp(start.backgroundAlpha, end.backgroundAlpha, t),
        contentAlpha = lerp(start.contentAlpha, end.contentAlpha, t),
        controlsAlpha = lerp(start.controlsAlpha, end.controlsAlpha, t),
    )
}

/** 协调全屏看图器的入场原位放大、跟手纵向拖拽缩小与退场回落动画。 */
@Stable
internal class FullScreenViewerTransitionState(
    initialPage: Int,
    private val imageAspectRatio: Float? = null,
    private val imageAspectRatioProvider: ((Int) -> Float?)? = null,
    private val originBoundsProvider: (Int) -> Rect?,
    private val onDismissComplete: () -> Unit,
) {
    var currentPage by mutableIntStateOf(initialPage)
    val isZoomedState = mutableStateOf(false)
    var isVerticalScrollMode by mutableStateOf(false)
    var containerBounds by mutableStateOf(Rect.Zero)

    private var hasPlayedOpen by mutableStateOf(false)
    private var openStartTransform by mutableStateOf<FullScreenViewerTransform?>(null)
    private val openProgress: Animatable<Float, AnimationVector1D> = Animatable(0f)

    private var isDragging by mutableStateOf(false)
    private var dragOffsetX by mutableFloatStateOf(0f)
    private var dragOffsetY by mutableFloatStateOf(0f)

    private var isClosing by mutableStateOf(false)
    private var isCanceling by mutableStateOf(false)
    private var closeStartTransform by mutableStateOf(FullScreenViewerTransform())
    private var closeEndTransform by mutableStateOf(FullScreenViewerTransform())
    private val closeProgress: Animatable<Float, AnimationVector1D> = Animatable(0f)
    private val cancelProgress: Animatable<Float, AnimationVector1D> = Animatable(0f)
    private var cancelStartTransform by mutableStateOf(FullScreenViewerTransform())

    private fun aspectRatioFor(page: Int): Float? =
        imageAspectRatioProvider?.invoke(page) ?: imageAspectRatio

    fun startOpenIfReady(): Boolean {
        if (hasPlayedOpen || containerBounds.width <= 1f || containerBounds.height <= 1f) return false
        val origin = originBoundsProvider(currentPage)
        openStartTransform = if (origin != null) {
            resolveViewerOriginEndpoint(origin, containerBounds, aspectRatioFor(currentPage))
        } else {
            FullScreenViewerTransform(scale = 0.92f, backgroundAlpha = 0f, contentAlpha = 0f, controlsAlpha = 0f)
        }
        hasPlayedOpen = true
        return true
    }

    suspend fun runOpenAnimation() {
        if (!hasPlayedOpen) return
        openProgress.snapTo(0f)
        openProgress.animateTo(1f, tween(FULLSCREEN_OPEN_DURATION_MS, easing = HyperOSDecelerateEasing))
    }

    fun updateDrag(offset: Offset) {
        if (isClosing) return
        isDragging = true
        isCanceling = false
        dragOffsetX = offset.x
        dragOffsetY = offset.y
    }

    suspend fun cancelDrag() {
        if (isClosing) return
        cancelStartTransform = currentTransform()
        isDragging = false
        dragOffsetX = 0f
        dragOffsetY = 0f
        isCanceling = true
        cancelProgress.snapTo(0f)
        cancelProgress.animateTo(1f, tween(FULLSCREEN_CANCEL_DURATION_MS, easing = HyperOSDecelerateEasing))
        isCanceling = false
    }

    suspend fun requestDismiss() {
        if (isClosing) return
        closeStartTransform = currentTransform()
        isDragging = false
        isCanceling = false
        isClosing = true
        try {
            val origin = originBoundsProvider(currentPage)
            closeEndTransform = if (origin != null && containerBounds.width > 1f) {
                resolveViewerOriginEndpoint(origin, containerBounds, aspectRatioFor(currentPage))
            } else {
                FullScreenViewerTransform(
                    translationY = closeStartTransform.translationY + 120f,
                    scale = (closeStartTransform.scale * 0.85f).coerceAtLeast(0.5f),
                    cornerRadiusPx = 28f,
                    backgroundAlpha = 0f,
                    contentAlpha = 0f,
                    controlsAlpha = 0f,
                )
            }
            closeProgress.snapTo(0f)
            closeProgress.animateTo(1f, tween(FULLSCREEN_CLOSE_DURATION_MS, easing = HyperOSDecelerateEasing))
        } finally {
            onDismissComplete()
        }
    }

    fun canInterceptVerticalDrag(): Boolean =
        !isClosing && !isZoomedState.value && !isVerticalScrollMode

    fun currentTransform(): FullScreenViewerTransform {
        if (!hasPlayedOpen) {
            return FullScreenViewerTransform(backgroundAlpha = 0f, contentAlpha = 0f, controlsAlpha = 0f)
        }
        if (isClosing) {
            return lerpViewerTransform(closeStartTransform, closeEndTransform, closeProgress.value)
        }
        if (isDragging) {
            val ch = containerBounds.height.takeIf { it > 1f } ?: 1000f
            val progress = (abs(dragOffsetY) / (ch * 0.45f)).coerceIn(0f, 1f)
            return FullScreenViewerTransform(
                translationX = dragOffsetX,
                translationY = dragOffsetY,
                scale = (1f - progress * 0.42f).coerceIn(0.55f, 1f),
                cornerRadiusPx = 28f * progress,
                backgroundAlpha = (1f - progress * 1.15f).coerceIn(0f, 1f),
                contentAlpha = 1f,
                controlsAlpha = (1f - progress * 4f).coerceIn(0f, 1f),
            )
        }
        if (isCanceling) {
            return lerpViewerTransform(cancelStartTransform, FullScreenViewerTransform(), cancelProgress.value)
        }
        val openStart = openStartTransform ?: return FullScreenViewerTransform()
        val p = openProgress.value
        if (p >= 0.999f) return FullScreenViewerTransform()
        return lerpViewerTransform(openStart, FullScreenViewerTransform(), p)
    }
}

@Composable
internal fun rememberFullScreenViewerTransitionState(
    initialPage: Int,
    imageAspectRatio: Float?,
    tracker: DetailImageBoundsTracker?,
    onDismissComplete: () -> Unit,
    imageAspectRatioProvider: ((Int) -> Float?)? = null,
): FullScreenViewerTransitionState {
    val currentOnDismiss by rememberUpdatedState(onDismissComplete)
    val currentAspectRatio by rememberUpdatedState(imageAspectRatio)
    val currentAspectRatioProvider by rememberUpdatedState(imageAspectRatioProvider)
    val state = remember(initialPage, tracker) {
        FullScreenViewerTransitionState(
            initialPage = initialPage,
            imageAspectRatioProvider = { page ->
                currentAspectRatioProvider?.invoke(page) ?: currentAspectRatio
            },
            originBoundsProvider = { page -> tracker?.visibleBoundsFor(page) },
            onDismissComplete = { currentOnDismiss() },
        )
    }
    LaunchedEffect(state) {
        snapshotFlow { state.containerBounds }.first { it.width > 1f && it.height > 1f }
        if (state.startOpenIfReady()) {
            state.runOpenAnimation()
        }
    }
    return state
}

/** 为全屏看图器容器挂载坐标测量与单指纵向拖拽关闭手势（放大、双指捏合与纵滚模式自动让渡）。 */
@Composable
internal fun Modifier.fullScreenViewerDismissContainer(
    transitionState: FullScreenViewerTransitionState?,
): Modifier {
    if (transitionState == null) return this
    val scope = rememberCoroutineScope()
    return this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                transitionState.containerBounds = coords.boundsInWindow()
            }
        }
        .pointerInput(transitionState) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!transitionState.canInterceptVerticalDrag()) return@awaitEachGesture
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                var activated = false
                var settled = false
                var cumulative = Offset.Zero
                var prevPosition = down.position
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } > 1) {
                            if (activated) {
                                settled = true
                                scope.launch { transitionState.cancelDrag() }
                            }
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (activated) {
                                val vy = tracker.calculateVelocity().y
                                val outcome = resolveViewerDismissOutcome(
                                    dragOffsetY = cumulative.y,
                                    velocityY = vy,
                                    containerHeightPx = size.height.toFloat(),
                                )
                                settled = true
                                scope.launch {
                                    when (outcome) {
                                        ViewerDismissOutcome.COMMIT -> transitionState.requestDismiss()
                                        ViewerDismissOutcome.CANCEL -> transitionState.cancelDrag()
                                    }
                                }
                            }
                            break
                        }
                        val delta = change.position - prevPosition
                        prevPosition = change.position
                        tracker.addPosition(change.uptimeMillis, change.position)
                        if (!activated) {
                            if (!transitionState.canInterceptVerticalDrag()) return@awaitEachGesture
                            val total = change.position - down.position
                            val dx = abs(total.x)
                            val dy = abs(total.y)
                            if (dx >= dy * 0.9f && dx > viewConfiguration.touchSlop) {
                                return@awaitEachGesture
                            }
                            if (dy <= viewConfiguration.touchSlop) {
                                continue
                            }
                            activated = true
                        }
                        cumulative += delta
                        transitionState.updateDrag(cumulative)
                        event.changes.forEach { it.consume() }
                    }
                } finally {
                    if (activated && !settled) {
                        scope.launch { transitionState.cancelDrag() }
                    }
                }
            }
        }
}

/** 应用全屏看图器缩放、平移与圆角变换到内容层。 */
internal fun Modifier.fullScreenViewerContentTransform(
    transitionState: FullScreenViewerTransitionState?,
): Modifier {
    if (transitionState == null) return this
    return graphicsLayer {
        val frame = transitionState.currentTransform()
        translationX = frame.translationX
        translationY = frame.translationY
        scaleX = frame.scale
        scaleY = frame.scale
        alpha = frame.contentAlpha
        if (frame.cornerRadiusPx > 0.5f) {
            shape = RoundedCornerShape(frame.cornerRadiusPx)
            clip = true
        } else {
            clip = false
        }
    }
}
