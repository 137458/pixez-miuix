package com.perol.pixez.desktop.platform

import org.junit.Test
import kotlin.test.assertTrue

class WindowsMicaTest {

    @Test
    fun `detects Windows 11 build correctly on Windows`() {
        val osName = System.getProperty("os.name").orEmpty()
        if (osName.contains("Windows", ignoreCase = true)) {
            val build = WindowsMica.windowsBuildNumber
            println("Detected Windows build number: $build")
            assertTrue(build > 0, "Build number should be positive on Windows")
            // Windows 11 starts at 22000
            if (build >= 22000) {
                assertTrue(WindowsMica.isWindows11)
            }
        }
    }

    @Test
    fun `DwmApi instance is available on Windows`() {
        val osName = System.getProperty("os.name").orEmpty()
        if (osName.contains("Windows", ignoreCase = true)) {
            assertTrue(WindowsMica.DwmApi.INSTANCE != null, "DwmApi should load successfully on Windows")
        }
    }
}
