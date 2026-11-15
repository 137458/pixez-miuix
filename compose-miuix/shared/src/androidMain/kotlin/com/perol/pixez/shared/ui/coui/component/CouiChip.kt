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
import android.os.Build
import androidx.annotation.RequiresApi
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** 标签 chip：筛选条与合并后的单一 chip 外观，并统一 ChipTabRow 的系统门槛常量，避免散落裸数字。 */

/** COUI 可滚动筛选胶囊的数据项。 */
internal data class CouiFilterChip(
    val key: String,
    val label: String,
)

/** COUI 库 ChipTabRow 的系统门槛。 */
/**
 * COUI 筛选胶囊栏：API 34+ 使用原厂 ChipTabRow（滚动胶囊 + 凝光材质），
 * 低版本按能力边界清单退回同版式自绘胶囊栏。
 */
@Composable
internal fun CouiFilterChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= COUI_CHIP_TAB_MIN_SDK) {
        CouiLibraryChipBar(chips, selectedKey, onSelect, modifier)
    } else {
        CouiPlainChipBar(chips, selectedKey, onSelect, modifier)
    }
}

@RequiresApi(COUI_CHIP_TAB_MIN_SDK)
@Composable
private fun CouiLibraryChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val items = remember(chips) {
        chips.map { chip -> CouiChipTabItem(key = chip.key, label = chip.label) }
    }
    ChipTabRow(
        modifier = modifier.fillMaxWidth(),
        items = items,
        selectedKey = selectedKey,
        onSelectedKeyChange = { key -> (key as? String)?.let(onSelect) },
        backdrop = rememberCouiBackdrop(),
    )
}

/** 低版本平替：与 ChipTabRow 同版式的横向滚动胶囊栏。 */
@Composable
private fun CouiPlainChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
 * 单一 chip 外观：筛选条与辅助胶囊原先各写一份胶囊，圆角、内边距与选中配色一旦调整就会漂移，
 * 这里合并成一处实现，两种用法只在选择语义上不同。
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
