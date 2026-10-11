package com.perol.pixez.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.perol.pixez.shared.data.settings.SettingsRepository

/**
 * [SettingsRepository] 的组合注入通道（中-3）：Local 定义收敛 ui 包，data 层不感知 Compose。
 * 由 RootContent 在应用根部 provide；消费端全在 UI。
 *
 * 中-9 单通道裁决：无默认值，缺失时读取即抛（快速失败），装配缺失首帧即暴露；
 * 非组合环境走构造注入，禁止回退 AppDependencies 全局单例（中-20）。
 */
val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository> {
    error("LocalSettingsRepository not provided")
}
