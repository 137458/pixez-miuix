package com.perol.pixez.shared.ui.navigation.animation.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect as AndroidRect
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import com.perol.pixez.shared.ui.navigation.animation.TransitionTarget
import com.perol.pixez.shared.ui.navigation.animation.isRectContainedInViewport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.roundToInt

/** 当前宿主 Activity 是否具备系统级原生窗口卡片转场能力（未开启/分屏/不支持设备为 false）。 */
internal val LocalSystemCardTransitionCapable = compositionLocalOf { false }

/** 当前页面背景色（用于给圆角卡片透明边缘垫底，防止系统放大位图时边缘变黑）。 */
internal val LocalSystemCardBackgroundColor = compositionLocalOf { Color.Black }

/** 独立详情窗口持有的系统转场会话 token；主窗口内嵌详情时为 null。 */
internal val LocalSystemTransitionSessionToken = compositionLocalOf<String?> { null }

internal tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

internal class SystemIllustCardSource(
    val target: TransitionTarget,
    val view: View,
    var cornerRadiusPx: Float,
    var backgroundColorArgb: Int = android.graphics.Color.BLACK,
    val layer: GraphicsLayer,
) {
    var coordinates: LayoutCoordinates? = null
    var safeViewportProvider: (() -> Rect?)? = null
    @Volatile
    var preCapturedSnapshot: Deferred<ImageBitmap?>? = null

    fun bounds(windowCoordinates: Boolean, allowPartial: Boolean = false): AndroidRect? {
        val coords = coordinates?.takeIf { it.isAttached } ?: return null
        if (!view.isAttachedToWindow) return null
        val rootView = view.rootView
        if (rootView.width <= 1 || rootView.height <= 1) return null

        val widthF = coords.size.width.toFloat()
        val heightF = coords.size.height.toFloat()
        if (widthF <= 1f || heightF <= 1f) return null

        val origin = coords.localToWindow(Offset.Zero)
        val topRight = coords.localToWindow(Offset(widthF, 0f))
        val bottomLeft = coords.localToWindow(Offset(0f, heightF))
        val bottom = coords.localToWindow(Offset(widthF, heightF))
        // 窗口协议只接收轴对齐的原尺寸截图，拒绝旋转或非等比缩放卡片。
        if (!sameBounds(
                Rect(origin, bottom),
                Rect(origin, origin + Offset(widthF, heightF)),
            ) ||
            abs(topRight.y - origin.y) > 1f ||
            abs(bottomLeft.x - origin.x) > 1f ||
            abs(topRight.x - bottom.x) > 1f ||
            abs(bottomLeft.y - bottom.y) > 1f
        ) {
            return null
        }
        val full = Rect(origin, bottom)
        val visible = coords.boundsInWindow()
        // 排除 HorizontalPager 离屏缓存页中的不可见卡片。
        val overlapsRoot = visible.right > 1f &&
            visible.bottom > 1f &&
            visible.left < rootView.width - 1f &&
            visible.top < rootView.height - 1f
        if (!overlapsRoot || visible.width <= 1f || visible.height <= 1f) return null
        if (!allowPartial && !sameBounds(full, visible)) return null
        // 卡片任一边缘被 TopBar / BottomBar / NavigationRail 遮挡时，系统级跨窗口转场返回会直接盖在栏位上方，
        // 必须回退到应用内 CardExpandLayer 进行安全视口裁切。
        val safeViewport = safeViewportProvider?.invoke()
        if (safeViewport != null && !isRectContainedInViewport(full, safeViewport)) return null

        val offset = IntArray(2)
        if (!windowCoordinates) {
            val screen = IntArray(2)
            view.getLocationOnScreen(screen)
            view.getLocationInWindow(offset)
            offset[0] = screen[0] - offset[0]
            offset[1] = screen[1] - offset[1]
        }
        return AndroidRect(
            (full.left + offset[0]).roundToInt(),
            (full.top + offset[1]).roundToInt(),
            (full.right + offset[0]).roundToInt(),
            (full.bottom + offset[1]).roundToInt(),
        )
    }

    fun captureLayer(): GraphicsLayer? = layer.takeIf {
        !it.isReleased &&
            it.size.width > 1 &&
            it.size.height > 1 &&
            it.size == coordinates?.size
    }
}

internal fun sameBounds(first: Rect, second: Rect): Boolean =
    abs(first.left - second.left) <= 1f &&
        abs(first.top - second.top) <= 1f &&
        abs(first.right - second.right) <= 1f &&
        abs(first.bottom - second.bottom) <= 1f

/** 打开与返回全程弱引用绑定同一次点击的卡片实例，Lazy 回收后不保活页面。 */
internal class SystemIllustCardAnchor(source: SystemIllustCardSource) {
    private val reference = WeakReference(source)
    val backgroundColorArgb: Int = source.backgroundColorArgb

    fun belongsToWindow(decor: View): Boolean = reference.get()?.let {
        SystemIllustCardAnchorRegistry.isAttached(it) && it.view.rootView === decor.rootView
    } == true

    fun bounds(windowCoordinates: Boolean, allowPartial: Boolean = false): AndroidRect? = reference.get()
        ?.takeIf { SystemIllustCardAnchorRegistry.isAttached(it) }
        ?.bounds(windowCoordinates, allowPartial)

    fun radiusPx(): Float? = reference.get()
        ?.takeIf { SystemIllustCardAnchorRegistry.isAttached(it) }
        ?.cornerRadiusPx

    fun captureLayer(): GraphicsLayer? = reference.get()
        ?.takeIf { SystemIllustCardAnchorRegistry.isAttached(it) }
        ?.captureLayer()

    suspend fun captureBitmap(): ImageBitmap? {
        val source = reference.get()
            ?.takeIf { SystemIllustCardAnchorRegistry.isAttached(it) }
            ?: return null
        val snapshot = try {
            source.preCapturedSnapshot?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
        currentCoroutineContext().ensureActive()
        val expectedSize = source.coordinates?.size
        if (snapshot != null && expectedSize != null &&
            snapshot.width == expectedSize.width &&
            snapshot.height == expectedSize.height
        ) {
            source.preCapturedSnapshot = null
            return snapshot
        }
        return try {
            source.captureLayer()?.toImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    fun matchesWindowGeometry(
        decor: View,
        expectedBounds: AndroidRect,
        expectedRadiusPx: Float,
        windowCoordinates: Boolean,
        allowPartial: Boolean,
    ): Boolean {
        val currentRadius = radiusPx() ?: return false
        return belongsToWindow(decor) &&
            bounds(windowCoordinates, allowPartial) == expectedBounds &&
            abs(currentRadius - expectedRadiusPx) <= 0.5f
    }
}

internal object SystemIllustCardAnchorRegistry {
    private val sources = mutableSetOf<SystemIllustCardSource>()
    private var lastTouched: WeakReference<SystemIllustCardSource>? = null

    fun attach(source: SystemIllustCardSource) {
        sources.add(source)
    }

    fun detach(source: SystemIllustCardSource) {
        sources.remove(source)
        source.coordinates = null
        source.safeViewportProvider = null
        if (lastTouched?.get() === source) lastTouched = null
    }

    fun markTouched(source: SystemIllustCardSource) {
        if (source in sources) {
            lastTouched = WeakReference(source)
        }
    }

    fun isAttached(source: SystemIllustCardSource): Boolean = source in sources

    fun peekAnchor(activity: Activity, target: TransitionTarget): SystemIllustCardAnchor? {
        val decor = activity.window?.decorView ?: return null
        val touched = lastTouched?.get()?.takeIf {
            it in sources &&
                it.target == target &&
                it.view.rootView === decor.rootView &&
                it.bounds(windowCoordinates = true, allowPartial = false) != null
        }
        if (touched != null) return SystemIllustCardAnchor(touched)

        val matched = sources.firstOrNull {
            it.target == target &&
                it.view.rootView === decor.rootView &&
                it.bounds(windowCoordinates = true, allowPartial = false) != null
        } ?: return null
        return SystemIllustCardAnchor(matched)
    }

    fun takeAnchor(activity: Activity, target: TransitionTarget): SystemIllustCardAnchor? =
        peekAnchor(activity, target).also {
            if (lastTouched?.get()?.target == target) {
                lastTouched = null
            }
        }
}
