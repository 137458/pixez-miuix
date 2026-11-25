package com.perol.pixez.shared.ui.navigation.animation.system

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withResumed
import com.itgsa.animation.sharedview.ViewAnimationConfig
import com.itgsa.animation.sharedview.ViewAnimationExtras
import com.itgsa.animation.sharedview.ViewAnimationListener
import com.itgsa.animation.sharedview.ViewAnimationManager
import com.perol.pixez.shared.ui.navigation.Config
import com.perol.pixez.shared.ui.navigation.animation.TransitionTarget
import com.perol.pixez.shared.ui.navigation.animation.TransitionTargetKind
import com.perol.pixez.shared.ui.navigation.toTransitionTarget
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.WeakHashMap
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * 系统级原生窗口卡片转场调度：
 * 标准 ITGSA 协议优先，再尝试 OPPO 旧无缝、vivo OneShot、小米圆角协议；
 * 设备不支持、处于分屏/小窗或捕获失败时返回 false，由调用方自动回退现有 CardExpandLayer。
 */
object SystemIllustCardTransition {
    private const val TAG = "SystemIllustCardTransition"
    const val EXTRA_SESSION = "pixez_system_card_transition_session"

    private class Session(
        val originTarget: TransitionTarget,
        var displayedTarget: TransitionTarget,
        val sourceActivity: WeakReference<ComponentActivity>,
        val sourceWindow: SystemCardWindowGeometry,
        val thumbnail: Bitmap,
        val canRefreshBitmap: Boolean,
        val canConfigureBlur: Boolean,
        val canConfigureBackgroundScale: Boolean,
        val method: SystemCardTransitionMethod,
        val oplusSdkWindow: Boolean,
        val backAnimMetadata: String?,
        val openingBounds: Rect,
        val windowCoordinates: Boolean,
        val allowPartialSource: Boolean,
        val openingRadius: Float,
        val sourceAnchor: SystemIllustCardAnchor,
    ) {
        val requestedAtMs = SystemClock.uptimeMillis()
        @Volatile var openingEvent = "awaiting_start"
        var oplusOpening: OplusCardTransition.Opening? = null
        var vivoOpening: VivoCardTransition.Opening? = null
    }

    private val sessions = mutableMapOf<String, Session>()
    private val openingJobs = WeakHashMap<ComponentActivity, Job>()
    private var latestWindowRequestToken: String? = null

    private fun windowGeometry(activity: Activity): SystemCardWindowGeometry {
        val display = activity.display
        val windowBounds = activity.windowManager.currentWindowMetrics.bounds
        return SystemCardWindowGeometry(
            left = windowBounds.left,
            top = windowBounds.top,
            right = windowBounds.right,
            bottom = windowBounds.bottom,
            displayId = display?.displayId ?: 0,
            rotation = display?.rotation ?: 0,
            densityDpi = activity.resources.configuration.densityDpi,
        )
    }

    internal fun deviceCapability(activity: Activity?): SystemCardCapability {
        val oplusSystemManager = OplusCardTransition.hasSystemManager
        val conditions = if (oplusSystemManager) OplusCardTransition.conditions(activity) else null
        val summary = conditions?.toString()
        if (conditions?.blocked == true) {
            return SystemCardCapability(systemConditionSummary = summary)
        }
        val officialSupport = runCatching { ViewAnimationManager.isViewAnimationSupported() }.getOrNull()
        val capability = SystemCardCapability(systemConditionSummary = summary, officialSupport = officialSupport)
        if (officialSupport != null && officialSupport > 0) {
            val extras = runCatching { ViewAnimationManager.getViewAnimationExtras() }.getOrNull()
            return capability.copy(
                method = SystemCardTransitionMethod.OFFICIAL,
                configurableBlur = extras?.containsKey(ViewAnimationExtras.EXTRA_BLUR) == true,
                backAnimMetadata = extras?.get(ViewAnimationExtras.EXTRA_BACK_ANIM_TYPE),
                oplusSdkWindow = oplusSystemManager,
            )
        }
        if (OplusCardTransition.available) {
            return capability.copy(method = SystemCardTransitionMethod.OPLUS_LEGACY)
        }
        if (VivoCardTransition.available) {
            return capability.copy(
                method = SystemCardTransitionMethod.VIVO_ONESHOT,
                configurableBlur = VivoCardTransition.configurableBlur,
            )
        }
        if (RoundedCardTransition.available) {
            return capability.copy(method = SystemCardTransitionMethod.XIAOMI_ROUNDED)
        }
        return capability
    }

    fun isDeviceSupported(activity: Activity?): Boolean {
        if (activity == null || activity.isInMultiWindowMode) return false
        return deviceCapability(activity).available
    }

    fun canAttempt(activity: ComponentActivity, config: Config): Boolean {
        val target = config.toTransitionTarget() ?: return false
        return canAttempt(activity, target)
    }

    fun canAttempt(activity: ComponentActivity, illustId: Long): Boolean =
        canAttempt(activity, TransitionTarget(TransitionTargetKind.Illust, illustId))

    fun canAttempt(activity: ComponentActivity, target: TransitionTarget): Boolean {
        val isMultiWindow = activity.isInMultiWindowMode
        val anchor = if (!isMultiWindow) SystemIllustCardAnchorRegistry.peekAnchor(activity, target) else null
        val capability = if (anchor != null) deviceCapability(activity) else null
        return shouldAttemptSystemCardTransition(
            isMultiWindowOrFloating = isMultiWindow,
            hasSourceAnchor = anchor != null,
            capabilityAvailable = capability?.available == true,
        )
    }

    fun hasSession(token: String?): Boolean = token != null && token in sessions

    fun updateDisplayedIllust(token: String?, displayedIllustId: Long) {
        if (token == null) return
        sessions[token]?.displayedTarget = TransitionTarget(TransitionTargetKind.Illust, displayedIllustId)
    }

    /** false 表示未启动系统窗口，调用方回退原有 CardExpandLayer 导航。 */
    suspend fun tryOpen(
        activity: ComponentActivity,
        config: Config,
        buildIntent: (sessionToken: String) -> Intent,
        isCurrent: () -> Boolean,
    ): Boolean {
        val target = config.toTransitionTarget() ?: return false
        return tryOpen(activity, target, buildIntent, isCurrent)
    }

    /** false 表示未启动系统窗口，调用方回退原有 CardExpandLayer 导航。 */
    suspend fun tryOpen(
        activity: ComponentActivity,
        illustId: Long,
        buildIntent: (sessionToken: String) -> Intent,
        isCurrent: () -> Boolean,
    ): Boolean = tryOpen(
        activity = activity,
        target = TransitionTarget(TransitionTargetKind.Illust, illustId),
        buildIntent = buildIntent,
        isCurrent = isCurrent,
    )

    /** false 表示未启动系统窗口，调用方回退原有 CardExpandLayer 导航。 */
    suspend fun tryOpen(
        activity: ComponentActivity,
        target: TransitionTarget,
        buildIntent: (sessionToken: String) -> Intent,
        isCurrent: () -> Boolean,
    ): Boolean {
        val job = currentCoroutineContext()[Job]
        if (job != null) {
            val previous = openingJobs.put(activity, job)
            if (previous !== job) previous?.cancel()
        }
        try {
            fun rejected(reason: String): Boolean {
                Napier.i(tag = TAG) { "未请求系统展开 target=$target reason=$reason" }
                return false
            }
            if (activity.isInMultiWindowMode) return rejected("multi_window_mode")
            val anchor = SystemIllustCardAnchorRegistry.takeAnchor(activity, target)
                ?: return rejected("source_anchor_missing")
            if (!anchor.belongsToWindow(activity.window.decorView)) return rejected("source_window_mismatch")
            val sourceWindow = windowGeometry(activity)
            val capability = deviceCapability(activity)
            if (!capability.available) {
                return rejected("system_protocol_unavailable ${capability.systemConditionSummary.orEmpty()}")
            }
            val hasLayer = anchor.captureLayer() != null
            val capturedImage = if (hasLayer) anchor.captureBitmap() else null
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return rejected("request_expired_before_capture")
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                !activity.window.decorView.hasWindowFocus()
            ) {
                return rejected("source_window_inactive")
            }

            val extras = if (capability.method == SystemCardTransitionMethod.OFFICIAL) {
                runCatching { ViewAnimationManager.getViewAnimationExtras() }.getOrNull()
            } else null
            val windowCoordinates = capability.method == SystemCardTransitionMethod.VIVO_ONESHOT ||
                officialBoundsUseWindowCoordinates(extras?.get(ViewAnimationExtras.EXTRA_BOUNDS))
            val bounds = anchor.bounds(windowCoordinates, allowPartial = capturedImage != null)
                ?: return rejected("source_bounds_unavailable")

            val sourceBackgroundColor = anchor.backgroundColorArgb
            val thumbnail = if (capturedImage != null) {
                val pixels = capturedImage.asAndroidBitmap()
                withContext(Dispatchers.Default) {
                    compositeOverBackgroundColor(pixels, sourceBackgroundColor)
                } ?: return rejected("source_pixels_unavailable")
            } else {
                val captureBounds = anchor.bounds(windowCoordinates = true)
                    ?: return rejected("source_bounds_unavailable")
                captureWindowRegion(activity, captureBounds) ?: return rejected("source_pixels_unavailable")
            }
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return rejected("request_expired_after_pixels")
            if (abs(thumbnail.width - bounds.width()) > 1 ||
                abs(thumbnail.height - bounds.height()) > 1
            ) {
                return rejected("source_size_mismatch bitmap=${thumbnail.width}x${thumbnail.height} bounds=$bounds")
            }
            val radius = anchor.radiusPx() ?: return rejected("source_radius_unavailable")
            val canConfigureBlur = extras?.containsKey(ViewAnimationExtras.EXTRA_BLUR) == true
            val canConfigureBackgroundScale = extras?.containsKey(ViewAnimationExtras.EXTRA_BACKGROUND_SCALE) == true
            val configuration = Bundle().apply {
                if (canConfigureBlur) putBoolean(ViewAnimationExtras.EXTRA_BLUR, true)
                if (canConfigureBackgroundScale) putFloat(ViewAnimationExtras.EXTRA_BACKGROUND_SCALE, 1f)
                if (capability.oplusSdkWindow && extras?.containsKey(ViewAnimationExtras.EXTRA_COLOR) == true) {
                    putInt(ViewAnimationExtras.EXTRA_COLOR, sourceBackgroundColor)
                }
            }
            val prefix = when (capability.method) {
                SystemCardTransitionMethod.VIVO_ONESHOT -> "vivo"
                SystemCardTransitionMethod.OPLUS_LEGACY -> "oplus"
                SystemCardTransitionMethod.XIAOMI_ROUNDED -> "rounded"
                SystemCardTransitionMethod.OFFICIAL -> "sdk"
                SystemCardTransitionMethod.NONE -> return rejected("no_protocol")
            }
            val token = "$prefix:${UUID.randomUUID()}"
            val session = Session(
                originTarget = target,
                displayedTarget = target,
                sourceActivity = WeakReference(activity),
                sourceWindow = sourceWindow,
                thumbnail = thumbnail,
                canRefreshBitmap = extras?.containsKey(ViewAnimationExtras.EXTRA_BITMAP) == true,
                canConfigureBlur = canConfigureBlur,
                canConfigureBackgroundScale = canConfigureBackgroundScale,
                method = capability.method,
                oplusSdkWindow = capability.oplusSdkWindow,
                backAnimMetadata = capability.backAnimMetadata,
                openingBounds = Rect(bounds),
                windowCoordinates = windowCoordinates,
                allowPartialSource = capturedImage != null,
                openingRadius = radius,
                sourceAnchor = anchor,
            )
            val listener = object : ViewAnimationListener {
                private fun record(event: String, animMode: Int) {
                    if (animMode == ViewAnimationManager.ANIM_MODE_OPEN) session.openingEvent = event
                    val mode = when (animMode) {
                        ViewAnimationManager.ANIM_MODE_OPEN -> "OPEN"
                        ViewAnimationManager.ANIM_MODE_BACK -> "BACK"
                        else -> animMode.toString()
                    }
                    Napier.d(tag = TAG) {
                        "session=$token mode=$mode event=$event elapsedMs=${SystemClock.uptimeMillis() - session.requestedAtMs}"
                    }
                }
                override fun onAnimationStart(animMode: Int) = record("start", animMode)
                override fun onAnimationCancel(animMode: Int) = record("cancel", animMode)
                override fun onAnimationEnd(animMode: Int) = record("end", animMode)
            }
            latestWindowRequestToken = token
            val optionsBundle = when (capability.method) {
                SystemCardTransitionMethod.VIVO_ONESHOT ->
                    VivoCardTransition.open(activity, thumbnail, bounds, radius, blurEnabled = true, listener)
                        ?.also { session.vivoOpening = it }?.options
                SystemCardTransitionMethod.OPLUS_LEGACY ->
                    OplusCardTransition.open(activity, thumbnail, bounds, radius, sourceBackgroundColor, listener)
                        ?.also { session.oplusOpening = it }?.options
                SystemCardTransitionMethod.XIAOMI_ROUNDED ->
                    RoundedCardTransition.options(activity, thumbnail, bounds, radius, sourceBackgroundColor) { event ->
                        when (event) {
                            "open_start" -> listener.onAnimationStart(ViewAnimationManager.ANIM_MODE_OPEN)
                            "open_end" -> listener.onAnimationEnd(ViewAnimationManager.ANIM_MODE_OPEN)
                            "return_start" -> listener.onAnimationStart(ViewAnimationManager.ANIM_MODE_BACK)
                            "return_end" -> listener.onAnimationEnd(ViewAnimationManager.ANIM_MODE_BACK)
                        }
                    }?.toBundle()
                SystemCardTransitionMethod.OFFICIAL ->
                    ViewAnimationManager.makeViewAnimationOptions(thumbnail, bounds, radius, configuration, listener)
                        ?.toBundle()
                SystemCardTransitionMethod.NONE -> null
            } ?: return rejected("system_options_null protocol=${capability.method}")

            sessions[token] = session
            var launched = false
            try {
                activity.lifecycle.withResumed { }
                currentCoroutineContext().ensureActive()
                val canLaunch = isCurrent() &&
                    !activity.isInMultiWindowMode &&
                    activity.window.decorView.hasWindowFocus() &&
                    windowGeometry(activity) == sourceWindow &&
                    anchor.matchesWindowGeometry(
                        activity.window.decorView,
                        bounds,
                        radius,
                        windowCoordinates,
                        allowPartial = capturedImage != null,
                    )
                if (!canLaunch) return rejected("request_expired_before_launch")

                val intent = buildIntent(token).putExtra(EXTRA_SESSION, token)
                activity.startActivity(intent, optionsBundle)
                launched = true
                Napier.i(tag = TAG) { "session=$token 系统卡片转场已请求 target=$target protocol=${capability.method}" }
                return true
            } catch (error: SecurityException) {
                Napier.w(tag = TAG, throwable = error) { "系统拒绝窗口转场，回退应用内转场" }
                return false
            } finally {
                if (!launched) {
                    sessions.remove(token)
                    session.vivoOpening?.abandon()
                    if (latestWindowRequestToken == token) {
                        when (session.method) {
                            SystemCardTransitionMethod.OPLUS_LEGACY -> OplusCardTransition.cancel()
                            SystemCardTransitionMethod.OFFICIAL -> runCatching {
                                ViewAnimationManager.cancelCurrentViewAnimation()
                            }
                            else -> Unit
                        }
                        latestWindowRequestToken = null
                    }
                }
            }
        } finally {
            if (job != null && openingJobs[activity] === job) {
                openingJobs.remove(activity)
            }
        }
    }

    fun refreshReturn(
        activity: Activity,
        reason: String,
        preparingToFinish: Boolean = false,
    ) {
        val token = activity.intent?.getStringExtra(EXTRA_SESSION) ?: return
        if (!preparingToFinish) return
        val session = sessions[token]
        val source = session?.sourceActivity?.get()
        val childWindow = windowGeometry(activity)
        val sameWindow = session != null &&
            source != null &&
            !source.isFinishing &&
            !source.isDestroyed &&
            !source.isInMultiWindowMode &&
            !activity.isInMultiWindowMode &&
            windowGeometry(source) == session.sourceWindow &&
            childWindow.displayId == session.sourceWindow.displayId &&
            childWindow.rotation == session.sourceWindow.rotation

        val anchorMatches = if (sameWindow) {
            session.sourceAnchor.matchesWindowGeometry(
                decor = source.window.decorView,
                expectedBounds = session.openingBounds,
                expectedRadiusPx = session.openingRadius,
                windowCoordinates = session.windowCoordinates,
                allowPartial = session.allowPartialSource,
            )
        } else {
            false
        }
        val sourceAvailable = session != null && shouldUseSourceReturnLanding(
            originIllustId = if (session.originTarget.kind == session.displayedTarget.kind) session.originTarget.id else -1L,
            displayedIllustId = session.displayedTarget.id,
            sameWindow = sameWindow,
            anchorMatchesGeometry = anchorMatches,
        )
        val bounds = if (sourceAvailable) session.openingBounds else null
        if (!sourceAvailable) {
            Napier.i(tag = TAG) {
                "返回落点失效，改用系统非锚点退场: reason=$reason origin=${session?.originTarget} displayed=${session?.displayedTarget} sameWindow=$sameWindow"
            }
        }
        if (token.startsWith("vivo:")) {
            session?.vivoOpening?.prepareReturn(
                bitmap = session.thumbnail,
                bounds = bounds,
                radius = session.openingRadius,
                enabled = sourceAvailable,
                blurEnabled = true,
            )
            return
        }
        if (token.startsWith("oplus:")) {
            OplusCardTransition.prepareReturn(
                target = activity,
                opening = session?.oplusOpening,
                initialBounds = session?.openingBounds,
                bounds = bounds,
                enabled = sourceAvailable,
            )
            return
        }
        if (token.startsWith("rounded:")) {
            source?.let {
                RoundedCardTransition.refreshReturn(it, bounds, sourceAvailable)
            }
            return
        }
        val currentRadius = session?.openingRadius?.takeIf { sourceAvailable }
        val oplusSdkWindow = session?.oplusSdkWindow == true
        val plan = resolveOfficialCardReturnPlan(
            metadata = session?.backAnimMetadata,
            oplusSdkWindow = oplusSdkWindow,
            preparingToFinish = preparingToFinish,
            motionEnabled = true,
            sourceAvailable = sourceAvailable,
        ) ?: return
        val config = ViewAnimationConfig()
        plan.backAnimType?.let { config.setBackAnimType(it) }
        if (plan.useSourceBounds && bounds != null) config.setBounds(bounds)
        if (plan.updateRadius && currentRadius != null) config.setRadius(currentRadius)
        val bundle = config.toBundle()
        if (session != null && bounds != null && session.canRefreshBitmap &&
            (!oplusSdkWindow || plan.useSourceBounds)
        ) {
            bundle.putParcelable(ViewAnimationExtras.EXTRA_BITMAP, session.thumbnail)
        }
        if (plan.updateEffects && session?.canConfigureBlur == true) {
            bundle.putBoolean(ViewAnimationExtras.EXTRA_BLUR, true)
        }
        if (plan.updateEffects && session?.canConfigureBackgroundScale == true) {
            bundle.putFloat(ViewAnimationExtras.EXTRA_BACKGROUND_SCALE, 1f)
        }
        runCatching {
            ViewAnimationManager.overrideViewAnimationTransition(activity, bundle)
        }.onFailure {
            Napier.w(tag = TAG, throwable = it) { "更新标准返回转场失败" }
        }
    }

    fun afterFinish(activity: Activity) {
        val token = activity.intent?.getStringExtra(EXTRA_SESSION) ?: return
        sessions[token]?.vivoOpening?.afterFinish(activity)
    }

    fun release(token: String?) {
        if (token != null) sessions.remove(token)
        if (latestWindowRequestToken == token) latestWindowRequestToken = null
    }

    private fun compositeOverBackgroundColor(source: Bitmap, backgroundColor: Int): Bitmap? {
        val softwareSource = try {
            if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false) ?: return null
            } else {
                source
            }
        } catch (error: IllegalArgumentException) {
            Napier.w(tag = TAG, throwable = error) { "卡片位图软件化失败" }
            return null
        }
        try {
            val backed = Bitmap.createBitmap(softwareSource.width, softwareSource.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(backed)
            canvas.drawColor(backgroundColor)
            canvas.drawBitmap(softwareSource, 0f, 0f, null)
            return backed
        } finally {
            if (softwareSource !== source) softwareSource.recycle()
        }
    }

    private suspend fun captureWindowRegion(activity: ComponentActivity, windowBounds: Rect): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            val bitmap = Bitmap.createBitmap(windowBounds.width(), windowBounds.height(), Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(
                    activity.window,
                    windowBounds,
                    bitmap,
                    { result ->
                        continuation.resume(bitmap.takeIf { result == PixelCopy.SUCCESS })
                    },
                    Handler(Looper.getMainLooper()),
                )
            } catch (error: IllegalArgumentException) {
                Napier.w(tag = TAG, throwable = error) { "卡片矩形像素复制失败" }
                continuation.resume(null)
            }
        }
}
