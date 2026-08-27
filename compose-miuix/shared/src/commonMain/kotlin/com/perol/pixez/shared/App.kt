package com.perol.pixez.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.navigation.RootContent

/**
 * 历史页通过该 CompositionLocal 获取 [HistoryRepository]，避免修改 RootContent 的签名。
 */
val LocalHistoryRepository = compositionLocalOf<HistoryRepository> {
    error("LocalHistoryRepository not provided")
}

/**
 * 共享的 Compose 应用入口。
 *
 * M3 阶段接入 Decompose 导航与 MIUIX 页面，替换 M1 的占位页面。
 */
@Composable
fun App(
    dependencies: AppDependencies,
    rootComponent: RootComponent? = null,
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { dependencies.httpClient.downloadClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(com.perol.pixez.shared.platform.getAppCacheDirectory() / "pixez_image_cache")
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    val component = rootComponent ?: rememberRootComponent(dependencies)
    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
        LocalHistoryRepository provides dependencies.historyRepository,
    ) {
        RootContent(
            component = component,
            illustRepository = dependencies.illustRepository,
            searchRepository = dependencies.searchRepository,
            userRepository = dependencies.userRepository,
            accountRepository = dependencies.accountRepository,
            bookmarkRepository = dependencies.bookmarkRepository,
            downloadRepository = dependencies.downloadRepository,
            downloadHistoryRepository = dependencies.downloadHistoryRepository,
            banRepository = dependencies.banRepository,
            settingsRepository = dependencies.settingsRepository,
            boardRepository = dependencies.boardRepository,
            historyRepository = dependencies.historyRepository,
            novelHistoryRepository = dependencies.novelHistoryRepository,
            muteRepository = dependencies.muteRepository,
            updateCheckClient = dependencies.updateCheckClient,
        )
    }
}

@Composable
private fun rememberRootComponent(dependencies: AppDependencies): RootComponent {
    // Decompose 需要显式生命周期管理；Compose 组合进入时 resume，销毁时 destroy。
    val lifecycle = remember { LifecycleRegistry() }
    val component = remember(dependencies) {
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle),
            settingsRepository = dependencies.settingsRepository,
        )
    }
    DisposableEffect(Unit) {
        lifecycle.resume()
        onDispose { lifecycle.destroy() }
    }
    return component
}
