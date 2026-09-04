package com.perol.pixez.shared.platform

import java.util.zip.ZipInputStream

actual class UgoiraZipExtractor actual constructor() {
    actual fun extractFrames(zipBytes: ByteArray): Map<String, ByteArray> {
        val map = mutableMapOf<String, ByteArray>()
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.substringAfterLast('/')
                    map[name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return map
    }
}
