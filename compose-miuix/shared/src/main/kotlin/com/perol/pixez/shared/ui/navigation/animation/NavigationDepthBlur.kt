package com.perol.pixez.shared.ui.navigation.animation

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import kotlin.math.abs
import kotlin.math.roundToInt

private const val NAV_BLUR_MAX_RADIUS_PX_DARK = 28f
private const val NAV_BLUR_MAX_RADIUS_PX_LIGHT = 22f
private const val NAV_BLUR_QUANTUM_PX = 4f
private const val NAV_BLUR_LIGHT_SEPARATION_TINT_ALPHA = 0.05f

/** 导航底层景深模糊单帧状态（固定半径 + 线性权重，避免每帧重建模糊管线）。 */
internal data class NavigationBlurBlendFrame(
    val radiusPx: Float,
    val alpha: Float,
    val separationTintAlpha: Float = 0f,
    val isLightBackground: Boolean = false,
) {
    companion object {
        val None = NavigationBlurBlendFrame(radiusPx = 0f, alpha = 0f)
    }
}

/** 按主题明暗返回转场最大景深模糊半径（像素）。 */
internal fun resolveNavigationBlurMaxRadiusPx(isLightBackground: Boolean): Float =
    if (isLightBackground) NAV_BLUR_MAX_RADIUS_PX_LIGHT else NAV_BLUR_MAX_RADIUS_PX_DARK

/**
 * 将覆盖/返回进度映射为景深模糊参数：
 * 半径按主题固定并对齐 4px 步长，仅渐变混合权重 [alpha]，避免手势期间重建 RenderEffect。
 */
internal fun resolveNavigationBlurBlendFrame(
    progress: Float,
    isLightBackground: Boolean = false,
    enabled: Boolean = true,
    sdkInt: Int = Build.VERSION.SDK_INT,
): NavigationBlurBlendFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val active = enabled && sdkInt >= Build.VERSION_CODES.S && clamped > 0.001f
    if (!active) return NavigationBlurBlendFrame.None
    val maxRadius = resolveNavigationBlurMaxRadiusPx(isLightBackground)
    val quantizedRadius = ((maxRadius / NAV_BLUR_QUANTUM_PX).roundToInt() * NAV_BLUR_QUANTUM_PX)
        .coerceAtLeast(NAV_BLUR_QUANTUM_PX)
    val separationTint = if (isLightBackground) {
        NAV_BLUR_LIGHT_SEPARATION_TINT_ALPHA * clamped
    } else {
        0f
    }
    return NavigationBlurBlendFrame(
        radiusPx = quantizedRadius,
        alpha = clamped,
        separationTintAlpha = separationTint,
        isLightBackground = isLightBackground,
    )
}

/** 缓存单份量化模糊 RenderEffect，半径不变时直接复用，零重复分配。 */
private class BlurRenderEffectCache {
    private var lastRadiusPx = -1f
    private var cached: RenderEffect? = null

    fun resolve(radiusPx: Float): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || radiusPx <= 0.5f) {
            lastRadiusPx = -1f
            cached = null
            return null
        }
        if (abs(radiusPx - lastRadiusPx) >= 0.5f || cached == null) {
            lastRadiusPx = radiusPx
            cached = AndroidRenderEffect
                .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
        return cached
    }
}

/**
 * 为被覆盖的底层页面叠加轻量实时景深模糊与亮色分离底色：
 * 内层录制内容图层引用（不冻结页面、不读回像素），稳态时直接直通绘制。
 */
internal fun Modifier.navigationDepthBlur(
    frameProvider: () -> NavigationBlurBlendFrame,
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    return drawWithCache {
        val source = obtainGraphicsLayer()
        val blurred = obtainGraphicsLayer()
        val effects = BlurRenderEffectCache()
        var effectRecorded = false
        onDrawWithContent {
            val frame = frameProvider()
            if (frame.alpha <= 0.001f || frame.radiusPx <= 0.5f) {
                if (effectRecorded) {
                    blurred.renderEffect = null
                    effectRecorded = false
                }
                drawContent()
            } else {
                source.record { this@onDrawWithContent.drawContent() }
                if (!effectRecorded) {
                    blurred.record { drawLayer(source) }
                    effectRecorded = true
                }
                blurred.renderEffect = effects.resolve(frame.radiusPx)
                blurred.alpha = frame.alpha
                if (frame.alpha < 0.999f) {
                    drawLayer(source)
                }
                drawLayer(blurred)
                if (frame.separationTintAlpha > 0.001f) {
                    val tintColor = if (frame.isLightBackground) Color.White else Color.Black
                    drawRect(tintColor.copy(alpha = frame.separationTintAlpha))
                }
            }
        }
    }.graphicsLayer()
}
