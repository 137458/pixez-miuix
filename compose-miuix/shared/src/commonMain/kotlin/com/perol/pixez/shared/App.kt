package com.perol.pixez.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 共享的 Compose 应用入口。
 * 当前为 M1 里程碑的最小可运行示例：Hello MIUIX。
 */
@Composable
fun App(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
) {
    // M1 先用 System 模式；后续接入 Monet 动态取色与跟随系统深色模式
    val controller = remember {
        ThemeController(ColorSchemeMode.System)
    }

    MiuixTheme(
        controller = controller,
    ) {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Hello MIUIX from PixEz",
                )
            }
        }
    }
}
