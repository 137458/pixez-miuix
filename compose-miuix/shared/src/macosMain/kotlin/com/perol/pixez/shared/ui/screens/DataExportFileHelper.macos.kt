package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

internal actual fun writeExportFile(path: String, content: String): Result<Unit> = runCatchingNonCancel {
    val baseDir = getExportBaseDirectory()
    val fullPath = if (path.startsWith("/")) path else (baseDir as NSString).stringByAppendingPathComponent(path)
    NSFileManager.defaultManager.createDirectoryAtPath(
        baseDir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    (content as NSString).writeToFile(fullPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    Unit
}

internal actual fun readExportFile(path: String): Result<String> = runCatchingNonCancel {
    val baseDir = getExportBaseDirectory()
    val fullPath = if (path.startsWith("/")) path else (baseDir as NSString).stringByAppendingPathComponent(path)
    NSString.stringWithContentsOfFile(fullPath, encoding = NSUTF8StringEncoding, error = null) ?: ""
}

internal actual fun getExportBaseDirectory(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val docs = (paths.firstOrNull() as? String) ?: ""
    return (docs as NSString).stringByAppendingPathComponent("PixEz/export")
}
