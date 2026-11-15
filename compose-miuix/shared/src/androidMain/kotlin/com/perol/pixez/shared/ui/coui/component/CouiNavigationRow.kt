package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/** 导航行：标题加右侧摘要的整行可点条目，作为页面内的二级入口。 */

/**
 * COUI 导航行：标题 + 摘要，整行可点击。
 *
 * 与开关行、选择行配合构成设置页的三类基础行；摘要为空时只渲染标题，不留空白槽。
 * [verticalPadding] 供行距不同的容器复用：设置页卡片为 12dp，详情页信息卡为 14dp。
 */
@Composable
internal fun CouiNavigationRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 12.dp,
) {
    val scheme = CouiTheme.colorScheme
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (summary.isNotBlank()) {
            Text(
                text = summary,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}
