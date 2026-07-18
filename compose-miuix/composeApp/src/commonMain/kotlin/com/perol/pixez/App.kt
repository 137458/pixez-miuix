package com.perol.pixez

import androidx.compose.runtime.Composable
import com.perol.pixez.shared.App
import com.perol.pixez.shared.AppDependencies

/**
 * composeApp 的共享入口，仅委托给 shared 模块。
 */
@Composable
fun PixEzApp(dependencies: AppDependencies) {
    App(dependencies)
}
