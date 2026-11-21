package com.perol.pixez.shared.platform

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.aakira.napier.Napier

/**
 * 触觉反馈类型定义。
 */
enum class HapticType {
    Confirm,
    Reject,
    GestureStart,
    GestureEnd,
    Tick,
}

/**
 * 触发系统物理级触觉反馈（触感振动）。
 */
fun performHapticFeedback(type: HapticType) {
    val context = BrowserLauncherContext.applicationContext ?: return
    try {
        // minSdk 31 起 Context.VIBRATOR_SERVICE 已废弃，改走 VibratorManager 取默认振动器。
        val vibrator: Vibrator? = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            val effect = when (type) {
                HapticType.Confirm -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticType.Reject -> VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1)
                HapticType.GestureStart -> VibrationEffect.createOneShot(10, 80)
                HapticType.GestureEnd -> VibrationEffect.createOneShot(12, 120)
                HapticType.Tick -> VibrationEffect.createOneShot(8, 70)
            }
            vibrator.vibrate(effect)
        }
    } catch (e: Throwable) {
        Napier.w("Haptic vibration failed", e)
    }
}
