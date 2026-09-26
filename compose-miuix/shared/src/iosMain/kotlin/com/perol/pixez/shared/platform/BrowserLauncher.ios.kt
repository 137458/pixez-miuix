package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openBrowser(url: String) {
    if (!isBrowserUrlAllowed(url)) {
        Napier.w("openBrowser 拒绝非白名单 scheme: ${url.take(64)}")
        return
    }
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
