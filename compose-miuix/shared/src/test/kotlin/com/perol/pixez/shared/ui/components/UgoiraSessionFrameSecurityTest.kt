package com.perol.pixez.shared.ui.components

import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UgoiraSessionFrameSecurityTest {

    @Test
    fun `resolveSafeUgoiraFramePath 拦截目录穿越与多级路径仅放行单级帧文件名`() {
        val framesDir = Files.createTempDirectory("ugoira-frames-guard").toAbsolutePath().toString().toPath()

        assertNotNull(resolveSafeUgoiraFramePath(framesDir, "000000.jpg"))
        assertEquals(framesDir / "000000.jpg", resolveSafeUgoiraFramePath(framesDir, "000000.jpg"))

        assertNull(resolveSafeUgoiraFramePath(framesDir, "../../secret.png"))
        assertNull(resolveSafeUgoiraFramePath(framesDir, "..\\..\\secret.png"))
        assertNull(resolveSafeUgoiraFramePath(framesDir, "sub/000000.jpg"))
        assertNull(resolveSafeUgoiraFramePath(framesDir, ".."))
        assertNull(resolveSafeUgoiraFramePath(framesDir, ""))
        assertNull(resolveSafeUgoiraFramePath(framesDir, "C:evil.png"))
    }
}
