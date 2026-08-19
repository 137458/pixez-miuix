package com.perol.pixez.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory

/**
 * Desktop(JVM) 应用入口，支持接收命令行参数（如 pixiv:// 回调链接）。
 */
fun main(args: Array<String>) = application {
    val dependencies = remember {
        AppDependencies(
            driverFactory = DriverFactory(),
            settingsFactory = SettingsFactory(),
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
