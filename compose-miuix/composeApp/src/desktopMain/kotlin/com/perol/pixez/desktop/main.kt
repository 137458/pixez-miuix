package com.perol.pixez.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.perol.pixez.PixEzApp
import com.perol.pixez.desktop.platform.WindowsMica
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.i18n.AppStrings
import io.github.aakira.napier.Napier
import java.awt.Dimension
import java.awt.SystemTray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
 * Desktop(JVM) 应用入口，集成系统代理、单实例回调转发、托盘与 Windows 11 Mica 材质。
 */
fun main(args: Array<String>) {
    // 顶层未捕获异常兜底：jpackage 发布版无控制台，崩溃信息写日志文件避免静默丢失
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("user.home"), ".pixez", "logs"))
            java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("user.home"), ".pixez", "logs", "crash-" + System.currentTimeMillis() + ".log"),
                "Thread: " + thread.name + "\n" + throwable.stackTraceToString(),
            )
        }
        System.err.println("Uncaught exception on ${thread.name}: ${throwable.stackTraceToString()}")
    }
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

    var lastHandledClipboardText by remember { mutableStateOf<String?>(null) }

    fun dispatchDeepLinkText(rawText: String?) {
        val parsed = com.perol.pixez.shared.navigation.DeepLinkParser.parse(rawText) ?: return
        com.perol.pixez.shared.navigation.DeepLinkParser.dispatch(
            parsed = parsed,
            rootComponent = rootComponent,
            onOAuthCode = { code ->
                scope.launch {
                    try {
                        dependencies.accountRepository.loginWithCode(code)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        Napier.e("Desktop OAuth login callback failed", t)
                    }
                }
            },
        )
    }

    fun handleLaunchArguments(arguments: List<String>) {
        for (arg in arguments) {
            val parsed = com.perol.pixez.shared.navigation.DeepLinkParser.parse(arg)
            if (parsed != null) {
                dispatchDeepLinkText(arg)
                return
            }
        }
    }

    fun checkClipboardOnFocus() {
        // Windows 剪贴板读取是 OLE 操作，被其它进程占用时可阻塞 EDT 约 1s：切 IO 线程读取，结果回主线程分发
        scope.launch(Dispatchers.IO) {
            val text = runCatching { com.perol.pixez.shared.platform.IllustClipboard().getText()?.trim() }.getOrNull()
            if (text.isNullOrBlank() || text == lastHandledClipboardText) return@launch
            val hasIllust = text.contains("artworks/") || text.contains("illust_id=")
            val hasUser = text.contains("users/")
            val hasScheme = text.startsWith("pixiv://", true) || text.startsWith("pixez://", true)
            if (!hasIllust && !hasUser && !hasScheme) return@launch
            withContext(Dispatchers.Main) {
                // 二次核对：IO 期间剪贴板可能已变化
                if (text != lastHandledClipboardText) {
                    lastHandledClipboardText = text
                    dispatchDeepLinkText(text)
                }
            }
        }
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
        // 注册表写入与旧设置文件迁移均为阻塞 I/O，切至 IO 调度器避免卡 UI（见 SettingsFactory 注释）
        withContext(Dispatchers.IO) {
            WindowsProtocolRegistrar.registerIfNeeded()
            dependencies.settingsFactory.migrateIfNeeded()
        }
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

    // 托盘文案随 languageNum 实时重建：key(languageNum) 会先 dispose 旧 Tray
    // （Compose Desktop 的 Tray 在 onDispose 中从 SystemTray 移除 TrayIcon），再创建新托盘，避免 AWT 资源泄漏。
    val trayLanguageNum = dependencies.settingsRepository.languageNum
    val trayStrings = remember(trayLanguageNum) { AppStrings.fromLanguageNum(trayLanguageNum) }
    if (trayAvailable) {
        key(trayLanguageNum) {
            Tray(
                icon = appIconPainter ?: PixEzTrayPainter,
                tooltip = "PixEz MIUIX",
                onAction = ::showWindow,
                menu = {
                    Item(trayStrings.trayOpenMainWindow, onClick = ::showWindow)
                    Item(trayStrings.trayDownloadTasks, onClick = {
                        showWindow()
                        rootComponent.onDownloadTaskClicked()
                    })
                    Item(trayStrings.trayQuit, onClick = ::shutdownAndExit)
                },
            )
        }
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
                        keyEvent.key == Key.Escape -> {
                            com.perol.pixez.shared.platform.DesktopBackDispatcher.dispatchBack() ||
                                rootComponent.onBack()
                        }
                        isModifier && keyEvent.key == Key.F -> {
                            rootComponent.onSearchClicked("")
                            true
                        }
                        isModifier && keyEvent.key == Key.One -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Hello)
                            true
                        }
                        isModifier && keyEvent.key == Key.Two -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Search)
                            true
                        }
                        isModifier && keyEvent.key == Key.Three -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Ranking)
                            true
                        }
                        isModifier && keyEvent.key == Key.Four -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.New)
                            true
                        }
                        isModifier && keyEvent.key == Key.Five -> {
                            rootComponent.onMainTabSelected(RootComponent.MainTab.Spotlight)
                            true
                        }
                        isModifier && (keyEvent.key == Key.J || keyEvent.key == Key.D) -> {
                            rootComponent.onDownloadTaskClicked()
                            true
                        }
                        isModifier && keyEvent.key == Key.H -> {
                            rootComponent.onHistoryClicked()
                            true
                        }
                        isModifier && keyEvent.key == Key.B -> {
                            rootComponent.onBookTagClicked()
                            true
                        }
                        isModifier && keyEvent.key == Key.N -> {
                            rootComponent.onNovelBrowseClicked()
                            true
                        }
                        (isModifier && keyEvent.key == Key.R) || keyEvent.key == Key.F5 -> {
                            dependencies.settingsRepository.notifyFilterChanged()
                            rootComponent.onTabReselected(rootComponent.selectedTab.value)
                            true
                        }
                        isModifier && keyEvent.key == Key.Comma -> {
                            rootComponent.onSettingsClicked()
                            true
                        }
                        isModifier && keyEvent.key == Key.W -> {
                            val handled = com.perol.pixez.shared.platform.DesktopBackDispatcher.dispatchBack() ||
                                rootComponent.onBack()
                            if (!handled) {
                                if (trayAvailable && dependencies.settingsRepository.closeToTray) {
                                    saveWindowPlacement()
                                    isWindowVisible = false
                                } else {
                                    shutdownAndExit()
                                }
                            }
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

            DisposableEffect(window, isDark) {
                WindowsMica.apply(window, isDark = isDark)
                onDispose {}
            }

            DisposableEffect(window) {
                val mouseListener = object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(event: java.awt.event.MouseEvent) {
                        if (event.button == 4) {
                            if (!com.perol.pixez.shared.platform.DesktopBackDispatcher.dispatchBack()) {
                                rootComponent.onBack()
                            }
                        }
                    }
                }
                val focusListener = object : java.awt.event.WindowFocusListener {
                    override fun windowGainedFocus(event: java.awt.event.WindowEvent?) {
                        checkClipboardOnFocus()
                    }

                    override fun windowLostFocus(event: java.awt.event.WindowEvent?) = Unit
                }
                window.addMouseListener(mouseListener)
                window.addWindowFocusListener(focusListener)
                onDispose {
                    window.removeMouseListener(mouseListener)
                    window.removeWindowFocusListener(focusListener)
                }
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
