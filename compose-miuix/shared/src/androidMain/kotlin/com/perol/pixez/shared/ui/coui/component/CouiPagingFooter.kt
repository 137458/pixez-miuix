package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState

/** 分页页脚：把 PagingFooterState 的加载/重试/无更多三态映射成同一外观，列表页无需各自判断。 */

/** COUI 翻页 footer：与 [PagingFooterState] 三态对齐的纯文本实现。 */
@Composable
internal fun CouiPagingFooter(
    state: PagingFooterState,
    onRetry: () -> Unit,
    noMoreText: String? = null,
) {
    val strings = LocalStrings.current
    val phase = state.phase
    if (phase == PagingFooterPhase.Hidden) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            PagingFooterPhase.Loading -> Text(
                text = strings.loadingMore,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            PagingFooterPhase.Retry -> CouiTextActionButton(
                text = strings.retry,
                onClick = onRetry,
            )
            PagingFooterPhase.NoMore -> Text(
                text = noMoreText ?: strings.noMoreData,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            PagingFooterPhase.Hidden -> Unit
        }
    }
}
