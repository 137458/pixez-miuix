package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** 下拉选择行：COUI 库无下拉原语，用菜单锚点把当前值放在行尾，点行即开菜单。 */

/**
 * COUI 下拉选择行：标题 + 当前值，点击贴着本行弹出官方菜单。
 *
 * COUI 库无下拉选择原语，用 [Menu] 锚点实现，选中项由调用方按索引回写。
 */
@Composable
internal fun CouiSelectRow(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val anchor = rememberCouiMenuAnchor()
    var expanded by remember { mutableStateOf(false) }
    val pressInteraction = remember { MutableInteractionSource() }
    Box(modifier = modifier.couiMenuAnchor(anchor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .couiPressFeedback(pressInteraction)
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null,
                    onClick = { anchor.open { expanded = true } },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = items.getOrNull(selectedIndex).orEmpty(),
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Menu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchor = anchor,
            headerText = title,
            items = items.mapIndexed { index, label ->
                CouiMenuItem(value = index.toString(), text = label)
            },
            onItemClick = { item ->
                expanded = false
                item.value.toIntOrNull()?.let(onSelect)
            },
        )
    }
}
