package com.perol.pixez.shared.ui.effect

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor

actual fun isRuntimeShaderSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

actual class RuntimeShaderCompat actual constructor(sksl: String) {
    private val shader: RuntimeShader? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            RuntimeShader(sksl)
        } catch (_: Throwable) {
            null
        }
    } else {
        null
    }

    actual fun setFloatUniform(name: String, value: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shader?.setFloatUniform(name, value)
        }
    }

    actual fun setFloatUniform(name: String, values: FloatArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shader?.setFloatUniform(name, values)
        }
    }

    actual val brush: Brush
        get() = if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ShaderBrush(shader)
        } else {
            SolidColor(Color.Transparent)
        }
}
