package com.perol.pixez.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory

/**
 * Desktop(JVM) 应用入口。
 */
fun main() = application {
    val dependencies = remember {
        AppDependencies(
            driverFactory = DriverFactory(),
            settingsFactory = SettingsFactory(),
        )
    }
    Window(
        onCloseRequest = {
            dependencies.close()
            exitApplication()
        },
        title = "PixEz",
    ) {
        PixEzApp(dependencies)
    }
}
