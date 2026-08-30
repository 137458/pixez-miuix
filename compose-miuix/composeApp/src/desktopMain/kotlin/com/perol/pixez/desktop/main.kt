package com.perol.pixez.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
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
 * PixEz 桌面系统托盘图标绘制。
 */
private object PixEzTrayPainter : Painter() {
    override val intrinsicSize: Size = Size(32f, 32f)

    override fun DrawScope.onDraw() {
        drawCircle(color = Color(0xFF2196F3))
        drawCircle(color = Color.White, radius = size.minDimension / 4.5f)
    }
}

/**
 * Desktop(JVM) 应用入口，集成系统托盘、全局快捷键、鼠标侧键导航、命令行参数处理与网络预热。
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

    var isWindowVisible by remember { mutableStateOf(true) }

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

    // 系统托盘集成
    Tray(
        icon = PixEzTrayPainter,
        tooltip = "PixEz MIUIX",
        onAction = { isWindowVisible = true },
        menu = {
            Item("打开主界面", onClick = { isWindowVisible = true })
            Item("下载任务", onClick = {
                isWindowVisible = true
                rootComponent.onDownloadTaskClicked()
            })
            Item("退出", onClick = {
                dependencies.close()
                exitApplication()
            })
        },
    )

    val windowState = rememberWindowState(size = DpSize(1100.dp, 750.dp))

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                dependencies.close()
                exitApplication()
            },
            state = windowState,
            title = "PixEz",
            onKeyEvent = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val isModifier = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                    when {
                        keyEvent.key == Key.Escape -> rootComponent.onBack()
                        isModifier && keyEvent.key == Key.F -> {
                            rootComponent.onSearchClicked("")
                            true
                        }
                        isModifier && keyEvent.key == Key.One -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Hello)
                            true
                        }
                        isModifier && keyEvent.key == Key.Two -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Ranking)
                            true
                        }
                        isModifier && keyEvent.key == Key.Three -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.New)
                            true
                        }
                        isModifier && keyEvent.key == Key.Four -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Spotlight)
                            true
                        }
                        isModifier && (keyEvent.key == Key.J || keyEvent.key == Key.D) -> {
                            rootComponent.onDownloadTaskClicked()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        ) {
            window.minimumSize = Dimension(600, 500)

            DisposableEffect(window) {
                val mouseListener = object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(e: java.awt.event.MouseEvent) {
                        // 鼠标侧键（Back 键：AWT button 4）直接触发全局返回
                        if (e.button == 4) {
                            rootComponent.onBack()
                        }
                    }
                }
                window.addMouseListener(mouseListener)
                onDispose {
                    window.removeMouseListener(mouseListener)
                }
            }

            PixEzApp(
                dependencies = dependencies,
                rootComponent = rootComponent,
            )
        }
    }
}

