package com.perol.pixez.shared.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import io.github.aakira.napier.Napier

actual fun performHapticFeedback(type: HapticType) {
    val context = BrowserLauncherContext.applicationContext ?: return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
        }
    } catch (e: Throwable) {
        Napier.w("Haptic vibration failed", e)
    }
}
