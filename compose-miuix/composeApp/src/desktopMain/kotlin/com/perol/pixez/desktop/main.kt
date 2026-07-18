package com.perol.pixez.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.perol.pixez.PixEzApp

/**
 * Desktop(JVM) 应用入口。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PixEz",
    ) {
        PixEzApp()
    }
}
