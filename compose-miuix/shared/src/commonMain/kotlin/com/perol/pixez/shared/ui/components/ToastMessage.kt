package com.perol.pixez.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text

/**
 * 轻量 Toast 提示，自动显示后消失。
 *
 * 适用于下载成功/失败等不需要用户交互的瞬时反馈。
 *
 * @param message 提示文本，为 null 或空字符串时不显示
 * @param durationMillis 显示时长，默认 2 秒
 * @param onDismiss 提示消失后的回调，可用于清空外部状态
 */
@Composable
fun ToastMessage(
    message: String?,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2000L,
    onDismiss: () -> Unit = {},
) {
    // visible 控制动画状态；当 message 变化时触发进入/退出动画。
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            visible = true
            delay(durationMillis)
            visible = false
            onDismiss()
        } else {
            visible = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 64.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message ?: "",
                    color = Color.White,
                )
            }
        }
    }
}
