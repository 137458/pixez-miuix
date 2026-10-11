package com.perol.pixez.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.perol.pixez.shared.data.settings.SettingsRepository

/**
 * [SettingsRepository] 的组合注入通道（中-3）：Local 定义收敛 ui 包，data 层不感知 Compose。
 * 由 RootContent 在应用根部 provide；消费端全在 UI，缺失时为 null。
 */
val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository?> { null }
