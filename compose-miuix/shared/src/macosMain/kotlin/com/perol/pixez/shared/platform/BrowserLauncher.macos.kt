package com.perol.pixez.shared.platform

import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

actual fun openBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    NSWorkspace.sharedWorkspace.openURL(nsUrl)
}
