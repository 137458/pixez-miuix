package com.perol.pixez.shared.ui.navigation.animation

/**
 * 转场链路诊断日志：仅用于实机排查首次转场异常，排查结束后关闭 [ENABLED] 即可整体静默。
 * Android 上 println 走 logcat 的 System.out 标签，桌面端走 stdout。
 */
internal object NavTransitionLog {
    internal const val ENABLED = false

    private val base = kotlin.time.TimeSource.Monotonic.markNow()

    internal fun d(tag: String, message: () -> String) {
        if (!ENABLED) return
        println("NavTransition|$tag|+${base.elapsedNow().inWholeMilliseconds}ms|${message()}")
    }
}
