package com.perol.pixez.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.navigation.RootContent
import com.perol.pixez.shared.ui.navigation.toRepositoryRegistry

/**
 * 共享的 Compose 应用入口：装配 Coil 图片加载器、Decompose 根组件与仓储注入通道。
 */
@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@Composable
fun App(
    dependencies: AppDependencies,
    rootComponent: RootComponent? = null,
    systemTransitionSessionToken: String? = null,
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = { dependencies.httpClient.downloadClient },
                        concurrentRequestStrategy = { coil3.network.DeDupeConcurrentRequestStrategy() },
                    )
                )
                com.perol.pixez.shared.platform.getPlatformGifDecoderFactories().forEach { add(it) }
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
                    .maxSizeBytes(1024L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    val component = rootComponent ?: rememberRootComponent(dependencies)
    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
    val registry = remember(dependencies) { dependencies.toRepositoryRegistry() }
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
    ) {
        RootContent(
            component = component,
            registry = registry,
            systemTransitionSessionToken = systemTransitionSessionToken,
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