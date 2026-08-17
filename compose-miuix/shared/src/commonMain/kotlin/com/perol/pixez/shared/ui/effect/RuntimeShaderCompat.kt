package com.perol.pixez.shared.ui.effect

import androidx.compose.ui.graphics.Brush

expect fun isRuntimeShaderSupported(): Boolean

expect class RuntimeShaderCompat(sksl: String) {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, values: FloatArray)
    val brush: Brush
}
