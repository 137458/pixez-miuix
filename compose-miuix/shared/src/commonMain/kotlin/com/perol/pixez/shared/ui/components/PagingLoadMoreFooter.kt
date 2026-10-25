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
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 分页「加载更多」footer 三态：加载中 / 出错重试 / 没有更多。
 * 由搜索画师与小说结果列表的同构实现收敛而来，间距、字号与文案键原样搬运，观感不变。
 *
 * 仅在 [isLoadingMore]、[loadMoreError] 非空或 [hasMore] 为 false 时渲染内容，
 * 其余状态不发射任何布局节点；在 LazyColumn 中的挂载时机与 item key 由调用方决定。
 */
@Composable
internal fun PagingLoadMoreFooter(
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: Throwable?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isLoadingMore && loadMoreError == null && hasMore) return
    val strings = LocalStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoadingMore -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfiniteProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        text = strings.loadingMore,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            loadMoreError != null -> {
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
                    text = strings.noMoreData,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
