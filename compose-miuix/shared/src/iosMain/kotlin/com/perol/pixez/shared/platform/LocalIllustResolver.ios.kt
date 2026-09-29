package com.perol.pixez.shared.platform

import com.perol.pixez.shared.data.model.Illust
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.stringByAppendingPathComponent

actual object LocalIllustResolver {
    actual fun findDownloadedFileUri(
        illust: Illust,
        pageIndex: Int,
        customBasePath: String?,
    ): String? {
        val documentsDir = (NSHomeDirectory() as NSString).stringByAppendingPathComponent("Documents")
        val baseDir = customBasePath ?: (documentsDir as NSString).stringByAppendingPathComponent("PixEz")
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(baseDir)) return null

        val authorSubDirName = "${FileNamePolicy.sanitizeSegment(illust.user.name)}_${illust.user.id}"
        val searchDirs = buildList {
            add(baseDir)
            for (sub in listOf(authorSubDirName, "NSFW_${authorSubDirName}", "NSFW")) {
                val subPath = (baseDir as NSString).stringByAppendingPathComponent(sub)
                if (fileManager.fileExistsAtPath(subPath)) add(subPath)
            }
        }

        val extensions = listOf("png", "jpg", "jpeg", "gif", "webp")
        for (dir in searchDirs) {
            for (ext in extensions) {
                val candidate = (dir as NSString).stringByAppendingPathComponent("${illust.id}_p${pageIndex}.${ext}")
                if (fileManager.fileExistsAtPath(candidate)) {
                    return "file://$candidate"
                }
                if (pageIndex == 0) {
                    val singleCandidate = (dir as NSString).stringByAppendingPathComponent("${illust.id}.${ext}")
                    if (fileManager.fileExistsAtPath(singleCandidate)) {
                        return "file://$singleCandidate"
                    }
                }
            }
        }
        return null
    }
}
