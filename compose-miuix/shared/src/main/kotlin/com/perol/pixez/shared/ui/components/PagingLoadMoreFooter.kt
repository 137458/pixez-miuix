package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 分页 footer 外观变体（架构审查 UI-3）：只区分「加载中是否带文案」，
 * 失败文案、重试控件与「没有更多」文案在所有变体间统一。
 */
internal enum class PagingFooterAppearance {
    /** 完整：指示器 + 「正在加载更多」文案（搜索结果、瀑布流、特辑网格）。 */
    Complete,

    /** 精简：仅指示器（用户列表、评论列表原样式）。 */
    Compact,
}

/**
 * 分页「加载更多」footer 三态：加载中 / 出错重试 / 没有更多。
 *
 * 架构审查 UI-3 收敛的唯一 footer：原先 8 处同构内联（瀑布流 54 行 + 六屏各 29 行 + 本组件）
 * 统一到此。调用方只提供状态快照 [state] 与重试入口——状态机直接给出
 * [com.perol.pixez.shared.ui.state.PagedListState.footerState]，渲染态由
 * [PagingFooterState.phase] 单点判定，[PagingFooterPhase.Hidden] 时不发射任何布局节点。
 *
 * 在 LazyColumn / 瀑布流中的挂载时机与 item key 由调用方决定
 * （按 phase != Hidden 决定是否添加 item，避免留下空白占位）。
 *
 * @param noMoreText 「没有更多」文案覆盖（如评论列表用 commentsNoMore），默认 noMoreData。
 */
@Composable
internal fun PagingLoadMoreFooter(
    state: PagingFooterState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: PagingFooterAppearance = PagingFooterAppearance.Complete,
    noMoreText: String? = null,
) {
    val phase = state.phase
    if (phase == PagingFooterPhase.Hidden) return
    val strings = LocalStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            PagingFooterPhase.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfiniteProgressIndicator(modifier = Modifier.size(20.dp))
                    if (appearance == PagingFooterAppearance.Complete) {
                        Text(
                            text = strings.loadingMore,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            PagingFooterPhase.Retry -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = strings.loadMoreFailedRetry,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.error,
                    )
                    TextButton(
                        text = strings.retry,
                        onClick = onRetry,
                    )
                }
            }
            else -> {
                Text(
                    text = noMoreText ?: strings.noMoreData,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
