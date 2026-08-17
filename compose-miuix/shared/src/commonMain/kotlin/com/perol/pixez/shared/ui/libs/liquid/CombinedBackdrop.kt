// SPDX-License-Identifier: Apache-2.0
package com.perol.pixez.shared.ui.libs.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop

/**
 * 官方 Example 实现的 CombinedBackdrop：
 * 依次绘制 [first]（页面背景）和 [second]（激活项层 tabsBackdrop），
 * 供选择指示器在单个 Backdrop 采样中同时获取底层背景和激活图标。
 */
@Stable
class CombinedBackdrop(
    val first: Backdrop,
    val second: Backdrop,
) : Backdrop {

    override val isCoordinatesDependent: Boolean =
        first.isCoordinatesDependent || second.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock) }
        with(second) { drawBackdrop(density, coordinates, layerBlock) }
    }
}

@Composable
fun rememberCombinedBackdrop(
    first: Backdrop,
    second: Backdrop,
): Backdrop =
    remember(first, second) { CombinedBackdrop(first, second) }
