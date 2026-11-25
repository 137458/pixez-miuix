package com.perol.pixez.android

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.navigation.animation.system.SystemIllustCardTransition
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 为宿主 Activity 绑定系统级原生窗口卡片转场拦截器；不可用或捕获失败时自动回退 CardExpandLayer。 */
internal fun attachIllustWindowInterceptor(
    activity: ComponentActivity,
    rootComponent: RootComponent,
) {
    var latestRequestId = 0L
    var stackInitialized = false
    val stackSubscription = rootComponent.stack.subscribe {
        if (stackInitialized) {
            // 栈发生切页/出栈时使在途异步 tryOpen 失效，避免慢速回退 fallback() 劫持后续页面。
            latestRequestId++
        } else {
            stackInitialized = true
        }
    }
    activity.lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                stackSubscription.cancel()
            }
        },
    )
    rootComponent.cardWindowInterceptor = { config, fallback ->
        if (!SystemIllustCardTransition.canAttempt(activity, config)) {
            latestRequestId++
            false
        } else {
            val requestId = ++latestRequestId
            activity.lifecycleScope.launch {
                val launched = SystemIllustCardTransition.tryOpen(
                    activity = activity,
                    config = config,
                    buildIntent = { IllustDetailActivity.createIntent(activity, config) },
                    isCurrent = { requestId == latestRequestId },
                )
                if (!launched && requestId == latestRequestId) {
                    fallback()
                }
            }
            true
        }
    }
}

internal fun applyWindowDisplayMode(
    activity: ComponentActivity,
    settingsRepository: SettingsRepository,
) {
    val candidates = activity.display?.supportedModes.orEmpty().map { mode ->
        DisplayModeCandidate(
            modeId = mode.modeId,
            refreshRate = mode.refreshRate,
            physicalWidth = mode.physicalWidth,
            physicalHeight = mode.physicalHeight,
        )
    }
    val selection = DisplayModePolicy.select(
        displayMode = settingsRepository.displayMode,
        modes = candidates,
    )
    val layoutParams = activity.window.attributes
    layoutParams.preferredDisplayModeId = selection.modeId
    layoutParams.preferredRefreshRate = selection.preferredRefreshRate
    activity.window.attributes = layoutParams
}

internal fun observeWindowDisplayModeChanges(
    activity: ComponentActivity,
    settingsRepository: SettingsRepository,
    displayModeSelector: (SettingsRepository) -> Int = { it.displayMode },
    onApply: () -> Unit = { applyWindowDisplayMode(activity, settingsRepository) },
) {
    activity.lifecycleScope.launch {
        settingsRepository.changeVersion
            .map { displayModeSelector(settingsRepository) }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                onApply()
            }
    }
}
