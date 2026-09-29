package com.perol.pixez.shared.platform

import android.os.Environment
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

            val defaultDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "PixEz",
            )
            val baseDirs = buildList {
                if (customDir != null) add(customDir)
                if (defaultDir.exists() && defaultDir.isDirectory && defaultDir != customDir) {
                    add(defaultDir)
                }
            }
            if (baseDirs.isEmpty()) return@runCatching null

            val authorSubDirName = "${FileNamePolicy.sanitizeSegment(illust.user.name)}_${illust.user.id}"
            val searchDirs = mutableListOf<File>()
            for (rootDir in baseDirs) {
                searchDirs.add(rootDir)
                listOf(authorSubDirName, "NSFW_${authorSubDirName}", "NSFW").forEach { subName ->
                    val sub = File(rootDir, subName)
                    if (sub.exists() && sub.isDirectory) searchDirs.add(sub)
                }
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
                // 1. 精确名称直接匹配
                for (name in candidateNames) {
                    val candidate = File(dir, name)
                    if (candidate.exists() && candidate.length() > 0) {
                        return@runCatching candidate.toURI().toString()
                    }
                }

                // 2. 自定义命名模板匹配（包含 id 与相应页码标识）
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
