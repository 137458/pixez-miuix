package com.perol.pixez.shared.ui.navigation.animation.system

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import com.itgsa.animation.sharedview.ViewAnimationListener
import io.github.aakira.napier.Napier
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.function.BiConsumer

/** OPPO / ColorOS 旧无缝窗口协议。系统保留转场与返回关系，应用只提供卡片像素和屏幕几何。 */
internal object OplusCardTransition {
    private const val TAG = "OplusCardTransition"
    private const val OPEN = "view_seamless_open"

    private val remoteManagerClass: Class<*>? by lazy {
        try {
            Class.forName("android.app.viewseamless.OplusViewSeamlessRemoteManager", false, null)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    /** 标准门面的识别不依赖旧接口反射是否获准，避免误用其他实现的返回更新策略。 */
    val hasSystemManager: Boolean get() = remoteManagerClass != null

    private data class Protocol(
        val open: Method,
        val skipBack: Method,
        val cancel: Method,
        val fromBundle: Method,
        val remoteTransition: Method,
        val callback: Class<*>,
    )

    private val protocol: Protocol? by lazy {
        var member = "com.oplus.animation.OplusViewSeamless"
        try {
            val facade = Class.forName(member, false, null)
            member = "com.oplus.animation.OplusViewSeamless\$AnimationCallback"
            val callback = Class.forName(member, false, null)

            member = "OplusViewSeamless.setSeamlessView"
            val open = facade.getMethod(
                "setSeamlessView",
                View::class.java,
                Context::class.java,
                Bundle::class.java,
                callback,
            )
            member = "OplusViewSeamless.skipBackAnim"
            val skipBack = facade.getMethod("skipBackAnim", Activity::class.java)
            member = "OplusViewSeamless.finishCurrentAnimation"
            val cancel = facade.getMethod("finishCurrentAnimation")
            member = "ActivityOptions.fromBundle"
            val fromBundle = ActivityOptions::class.java.getMethod("fromBundle", Bundle::class.java)
            member = "ActivityOptions.getRemoteTransition"
            val remoteTransition = ActivityOptions::class.java.getMethod("getRemoteTransition")
            Protocol(open, skipBack, cancel, fromBundle, remoteTransition, callback)
        } catch (error: ClassNotFoundException) {
            if (hasSystemManager) Napier.w(tag = TAG) { "旧无缝窗口协议类查找失败: member=$member error=$error" }
            null
        } catch (error: NoSuchMethodException) {
            Napier.w(tag = TAG) { "旧无缝窗口协议方法查找失败: member=$member error=$error" }
            null
        } catch (error: SecurityException) {
            Napier.w(tag = TAG, throwable = error) { "系统不允许访问旧无缝窗口协议: member=$member" }
            null
        } catch (error: LinkageError) {
            Napier.w(tag = TAG, throwable = error) { "旧无缝窗口协议依赖不可用: member=$member" }
            null
        }
    }

    val available: Boolean get() = protocol != null

    internal data class Conditions(
        val featureEnabled: Boolean?,
        val configurationEnabled: Boolean?,
        val animationLevel: Int?,
        val windowAllowed: Boolean?,
        val windowState: Int?,
        val compatMode: Boolean?,
        val queryErrors: List<String>,
    ) {
        val blocked: Boolean
            get() = featureEnabled == false ||
                configurationEnabled == false ||
                (animationLevel != null && animationLevel > 3) ||
                windowAllowed == false
    }

    fun conditions(activity: Activity?): Conditions {
        val errors = mutableListOf<String>()
        val manager = remoteManagerClass
            ?: return Conditions(null, null, null, null, null, null, listOf("原生无缝管理器不存在"))
        fun <T> read(name: String, block: () -> T): T? = try {
            block()
        } catch (error: ReflectiveOperationException) {
            errors += "$name: ${cause(error)}"
            null
        } catch (error: SecurityException) {
            errors += "$name: $error"
            null
        }
        fun field(name: String): Any? = read(name) {
            manager.getDeclaredField(name).apply { isAccessible = true }.get(null)
        }
        val feature = field("FEATURE_ENABLE") as? Boolean
        val configuration = field("DPP") as? Boolean
        val level = field("ANIM_LEVEL") as? Int
        val allowed = if (activity == null) null else read("isFeatureEnable") {
            val instance = manager.getMethod("getInstance").invoke(null)
            manager.getDeclaredMethod("isFeatureEnable", Context::class.java)
                .apply { isAccessible = true }.invoke(instance, activity) as Boolean
        }
        val windowState = if (activity == null) null else read("windowState") {
            val flexible = Class.forName("com.oplus.flexiblewindow.FlexibleWindowManager", false, null)
            flexible.getMethod("getFlexibleWindowState", Activity::class.java)
                .invoke(flexible.getMethod("getInstance").invoke(null), activity) as Int
        }
        val compatMode = if (activity == null) null else read("compatMode") {
            manager.getMethod("inOplusCompatMode", Configuration::class.java)
                .invoke(null, activity.resources.configuration) as Boolean
        }
        return Conditions(feature, configuration, level, allowed, windowState, compatMode, errors)
    }

    internal class Opening(
        val options: Bundle,
        private val callback: Any,
        private val offset: Method,
        val supportsPositionOffset: Boolean,
    ) {
        fun updatePosition(bounds: Rect, initialBounds: Rect) {
            offset.invoke(
                callback,
                PointF((bounds.left - initialBounds.left).toFloat(), (bounds.top - initialBounds.top).toFloat()),
            )
        }
    }

    fun open(
        activity: Activity,
        bitmap: Bitmap,
        bounds: Rect,
        radius: Float,
        backgroundColor: Int,
        listener: ViewAnimationListener,
    ): Opening? {
        val api = protocol ?: return null
        val config = Bundle().apply {
            putParcelable("view_seamless_bitmap", bitmap)
            putParcelable("view_seamless_rect", Rect(bounds))
            putFloat("view_seamless_radius", radius)
            putInt("view_seamless_color", backgroundColor)
            // 来源是窗口 View，不是独立卡片 View；不能让系统隐藏整页 Compose 内容。
            putBoolean("view_seamless_view_visible", true)
        }
        return try {
            val events = BiConsumer<String, Boolean> { event, isOpen ->
                val mode = if (isOpen) 0 else 1
                if (event == "start") listener.onAnimationStart(mode) else listener.onAnimationEnd(mode)
            }
            val callback = Class.forName(
                "com.perol.pixez.shared.ui.navigation.animation.system.OplusAnimationCallbackBridge",
            ).getMethod("create", BiConsumer::class.java).invoke(null, events)
                ?: return null
            val offset = api.callback.getMethod("setPositionOffset", PointF::class.java)
            if (api.open.invoke(null, activity.window.decorView, activity, config, callback) != true) {
                Napier.i(tag = TAG) { "系统拒绝旧无缝展开，回退应用内转场" }
                return null
            }
            val options = api.fromBundle.invoke(null, config) as? ActivityOptions
            if (options == null || api.remoteTransition.invoke(options) == null) {
                cancel()
                Napier.w(tag = TAG) { "旧无缝协议未返回远程转场" }
                null
            } else {
                Opening(
                    options.toBundle().apply { putBoolean(OPEN, true) },
                    callback,
                    offset,
                    springPositionOffsetSupported(),
                )
            }
        } catch (error: ReflectiveOperationException) {
            Napier.w(tag = TAG, throwable = cause(error)) { "旧无缝展开配置失败" }
            cancel()
            null
        } catch (error: SecurityException) {
            Napier.w(tag = TAG, throwable = error) { "系统拒绝旧无缝展开配置" }
            cancel()
            null
        } catch (error: LinkageError) {
            Napier.w(tag = TAG, throwable = error) { "系统回调契约不可用" }
            cancel()
            null
        }
    }

    fun prepareReturn(target: Activity, opening: Opening?, initialBounds: Rect?, bounds: Rect?, enabled: Boolean) {
        val api = protocol ?: return
        if (enabled && opening != null && initialBounds != null && bounds != null &&
            (bounds == initialBounds || opening.supportsPositionOffset)
        ) {
            try {
                opening.updatePosition(bounds, initialBounds)
                return
            } catch (error: ReflectiveOperationException) {
                Napier.w(tag = TAG, throwable = cause(error)) { "返回位置偏移更新失败，跳过卡片收回" }
            } catch (error: SecurityException) {
                Napier.w(tag = TAG, throwable = error) { "系统拒绝返回位置偏移，跳过卡片收回" }
            }
        }
        try {
            api.skipBack.invoke(null, target)
        } catch (error: ReflectiveOperationException) {
            Napier.w(tag = TAG, throwable = cause(error)) { "无法取消旧无缝返回落点" }
        } catch (error: SecurityException) {
            Napier.w(tag = TAG, throwable = error) { "系统拒绝取消旧无缝返回落点" }
        }
    }

    private fun springPositionOffsetSupported(): Boolean = try {
        Class.forName("android.app.viewseamless.ViewSeamlessRemoteTransition", false, null)
            .getDeclaredField("SPRING").apply { isAccessible = true }.getBoolean(null)
    } catch (_: ReflectiveOperationException) {
        false
    } catch (_: SecurityException) {
        false
    }

    fun cancel() {
        val api = protocol ?: return
        try {
            val accepted = api.cancel.invoke(null)
            Napier.i(tag = TAG) { "结束旧无缝动画请求: accepted=$accepted" }
        } catch (error: ReflectiveOperationException) {
            Napier.w(tag = TAG, throwable = cause(error)) { "无法结束旧无缝动画" }
        } catch (error: SecurityException) {
            Napier.w(tag = TAG, throwable = error) { "系统拒绝结束旧无缝动画" }
        }
    }

    private fun cause(error: ReflectiveOperationException): Throwable =
        (error as? InvocationTargetException)?.targetException ?: error
}
