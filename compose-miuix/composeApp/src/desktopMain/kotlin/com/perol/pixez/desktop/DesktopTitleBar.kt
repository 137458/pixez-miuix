package com.perol.pixez.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 自定义沉浸式无边框标题栏 (Custom TitleBar)。
 *
 * 提供手机版同款应用图标、标题展示、拖拽移动、双击最大化/还原以及 Windows 原生标准控制按钮（最小化、最大化、关闭）。
 */
@Composable
fun WindowScope.DesktopTitleBar(
    windowState: WindowState,
    appIcon: Painter?,
    title: String = "PixEz",
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    WindowDraggableArea(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color.Transparent)
            .pointerInput(windowState) {
                detectTapGestures(
                    onDoubleTap = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 左侧：App 图标与标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (appIcon != null) {
                    Image(
                        painter = appIcon,
                        contentDescription = "PixEz Icon",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2196F3), CircleShape),
                    )
                }

                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }

            // 右侧：最小化、最大化/还原、关闭 控制按钮
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 最小化
                WindowControlButton(
                    onClick = { windowState.isMinimized = true },
                    hoverColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = "─",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }

                // 最大化 / 还原
                WindowControlButton(
                    onClick = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    hoverColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = if (isMaximized) "⧉" else "□",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }

                // 关闭
                WindowControlButton(
                    onClick = onCloseRequest,
                    hoverColor = Color(0xFFE81123),
                    isClose = true,
                ) { isHovered ->
                    Text(
                        text = "✕",
                        fontSize = 12.sp,
                        color = if (isHovered) Color.White else MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    hoverColor: Color,
    isClose: Boolean = false,
    content: @Composable (isHovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedBg by animateColorAsState(
        targetValue = if (isHovered) hoverColor else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
    )

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(animatedBg),
        contentAlignment = Alignment.Center,
    ) {
        content(isHovered)
    }
}
