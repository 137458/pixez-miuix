package com.perol.pixez.shared.platform

import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.stringByAppendingPathComponent

actual fun getDefaultPictureDirectory(): String =
    (NSHomeDirectory() as NSString).stringByAppendingPathComponent("Pictures/PixEz")
