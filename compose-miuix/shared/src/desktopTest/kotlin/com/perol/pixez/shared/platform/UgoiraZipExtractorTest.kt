package com.perol.pixez.shared.platform

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 动图 Zip 解压测试（desktop 实现，android 实现同构）：
 * 验证帧内容提取、目录层级剥离、目录项跳过与空包处理。
 */
class UgoiraZipExtractorTest {

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `提取帧内容并以文件名为键`() {
        val zip = zipOf(
            "000000.jpg" to byteArrayOf(1, 2, 3),
            "000001.jpg" to byteArrayOf(4, 5),
        )

        val frames = UgoiraZipExtractor().extractFrames(zip)

        assertEquals(2, frames.size)
        assertContentEquals(byteArrayOf(1, 2, 3), frames["000000.jpg"])
        assertContentEquals(byteArrayOf(4, 5), frames["000001.jpg"])
    }

    @Test
    fun `目录层级被剥离仅保留文件名`() {
        val zip = zipOf(
            "ugoira/frames/000000.jpg" to byteArrayOf(9),
            "nested/deep/dir/000001.jpg" to byteArrayOf(8),
        )

        val frames = UgoiraZipExtractor().extractFrames(zip)

        assertEquals(setOf("000000.jpg", "000001.jpg"), frames.keys, "键应为剥离目录后的文件名")
        assertContentEquals(byteArrayOf(9), frames["000000.jpg"])
    }

    @Test
    fun `目录条目被跳过`() {
        val zip = zipOf(
            "frames/" to ByteArray(0),
            "frames/000000.jpg" to byteArrayOf(7),
        )

        val frames = UgoiraZipExtractor().extractFrames(zip)

        assertEquals(setOf("000000.jpg"), frames.keys)
    }

    @Test
    fun `同名文件名后条目覆盖前条目`() {
        val zip = zipOf(
            "a/000000.jpg" to byteArrayOf(1),
            "b/000000.jpg" to byteArrayOf(2),
        )

        val frames = UgoiraZipExtractor().extractFrames(zip)

        assertEquals(1, frames.size, "同 basename 的帧按 Map 写入语义后者覆盖前者")
        assertContentEquals(byteArrayOf(2), frames["000000.jpg"])
    }

    @Test
    fun `空 zip 返回空映射`() {
        val zip = zipOf()

        val frames = UgoiraZipExtractor().extractFrames(zip)

        assertTrue(frames.isEmpty())
    }
}
