package com.perol.pixez.shared.platform

import io.github.aakira.napier.Napier
import okio.Path

actual class UgoiraZipExtractor actual constructor() {
    actual fun extractFrames(zipPath: Path, framesDir: Path): List<Path> {
        // iOS 端动图解压尚未实现；显式日志标注功能缺失，避免静默黑帧难以定位
        Napier.w("UgoiraZipExtractor.extractFrames 未在 iOS 实现，动图播放不可用", tag = "Ugoira")
        return emptyList()
    }
}
