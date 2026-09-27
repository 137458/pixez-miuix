package com.perol.pixez.shared.platform

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.Path
import okio.Path.Companion.toPath

/**
 * 动图 Zip 流式解压测试（desktop 实现，android 实现同构）：
 * 验证逐 entry 流式落盘、目录层级剥离、目录项跳过与空包处理；
 * 实现不再将 zip 与全部帧字节整体驻留内存（D-7）。
 */
class UgoiraZipExtractorTest {

    private fun zipOf(vararg entries: Pair<String, ByteArray>): Path {
        val zipFile = Files.createTempFile("ugoira-test", ".zip")
        ZipOutputStream(zipFile.toFile().outputStream()).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return zipFile.toAbsolutePath().toString().toPath()
    }

    private fun framesDir(): Path = Files.createTempDirectory("ugoira-frames").toAbsolutePath().toString().toPath()

    @Test
    fun `逐 entry 落盘并以帧路径返回且内容完整`() {
        val zipPath = zipOf(
            "000000.jpg" to byteArrayOf(1, 2, 3),
            "000001.jpg" to byteArrayOf(4, 5),
        )
        val dir = framesDir()

        val frames = UgoiraZipExtractor().extractFrames(zipPath, dir)

        assertEquals(2, frames.size)
        val written = frames.map { it.toFile() }
        assertTrue(written.all { it.exists() }, "返回的帧路径应已落盘")
        assertTrue(written[0].readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(written[1].readBytes().contentEquals(byteArrayOf(4, 5)))
    }

    @Test
    fun `目录层级被剥离仅保留文件名`() {
        val zipPath = zipOf(
            "ugoira/frames/000000.jpg" to byteArrayOf(9),
            "nested/deep/dir/000001.jpg" to byteArrayOf(8),
        )
        val dir = framesDir()

        val frames = UgoiraZipExtractor().extractFrames(zipPath, dir)

        assertEquals(setOf("000000.jpg", "000001.jpg"), frames.map { it.name }.toSet(), "文件名应剥离目录层级")
        assertEquals(9, (dir / "000000.jpg".toPath()).toFile().readBytes()[0].toInt())
    }

    @Test
    fun `目录条目被跳过`() {
        val zipPath = zipOf(
            "frames/" to ByteArray(0),
            "frames/000000.jpg" to byteArrayOf(7),
        )
        val dir = framesDir()

        val frames = UgoiraZipExtractor().extractFrames(zipPath, dir)

        assertEquals(setOf("000000.jpg"), frames.map { it.name }.toSet())
        assertFalse(File(dir.toFile(), "frames").exists(), "目录条目不应落盘")
    }

    @Test
    fun `空 zip 返回空列表`() {
        val zipPath = zipOf()
        val dir = framesDir()

        val frames = UgoiraZipExtractor().extractFrames(zipPath, dir)

        assertTrue(frames.isEmpty())
    }
}
