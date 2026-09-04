package com.perol.pixez.shared.platform

/**
 * 跨平台动图 Zip 包解压与帧提取器。
 */
expect class UgoiraZipExtractor() {
    /**
     * 从下载的 Ugoira Zip 压缩包数据中提取出各个帧文件的二进制内容映射，Key 为文件名（如 "000000.jpg"）。
     */
    fun extractFrames(zipBytes: ByteArray): Map<String, ByteArray>
}
