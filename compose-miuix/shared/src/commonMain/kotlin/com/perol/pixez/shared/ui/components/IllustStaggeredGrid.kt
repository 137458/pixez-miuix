package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust

/**
 * 插画瀑布流网格。
 *
 * 竖屏默认 2 列，Desktop/横屏通过外层 Modifier 或调整 cells 参数扩展列数。
 */
@Composable
fun IllustStaggeredGrid(
    illusts: List<Illust>,
    onIllustClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: StaggeredGridCells = StaggeredGridCells.Fixed(2),
    contentPadding: PaddingValues = PaddingValues(8.dp),
) {
    LazyVerticalStaggeredGrid(
        columns = columns,
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
