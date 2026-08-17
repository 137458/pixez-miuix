package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.LocalSettingsRepository

/**
 * 插画瀑布流网格。
 *
 * 支持通过设置项进行自适应宽度计算或固定列数配置。
 */
@Composable
fun IllustStaggeredGrid(
    illusts: List<Illust>,
    onIllustClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: StaggeredGridCells? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = 8.dp,
        top = 8.dp,
        end = 8.dp,
        bottom = 100.dp,
    ),
) {
    val settings = LocalSettingsRepository.current
    val effectiveColumns = remember(columns, settings?.crossAdapt, settings?.crossAdapterWidth, settings?.crossCount, settings?.changeVersion) {
        if (columns != null) {
            columns
        } else if (settings?.crossAdapt == true) {
            val minWidth = settings.crossAdapterWidth.coerceIn(100, 500)
            StaggeredGridCells.Adaptive(minWidth.dp)
        } else {
            val cols = settings?.crossCount?.coerceIn(1, 6) ?: 2
            StaggeredGridCells.Fixed(cols)
        }
    }

    LazyVerticalStaggeredGrid(
        columns = effectiveColumns,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(
            items = illusts,
            key = { it.id },
        ) { illust ->
            IllustCard(
                illust = illust,
                onClick = { onIllustClick(illust.id) },
            )
        }
    }
}
