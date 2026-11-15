package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.i18n.LocalStrings

/** 错误与空态：把 Throwable 翻成当前语言文案，并给出统一的空态占位外观，避免各页面自造错误排版。 */

/**
 * COUI 占位区的错误文案：统一走 [PixivApiErrorFormatter] 本地化，
 * 避免 `Throwable.message` 这类原始异常文本直透 UI（与 MIUIX 占位组件同口径）。
 */
@Composable
internal fun couiErrorMessage(error: Throwable?): String {
    val strings = LocalStrings.current
    return error?.let { PixivApiErrorFormatter.format(it, strings) } ?: strings.retry
}

/** COUI 首屏加载/错误/空态：单卡片提示，保留重试入口。 */
@Composable
internal fun CouiStatePlaceholder(
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CouiTheme.colorScheme.container)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = message,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
            )
            if (actionText != null && onAction != null) {
                CouiTextActionButton(text = actionText, onClick = onAction)
            }
        }
    }
}
