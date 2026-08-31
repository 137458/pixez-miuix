package com.perol.pixez.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.mayakapps.compose.windowstyler.WindowBackdrop
import com.mayakapps.compose.windowstyler.WindowStyle
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.ui.navigation.RootComponent
import io.github.aakira.napier.Napier
import java.awt.Dimension
import java.awt.SystemTray
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 托盘图标回退绘制（当资源未就绪时使用）。
 */
private object PixEzTrayPainter : Painter() {
    override val intrinsicSize: Size = Size(32f, 32f)

    override fun DrawScope.onDraw() {
        drawCircle(color = Color(0xFF2196F3))
        drawCircle(color = Color.White, radius = size.minDimension / 4.5f)
    }
}

/**
 * window-styler 0.3.2 relies on ComposeWindow's former `delegate` field. Compose 1.11 removed it,
 * so Mica must be skipped rather than failing later on the AWT event thread.
 */
private val supportsWindowStylerMica: Boolean by lazy {
    runCatching { ComposeWindow::class.java.getDeclaredField("delegate") }.isSuccess
}

/**
 * Desktop(JVM) 应用入口，集成系统代理、单实例回调转发、托盘与 Windows 11 Mica 材质。
 */
fun main(args: Array<String>) {
    System.setProperty("skiko.fps", "0")
    System.setProperty("skiko.vsync.enabled", "true")
    System.setProperty("skiko.hardwareAcceleration", "true")
    System.setProperty("skiko.directx.enabled", "true")
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("sun.java2d.d3d", "true")

    DesktopProxySelector.install()
    when (val acquisition = SingleInstanceCoordinator.acquireOrForward(args.toList())) {
        is SingleInstanceCoordinator.Acquisition.Primary -> application {
            PixEzDesktopApplication(args.toList(), acquisition.coordinator)
        }
        SingleInstanceCoordinator.Acquisition.ForwardedToPrimary -> Unit
        is SingleInstanceCoordinator.Acquisition.Unavailable -> {
            System.err.println("PixEz could not start: ${acquisition.reason}")
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun androidx.compose.ui.window.ApplicationScope.PixEzDesktopApplication(
    initialArguments: List<String>,
    singleInstance: SingleInstanceCoordinator,
) {
    val dependencies = remember {
        AppDependencies(
            driverFactory = DriverFactory(),
            settingsFactory = SettingsFactory(),
        )
    }
    val lifecycle = remember { LifecycleRegistry() }
    val rootComponent = remember {
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle),
            settingsRepository = dependencies.settingsRepository,
        )
    }
    val scope = rememberCoroutineScope()
    val restoredPlacement = remember { DesktopWindowPreferences.load(dependencies.settingsRepository) }
    val windowState = rememberWindowState(
        size = restoredPlacement.size,
        position = restoredPlacement.position,
        placement = restoredPlacement.placement,
    )
    val pendingLaunches = remember { Channel<List<String>>(Channel.UNLIMITED) }
    val trayAvailable = remember { SystemTray.isSupported() }
    var isWindowVisible by remember { mutableStateOf(true) }
    var focusRequestVersion by remember { mutableIntStateOf(0) }
    var isShuttingDown by remember { mutableStateOf(false) }

    fun saveWindowPlacement() {
        DesktopWindowPreferences.save(
            dependencies.settingsRepository,
            DesktopWindowPlacement(windowState.size, windowState.position, windowState.placement),
        )
    }

    fun shutdownAndExit() {
        if (isShuttingDown) return
        isShuttingDown = true
        saveWindowPlacement()
        runCatching { singleInstance.close() }
        runCatching { lifecycle.destroy() }
        runCatching { dependencies.close() }
        exitApplication()
    }

    fun showWindow() {
        isWindowVisible = true
        focusRequestVersion++
    }

    suspend fun handleLaunchArguments(arguments: List<String>) {
        val loginArgument = arguments.firstOrNull(::isLoginLaunchArgument) ?: return
        runCatching { dependencies.accountRepository.login(loginArgument) }
            .onFailure { Napier.e("Desktop login callback handling failed") }
    }

    DisposableEffect(lifecycle) {
        lifecycle.resume()
        onDispose {
            if (!isShuttingDown) lifecycle.destroy()
        }
    }
    DisposableEffect(singleInstance) {
        val listener = singleInstance.addLaunchListener { arguments ->
            pendingLaunches.trySend(arguments)
        }
        onDispose { listener.close() }
    }
    DisposableEffect(windowState) {
        onDispose { saveWindowPlacement() }
    }

    LaunchedEffect(dependencies) {
        dependencies.settingsFactory.migrateIfNeeded()
        dependencies.settingsRepository.notifyChanged()
        dependencies.warmupAsync(scope)
    }
    LaunchedEffect(windowState) {
        snapshotFlow {
            DesktopWindowPlacement(windowState.size, windowState.position, windowState.placement)
        }.debounce(400).collectLatest { placement ->
            DesktopWindowPreferences.save(dependencies.settingsRepository, placement)
        }
    }
    LaunchedEffect(Unit) {
        handleLaunchArguments(initialArguments)
        pendingLaunches.receiveAsFlow().collect { arguments ->
            showWindow()
            handleLaunchArguments(arguments)
        }
    }

    val appIconPainter: Painter? = remember {
        runCatching {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
                ?: PixEzTrayPainter::class.java.getResourceAsStream("/icon.png")
            stream?.use { BitmapPainter(loadImageBitmap(it)) }
        }.getOrNull()
    }

    if (trayAvailable) {
        Tray(
            icon = appIconPainter ?: PixEzTrayPainter,
            tooltip = "PixEz MIUIX",
            onAction = ::showWindow,
            menu = {
                Item("打开主界面", onClick = ::showWindow)
                Item("下载任务", onClick = {
                    showWindow()
                    rootComponent.onDownloadTaskClicked()
                })
                Item("退出", onClick = ::shutdownAndExit)
            },
        )
    }

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (trayAvailable && dependencies.settingsRepository.closeToTray) {
                    saveWindowPlacement()
                    isWindowVisible = false
                } else {
                    shutdownAndExit()
                }
            },
            state = windowState,
            title = "PixEz",
            icon = appIconPainter ?: PixEzTrayPainter,
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
            window.minimumSize = Dimension(
                DesktopWindowPreferences.MinimumWidth,
                DesktopWindowPreferences.MinimumHeight,
            )

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (dependencies.settingsRepository.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemDark
            }

            if (supportsWindowStylerMica) {
                WindowStyle(
                    isDarkTheme = isDark,
                    backdropType = WindowBackdrop.Mica,
                )
            }

            DisposableEffect(window) {
                val mouseListener = object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(event: java.awt.event.MouseEvent) {
                        if (event.button == 4) rootComponent.onBack()
                    }
                }
                window.addMouseListener(mouseListener)
                onDispose { window.removeMouseListener(mouseListener) }
            }
            LaunchedEffect(focusRequestVersion) {
                window.toFront()
                window.requestFocus()
            }

            PixEzApp(
                dependencies = dependencies,
                rootComponent = rootComponent,
            )
        }
    }
}

private fun isLoginLaunchArgument(argument: String): Boolean =
    argument.startsWith("pixiv://", ignoreCase = true) || argument.contains("code=")
