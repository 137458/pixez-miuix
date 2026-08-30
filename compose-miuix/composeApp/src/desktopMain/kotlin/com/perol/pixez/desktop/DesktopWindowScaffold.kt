package com.perol.pixez.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.perol.pixez.shared.data.settings.SettingsRepository
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.awt.Cursor
import java.awt.Point
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * 桌面端沉浸式窗口容器：
 * 集成自定义无边框标题栏、Windows 11 Mica / 亚克力底色联动与边缘自由拉伸缩放手柄。
 */
@Composable
fun WindowScope.DesktopWindowScaffold(
    windowState: WindowState,
    settingsRepository: SettingsRepository,
    appIcon: Painter?,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (settingsRepository.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    // 动态同步 Windows 11 Mica 材质与深浅色模式
    LaunchedEffect(isDark) {
        WindowsDwmHelper.applyMica(
            window = window,
            isDarkMode = isDark,
            backdropType = 2, // 2 = Mica
        )
    }

    // 挂载无边框窗口边缘鼠标拖拽调整大小监听器
    DisposableEffect(window, isMaximized) {
        if (!isMaximized) {
            val resizer = WindowResizeListener(window)
            window.addMouseListener(resizer)
            window.addMouseMotionListener(resizer)
            onDispose {
                window.removeMouseListener(resizer)
                window.removeMouseMotionListener(resizer)
            }
        } else {
            onDispose {}
        }
    }

    val windowShape = if (isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp)
    val windowBorderColor = if (isMaximized) Color.Transparent else MiuixTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(windowShape)
            .border(
                width = if (isMaximized) 0.dp else 1.dp,
                color = windowBorderColor,
                shape = windowShape,
            )
            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.88f)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DesktopTitleBar(
                windowState = windowState,
                appIcon = appIcon,
                title = "PixEz",
                onCloseRequest = onCloseRequest,
            )
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

/**
 * 无边框窗口 8 方向边缘鼠标缩放手柄。
 */
private class WindowResizeListener(private val window: Window) : MouseAdapter() {
    private val borderSize = 6
    private var dragCursor: Int = Cursor.DEFAULT_CURSOR
    private var startPos: Point? = null
    private var startBounds: java.awt.Rectangle? = null

    override fun mouseMoved(e: MouseEvent) {
        val cursor = getCursorForPos(e.x, e.y)
        window.cursor = Cursor.getPredefinedCursor(cursor)
    }

    override fun mousePressed(e: MouseEvent) {
        dragCursor = getCursorForPos(e.x, e.y)
        if (dragCursor != Cursor.DEFAULT_CURSOR) {
            startPos = e.locationOnScreen
            startBounds = window.bounds
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        val initialPos = startPos ?: return
        val initialBounds = startBounds ?: return
        val currentPos = e.locationOnScreen
        val dx = currentPos.x - initialPos.x
        val dy = currentPos.y - initialPos.y

        var newX = initialBounds.x
        var newY = initialBounds.y
        var newW = initialBounds.width
        var newH = initialBounds.height

        val minW = window.minimumSize?.width ?: 600
        val minH = window.minimumSize?.height ?: 500

        when (dragCursor) {
            Cursor.E_RESIZE_CURSOR -> newW = (initialBounds.width + dx).coerceAtLeast(minW)
            Cursor.S_RESIZE_CURSOR -> newH = (initialBounds.height + dy).coerceAtLeast(minH)
            Cursor.W_RESIZE_CURSOR -> {
                val candidateW = (initialBounds.width - dx).coerceAtLeast(minW)
                newX = initialBounds.x + (initialBounds.width - candidateW)
                newW = candidateW
            }
            Cursor.N_RESIZE_CURSOR -> {
                val candidateH = (initialBounds.height - dy).coerceAtLeast(minH)
                newY = initialBounds.y + (initialBounds.height - candidateH)
                newH = candidateH
            }
            Cursor.SE_RESIZE_CURSOR -> {
                newW = (initialBounds.width + dx).coerceAtLeast(minW)
                newH = (initialBounds.height + dy).coerceAtLeast(minH)
            }
            Cursor.SW_RESIZE_CURSOR -> {
                val candidateW = (initialBounds.width - dx).coerceAtLeast(minW)
                newX = initialBounds.x + (initialBounds.width - candidateW)
                newW = candidateW
                newH = (initialBounds.height + dy).coerceAtLeast(minH)
            }
            Cursor.NE_RESIZE_CURSOR -> {
                newW = (initialBounds.width + dx).coerceAtLeast(minW)
                val candidateH = (initialBounds.height - dy).coerceAtLeast(minH)
                newY = initialBounds.y + (initialBounds.height - candidateH)
                newH = candidateH
            }
            Cursor.NW_RESIZE_CURSOR -> {
                val candidateW = (initialBounds.width - dx).coerceAtLeast(minW)
                newX = initialBounds.x + (initialBounds.width - candidateW)
                newW = candidateW
                val candidateH = (initialBounds.height - dy).coerceAtLeast(minH)
                newY = initialBounds.y + (initialBounds.height - candidateH)
                newH = candidateH
            }
        }
        window.setBounds(newX, newY, newW, newH)
    }

    override fun mouseReleased(e: MouseEvent) {
        startPos = null
        startBounds = null
        window.cursor = Cursor.getDefaultCursor()
    }

    private fun getCursorForPos(x: Int, y: Int): Int {
        val w = window.width
        val h = window.height

        val onLeft = x <= borderSize
        val onRight = x >= w - borderSize
        val onTop = y <= borderSize
        val onBottom = y >= h - borderSize

        return when {
            onTop && onLeft -> Cursor.NW_RESIZE_CURSOR
            onTop && onRight -> Cursor.NE_RESIZE_CURSOR
            onBottom && onLeft -> Cursor.SW_RESIZE_CURSOR
            onBottom && onRight -> Cursor.SE_RESIZE_CURSOR
            onLeft -> Cursor.W_RESIZE_CURSOR
            onRight -> Cursor.E_RESIZE_CURSOR
            onTop -> Cursor.N_RESIZE_CURSOR
            onBottom -> Cursor.S_RESIZE_CURSOR
            else -> Cursor.DEFAULT_CURSOR
        }
    }
}
