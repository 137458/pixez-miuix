package com.perol.pixez.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.ui.navigation.RootComponent
import java.awt.Dimension

/**
 * Desktop(JVM) 应用入口，支持接收命令行参数、网络预热、ESC 键返回与窗口尺寸限制。
 */
fun main(args: Array<String>) = application {
    val dependencies = remember {
        AppDependencies(
            driverFactory = DriverFactory(),
            settingsFactory = SettingsFactory(),
        )
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        dependencies.warmupAsync(scope)
    }

    val rootComponent = remember {
        RootComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            settingsRepository = dependencies.settingsRepository,
        )
    }

    LaunchedEffect(args) {
        val loginArg = args.firstOrNull { it.contains("pixiv://") || it.contains("code=") }
        if (!loginArg.isNullOrBlank()) {
            try {
                dependencies.accountRepository.login(loginArg)
                println("桌面端通过启动参数登录成功: $loginArg")
            } catch (e: Exception) {
                System.err.println("桌面端处理启动参数登录失败: $loginArg - ${e.message}")
            }
        }
    }

    val windowState = rememberWindowState(size = DpSize(1100.dp, 750.dp))

    Window(
        onCloseRequest = {
            dependencies.close()
            exitApplication()
        },
        state = windowState,
        title = "PixEz",
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                rootComponent.onBack()
            } else {
                false
            }
        },
    ) {
        window.minimumSize = Dimension(600, 500)
        PixEzApp(
            dependencies = dependencies,
            rootComponent = rootComponent,
        )
    }
}
