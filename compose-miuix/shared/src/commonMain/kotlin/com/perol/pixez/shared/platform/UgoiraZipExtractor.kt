package com.perol.pixez.shared.platform

import okio.Path

/**
 * 跨平台动图 Zip 包解压与帧提取器。
 */
expect class UgoiraZipExtractor() {
    /**
     * 从磁盘上的 Ugoira Zip 压缩包流式解压帧文件至 [framesDir]：
     * 逐 entry 读流写盘（峰值内存仅单个帧），不将 zip 与全部帧字节整体驻留内存。
     *
     * @return 已写入的帧文件路径列表（文件名已剥离目录层级，如 "000000.jpg"）。
     */
    fun extractFrames(zipPath: Path, framesDir: Path): List<Path>
}
