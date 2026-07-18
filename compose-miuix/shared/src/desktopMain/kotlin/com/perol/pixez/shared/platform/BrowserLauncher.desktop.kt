package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.net.URI

/**
 * Desktop(JVM) 平台通过 [java.awt.Desktop] 打开系统浏览器。
 */
actual fun openBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            throw IllegalStateException("当前桌面环境不支持打开浏览器")
        }
    } catch (e: Exception) {
        Napier.e("打开浏览器失败 url=$url", e)
        throw e
    }
}
