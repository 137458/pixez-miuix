package com.perol.pixez.shared.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.aakira.napier.Napier

/**
 * Android 平台通过 Intent 打开系统浏览器。
 *
 * 需要调用方传入 [Context]；为简化 Compose 调用，这里使用一个可在 Application/Activity 中初始化的单例。
 */
object BrowserLauncherContext {
    var applicationContext: Context? = null
}

actual fun openBrowser(url: String) {
    val context = BrowserLauncherContext.applicationContext
        ?: throw IllegalStateException("BrowserLauncherContext 未初始化")
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Napier.e("打开浏览器失败 url=$url", e)
        throw e
    }
}
