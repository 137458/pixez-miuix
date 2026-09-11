package com.perol.pixez.shared.platform

import com.perol.pixez.shared.data.model.Illust
import java.io.File

actual object LocalIllustResolver {
    actual fun findDownloadedFileUri(
        illust: Illust,
        pageIndex: Int,
        customBasePath: String?,
    ): String? {
        return runCatching {
            val customDir = if (!customBasePath.isNullOrBlank()) {
                val custom = File(customBasePath.trim())
                if (custom.exists() && custom.isDirectory) custom else null
            } else null

            val baseDir = customDir ?: run {
                val userHome = System.getProperty("user.home") ?: return@runCatching null
                val picturesDir = File(userHome, "Pictures")
                File(picturesDir, "PixEz")
            }

            if (!baseDir.exists() || !baseDir.isDirectory) return@runCatching null

            val searchDirs = mutableListOf(baseDir)
            val authorSubDirName = "${FileNamePolicy.sanitizeSegment(illust.user.name)}_${illust.user.id}"
            val authorSubDir = File(baseDir, authorSubDirName)
            if (authorSubDir.exists() && authorSubDir.isDirectory) {
                searchDirs.add(authorSubDir)
            }

            val extensions = listOf("png", "jpg", "jpeg", "gif", "webp")
            val candidateNames = mutableListOf<String>()
            for (ext in extensions) {
                candidateNames.add("${illust.id}_p${pageIndex}.${ext}")
                if (pageIndex == 0) {
                    candidateNames.add("${illust.id}.${ext}")
                }
            }

            for (dir in searchDirs) {
                for (name in candidateNames) {
                    val candidate = File(dir, name)
                    if (candidate.exists() && candidate.length() > 0) {
                        return@runCatching candidate.toURI().toString()
                    }
                }

                val files = dir.listFiles() ?: continue
                val idStr = illust.id.toString()
                val pageMarker = "_p${pageIndex}."
                for (file in files) {
                    if (file.isFile && file.length() > 0) {
                        val name = file.name
                        if (name.contains(idStr)) {
                            if (name.contains(pageMarker) || (pageIndex == 0 && !name.contains("_p"))) {
                                return@runCatching file.toURI().toString()
                            }
                        }
                    }
                }
            }

            null
        }.getOrNull()
    }
}
