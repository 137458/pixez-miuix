package com.perol.pixez.shared.platform

/**
 * 插画保存抽象接口，供生产端平台实现与 JVM 测试替身共用。
 */
interface IllustFileSaver {
    suspend fun save(
        fileName: String,
        bytes: ByteArray,
        subDir: String? = null,
        customBasePath: String? = null,
    ): String

    suspend fun saveFromTempFile(
        fileName: String,
        tempFilePath: okio.Path,
        subDir: String? = null,
        customBasePath: String? = null,
    ): String
}

/**
 * 跨平台保存插画图片字节到本地存储。
 *
 * Android 实现负责保存到公共 Pictures/PixEz 目录并刷新 MediaStore。
 */
expect class IllustSaver() : IllustFileSaver {
    override suspend fun save(
        fileName: String,
        bytes: ByteArray,
        subDir: String?,
        customBasePath: String?,
    ): String

    override suspend fun saveFromTempFile(
        fileName: String,
        tempFilePath: okio.Path,
        subDir: String?,
        customBasePath: String?,
    ): String
}
