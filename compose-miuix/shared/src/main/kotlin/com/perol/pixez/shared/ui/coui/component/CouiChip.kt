package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** COUI 分类条（[CouiChipTabRow]）的数据项。 */
internal data class CouiFilterChip(
    val key: String,
    val label: String,
)

/**
 * COUI 辅助胶囊：可独立选中，用于作品标签、互不排斥的快捷筛选（排序、AI 过滤、筛选入口）与面包屑。
 * 互斥单选的页签与分类栏统一使用 [CouiSegmentedTabs] / [CouiTopBarSegmentedTabs] / [CouiChipTabRow]。
 */
@Composable
internal fun CouiAssistChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(CouiCapsuleShape)
            .background(if (selected) scheme.accent else scheme.container)
            .couiPressFeedback(pressInteraction, list = false)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                role = Role.Button,
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
