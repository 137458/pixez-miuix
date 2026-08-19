package com.perol.pixez.shared.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
