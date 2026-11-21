package com.perol.pixez.shared.ui.coui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState

/**
 * 状态层：加载、空、错误、分页页脚。
 *
 * 文案统一走 [LocalStrings] 与 [PixivApiErrorFormatter]，避免把异常原文直透 UI；
 * 外观只用主题色与官方文字角色，保证与列表页同一套节奏。
 */

/** 把异常翻成当前语言的可读文案。 */
@Composable
internal fun CouiErrorMessage(error: Throwable?): String {
    val strings = LocalStrings.current
    return error?.let { PixivApiErrorFormatter.format(it, strings) } ?: strings.retry
}

/** 胶囊文字按钮：官方库没有通用按钮组件，这里用主题色 + 库内按压反馈平替。 */
@Composable
internal fun CouiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CouiCapsuleShape)
            .background(if (filled) scheme.accent else scheme.container)
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = source, enabled = enabled, list = false)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemValue,
            color = if (filled) scheme.onAccent else scheme.accent,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 首屏占位：加载、空、错误共用同一张卡片。
 *
 * 卡片自带最小高度与居中约束，直接放进正文区即可；页面无需再叠一层居中盒子。
 */
@Composable
internal fun CouiPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(CouiPageShape)
                .background(CouiTheme.colorScheme.container)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = message,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (actionText != null && onAction != null) {
                CouiTextButton(text = actionText, onClick = onAction)
            }
        }
    }
}

/** 首屏加载占位。 */
@Composable
internal fun CouiLoadingPlaceholder(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val strings = LocalStrings.current
    CouiPlaceholder(message = message ?: strings.loading, modifier = modifier)
}

/** 首屏错误占位，自带重试入口。 */
@Composable
internal fun CouiErrorPlaceholder(
    error: Throwable?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    CouiPlaceholder(
        message = CouiErrorMessage(error),
        modifier = modifier,
        actionText = strings.retry,
        onAction = onRetry,
    )
}

/** 分页页脚：与 [PagingFooterState] 三态对齐。 */
@Composable
internal fun CouiLoadMoreFooter(
    state: PagingFooterState,
    onRetry: () -> Unit,
    noMoreText: String? = null,
) {
    val strings = LocalStrings.current
    if (state.phase == PagingFooterPhase.Hidden) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state.phase) {
            PagingFooterPhase.Loading -> Text(
                text = strings.loadingMore,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PagingFooterPhase.Retry -> CouiTextButton(text = strings.retry, onClick = onRetry, filled = false)
            PagingFooterPhase.NoMore -> Text(
                text = noMoreText ?: strings.noMoreData,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PagingFooterPhase.Hidden -> Unit
        }
    }
}

/** 与官方 Group 卡片一致的圆角（24dp、连续曲率近似由圆角形状表达）。 */
private val CouiPageShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)