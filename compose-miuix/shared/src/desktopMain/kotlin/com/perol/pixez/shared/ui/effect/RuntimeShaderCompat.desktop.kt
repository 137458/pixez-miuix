package com.perol.pixez.shared.ui.effect

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeShader
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect

actual fun isRuntimeShaderSupported(): Boolean = true

actual class RuntimeShaderCompat actual constructor(sksl: String) {
    private val effect: RuntimeEffect? = try {
        RuntimeEffect.makeForShader(sksl)
    } catch (_: Throwable) {
        null
    }

    private val floatUniforms = mutableMapOf<String, FloatArray>()

    actual fun setFloatUniform(name: String, value: Float) {
        floatUniforms[name] = floatArrayOf(value)
    }

    actual fun setFloatUniform(name: String, values: FloatArray) {
        floatUniforms[name] = values.copyOf()
    }

    actual val brush: Brush
        get() {
            val eff = effect ?: return SolidColor(Color.Transparent)
            return try {
                val totalFloats = floatUniforms.values.sumOf { it.size }
                val buffer = java.nio.ByteBuffer.allocate(totalFloats * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                for (arr in floatUniforms.values) {
                    for (f in arr) buffer.putFloat(f)
                }
                val data = Data.makeFromBytes(buffer.array())
                val skiaShader = eff.makeShader(data, null, null)
                ShaderBrush(skiaShader.asComposeShader())
            } catch (_: Throwable) {
                SolidColor(Color.Transparent)
            }
        }
}
