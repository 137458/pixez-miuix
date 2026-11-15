package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.radiobutton.RadioButton
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** 单选行：整行可点并由 Modifier.selectable 承担语义，右侧用 COUI 库 RadioButton 只作指示器。 */

/**
 * COUI 单选行：标题（+可选摘要）与行尾库单选指示器。
 *
 * 点击语义挂在整行上（[selectable] 自带 role = [Role.RadioButton]），行尾的库
 * [RadioButton] 只传 selected 作视觉指示：库控件没有 onClick 参数，正好避免出现
 * 第二个可点目标让读屏焦点与点击热区分散。
 *
 * 行尾指示器的语义被清除（[Box] + [clearAndSetSemantics]，只清语义、不影响绘制与测量），
 * 读屏只播报整行的单选状态与标题，不会因库控件自带选中语义而重复播报。
 */
@Composable
internal fun CouiRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .couiPressFeedback(pressInteraction, enabled = enabled)
            .selectable(
                selected = selected,
                interactionSource = pressInteraction,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Box(modifier = Modifier.clearAndSetSemantics { }) {
            RadioButton(selected = selected, enabled = enabled)
        }
    }
}
