package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** COUI 可滚动筛选胶囊的数据项。 */
internal data class CouiFilterChip(
    val key: String,
    val label: String,
)

/**
 * COUI 筛选胶囊栏：横向滚动的胶囊行。
 *
 * 不使用库内 ChipTabRow：它的凝光材质要求 backdrop 来自「不透明的兄弟节点」，
 * 而筛选条位于页面正文源内部（自身即被采样的内容），采样会记录到自己，实机上表现为
 * 凝光失效或拖影。这里统一用不依赖采样的胶囊外观，几何与选中配色沿用官方取值。
 */
@Composable
internal fun CouiFilterChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips, key = { it.key }) { chip ->
            CouiChipSurface(
                text = chip.label,
                selected = chip.key == selectedKey,
                onClick = { onSelect(chip.key) },
            )
        }
    }
}

/**
 * COUI 辅助胶囊：可独立选中，用于互不排斥的快捷筛选（排序、AI 过滤、筛选入口）。
 * 与 [CouiFilterChipBar] 的区别是不做互斥选择，选中态只由调用方给定。
 */
@Composable
internal fun CouiAssistChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CouiChipSurface(text = text, selected = selected, onClick = onClick, modifier = modifier)
}

/**
 * 单一 chip 外观：筛选条与辅助胶囊共用一份胶囊实现，两种用法只在选择语义上不同。
 */
@Composable
private fun CouiChipSurface(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.accent else scheme.container)
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = if (selected) scheme.onAccent else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
