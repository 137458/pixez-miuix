package com.perol.pixez.shared.platform

import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberScreenCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view, density) {
        // minSdk 31 恒不低于 S（31）：getRoundedCorner 恒可用，低版本分支已删除。
        val insets = view.rootWindowInsets
        val topLeft = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        val topRight = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
        val bottomLeft = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        val bottomRight = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
        var maxRadiusPx = maxOf(topLeft, topRight, bottomLeft, bottomRight)
        if (maxRadiusPx <= 0) {
            val res = view.context.resources
            val resId = res.getIdentifier("rounded_corner_radius", "dimen", "android")
            if (resId > 0) {
                maxRadiusPx = runCatching { res.getDimensionPixelSize(resId) }.getOrDefault(0)
            }
        }
        if (maxRadiusPx > 0) {
            with(density) { maxRadiusPx.toDp() }
        } else {
            0.dp
        }
    }
}
