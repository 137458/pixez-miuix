package com.perol.pixez.shared.ui.navigation.animation

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.navigation.animation.system.LocalSystemCardBackgroundColor
import com.perol.pixez.shared.ui.navigation.animation.system.LocalSystemCardTransitionCapable
import com.perol.pixez.shared.ui.navigation.animation.system.SystemIllustCardAnchorRegistry
import com.perol.pixez.shared.ui.navigation.animation.system.SystemIllustCardSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/**
 * 让 Spotlight 特辑卡片把自己的窗口坐标矩形与视觉圆角登记到 [LocalSharedBoundsRegistry]。
 */
internal fun Modifier.spotlightTransitionBounds(
    articleId: Long?,
    cornerRadius: Dp = 16.dp,
): Modifier = transitionTargetBounds(
    kind = TransitionTargetKind.Spotlight,
    id = articleId,
    cornerRadius = cornerRadius,
)

/**
 * 让用户预览卡片把自己的窗口坐标矩形与视觉圆角登记到 [LocalSharedBoundsRegistry]。
 */
internal fun Modifier.userTransitionBounds(
    userId: Long?,
    cornerRadius: Dp = 18.dp,
): Modifier = transitionTargetBounds(
    kind = TransitionTargetKind.User,
    id = userId,
    cornerRadius = cornerRadius,
)

/**
 * 让评论入口卡片或操作块把自己的窗口坐标矩形与视觉圆角登记到 [LocalSharedBoundsRegistry]。
 */
internal fun Modifier.commentsTransitionBounds(
    targetId: Long?,
    isNovel: Boolean = false,
    cornerRadius: Dp = 16.dp,
): Modifier = transitionTargetBounds(
    kind = if (isNovel) TransitionTargetKind.NovelComments else TransitionTargetKind.Comments,
    id = targetId,
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
    val systemCapable = LocalSystemCardTransitionCapable.current &&
        id != null &&
        id > 0L

    val systemModifier = if (systemCapable) {
        val view = LocalView.current
        val density = LocalDensity.current
        val backgroundColorArgb = LocalSystemCardBackgroundColor.current.toArgb()
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val layer = rememberGraphicsLayer()
        val source = remember(kind, id, view, layer) {
            SystemIllustCardSource(
                target = TransitionTarget(kind, id),
                view = view,
                cornerRadiusPx = cornerRadiusPx,
                backgroundColorArgb = backgroundColorArgb,
                layer = layer,
            )
        }
        val captureGuard = remember(source) { SystemCardLayerCaptureGuard() }
        val captureScope = rememberCoroutineScope()
        SideEffect {
            source.cornerRadiusPx = cornerRadiusPx
            source.backgroundColorArgb = backgroundColorArgb
        }
        DisposableEffect(source) {
            SystemIllustCardAnchorRegistry.attach(source)
            onDispose {
                source.preCapturedSnapshot?.cancel()
                source.preCapturedSnapshot = null
                SystemIllustCardAnchorRegistry.detach(source)
            }
        }
        Modifier
            .onPlaced { source.coordinates = it }
            .pointerInput(source, captureGuard) {
                var clearJob: kotlinx.coroutines.Job? = null
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    clearJob?.cancel()
                    // 触摸按下瞬间锁定离屏图层，并立即在当前帧尚未绘制按压反馈前抢先导出未按压位图，
                    // 避免子组件 RenderNode 随后更新按压遮罩导致 layer.toImageBitmap() 录进灰色按压阴影。
                    captureGuard.onPointerDown()
                    if (source.captureLayer() != null) {
                        source.preCapturedSnapshot?.cancel()
                        source.preCapturedSnapshot = captureScope.async(start = CoroutineStart.UNDISPATCHED) {
                            runCatching { source.captureLayer()?.toImageBitmap() }.getOrNull()
                        }
                    }
                    SystemIllustCardAnchorRegistry.markTouched(source)
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.none { it.pressed }) break
                        }
                    } finally {
                        captureGuard.onPointerUpOrCancel(SystemClock.uptimeMillis())
                        val scheduledSnapshot = source.preCapturedSnapshot
                        clearJob = captureScope.launch {
                            delay(LAYER_PRESS_SETTLE_GUARD_MS)
                            if (source.preCapturedSnapshot === scheduledSnapshot) {
                                source.preCapturedSnapshot = null
                            }
                        }
                    }
                }
            }
            .drawWithContent {
                if (captureGuard.shouldRecord(size, SystemClock.uptimeMillis())) {
                    layer.record {
                        this@drawWithContent.drawContent()
                    }
                    captureGuard.markRecorded(size)
                    drawLayer(layer)
                } else {
                    this@drawWithContent.drawContent()
                }
            }
    } else {
        Modifier
    }

    val registryModifier = remember(registry, kind, id) {
        var latestCoordinates: androidx.compose.ui.layout.LayoutCoordinates? = null
        Modifier
            .onGloballyPositioned { coordinates ->
                latestCoordinates = coordinates
                val resolvedId = currentId
                if (resolvedId != null && resolvedId > 0 && coordinates.isAttached) {
                    registry.putTarget(
                        target = TransitionTarget(kind, resolvedId),
                        rect = coordinates.boundsInWindow(),
                        cornerRadiusDp = currentCornerRadius.value,
                    )
                }
            }
            .pointerInput(registry, kind, id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val coords = latestCoordinates
                    val resolvedId = currentId
                    if (coords != null && coords.isAttached && resolvedId != null && resolvedId > 0) {
                        registry.putTarget(
                            target = TransitionTarget(kind, resolvedId),
                            rect = coords.boundsInWindow(),
                            cornerRadiusDp = currentCornerRadius.value,
                        )
                    }
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

    systemModifier.then(registryModifier)
}

private const val LAYER_PRESS_SETTLE_GUARD_MS = 450L

/**
 * 在手指按压及按压反馈回弹期间冻结系统转场离屏图层录制，
 * 保留触控前的未按压画面作为卡片展开/返回位图。
 */
internal class SystemCardLayerCaptureGuard {
    private var pointerDown = false
    private var freezeUntilUptimeMs = 0L
    private var recordedSize: Size = Size.Unspecified

    fun onPointerDown() {
        pointerDown = true
    }

    fun onPointerUpOrCancel(nowUptimeMs: Long) {
        pointerDown = false
        freezeUntilUptimeMs = nowUptimeMs + LAYER_PRESS_SETTLE_GUARD_MS
    }

    fun shouldRecord(size: Size, nowUptimeMs: Long): Boolean {
        if (recordedSize == Size.Unspecified) return true
        if (!pointerDown && size != recordedSize) return true
        return !pointerDown && nowUptimeMs >= freezeUntilUptimeMs
    }

    fun markRecorded(size: Size) {
        recordedSize = size
    }
}
