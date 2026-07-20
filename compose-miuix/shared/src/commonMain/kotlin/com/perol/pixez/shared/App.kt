package com.perol.pixez.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.navigation.RootContent

/**
 * 共享的 Compose 应用入口。
 *
 * M3 阶段接入 Decompose 导航与 MIUIX 页面，替换 M1 的占位页面。
 */
@Composable
fun App(dependencies: AppDependencies) {
    val rootComponent = rememberRootComponent(dependencies)
    RootContent(
        component = rootComponent,
        illustRepository = dependencies.illustRepository,
        searchRepository = dependencies.searchRepository,
        userRepository = dependencies.userRepository,
        accountRepository = dependencies.accountRepository,
        bookmarkRepository = dependencies.bookmarkRepository,
        downloadRepository = dependencies.downloadRepository,
        downloadHistoryRepository = dependencies.downloadHistoryRepository,
        settingsRepository = dependencies.settingsRepository,
    )
}

@Composable
private fun rememberRootComponent(dependencies: AppDependencies): RootComponent {
    // Decompose 需要显式生命周期管理；Compose 组合进入时 resume，销毁时 destroy。
    val lifecycle = remember { LifecycleRegistry() }
    val component = remember(dependencies) {
        RootComponent(DefaultComponentContext(lifecycle))
    }
    DisposableEffect(Unit) {
        lifecycle.resume()
        onDispose { lifecycle.destroy() }
    }
    return component
}
