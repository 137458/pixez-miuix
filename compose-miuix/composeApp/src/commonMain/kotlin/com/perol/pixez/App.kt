package com.perol.pixez

import androidx.compose.runtime.Composable
import com.perol.pixez.shared.App

/**
 * composeApp 的共享入口，仅委托给 shared 模块。
 */
@Composable
fun PixEzApp() {
    App()
}
