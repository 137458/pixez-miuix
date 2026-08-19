package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用加载/空态/错误占位组件，供各页面复用。
 */

@Composable
internal fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${strings.loading}…",
            style = MiuixTheme.textStyles.body1,
        )
    }
}

@Composable
internal fun EmptyPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

@Composable
internal fun ErrorPlaceholder(
    error: Throwable?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${strings.loadFailed}: ${error?.message ?: strings.loadFailed}",
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            TextButton(
                text = strings.retry,
                onClick = onRetry,
            )
        }
    }
}
