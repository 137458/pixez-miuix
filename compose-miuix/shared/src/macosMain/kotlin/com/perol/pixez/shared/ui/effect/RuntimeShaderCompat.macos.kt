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
            val e = effect ?: return SolidColor(Color.Transparent)
            val uniformsData = buildUniformsData()
            val shader = e.makeShader(uniformsData, null, null)
            return ShaderBrush(shader.asComposeShader())
        }

    private fun buildUniformsData(): Data? {
        if (floatUniforms.isEmpty()) return null
        val totalFloats = floatUniforms.values.sumOf { it.size }
        val bytes = ByteArray(totalFloats * 4)
        var offset = 0
        for (arr in floatUniforms.values) {
            for (f in arr) {
                val bits = f.toRawBits()
                bytes[offset++] = (bits and 0xFF).toByte()
                bytes[offset++] = ((bits shr 8) and 0xFF).toByte()
                bytes[offset++] = ((bits shr 16) and 0xFF).toByte()
                bytes[offset++] = ((bits shr 24) and 0xFF).toByte()
            }
        }
        return Data.makeFromBytes(bytes)
    }
}
