package com.perol.pixez.shared.ui.screens

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DataExportFileHelperDesktopTest {

    @Test
    fun `未经原生选择器批准的导出目录外绝对路径被拒绝`() {
        val outsideDir = Files.createTempDirectory("pixez-export-outside").toFile()
        try {
            val outsidePath = java.io.File(outsideDir, "steal.json").absolutePath
            val writeResult = writeExportFile(outsidePath, "[]")
            assertTrue(writeResult.isFailure, "未批准的外部绝对路径不应允许写入: $outsidePath")
            val exception = assertIs<DataExportException>(writeResult.exceptionOrNull())
            assertEquals(DataExportErrorCode.PathOutsideExportDir, exception.code)

            java.io.File(outsidePath).writeText("[]")
            val readResult = readExportFile(outsidePath)
            assertTrue(readResult.isFailure, "未批准的外部绝对路径不应允许读取: $outsidePath")
            val readEx = assertIs<DataExportException>(readResult.exceptionOrNull())
            assertEquals(DataExportErrorCode.PathOutsideExportDir, readEx.code)
        } finally {
            outsideDir.deleteRecursively()
        }
    }
}
