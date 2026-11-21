package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 剪贴板 OAuth 登录二次确认弹窗：
 * 防止剪贴板中残留或被其它应用写入的 Pixiv OAuth 回调链接在用户无感知情况下静默兑换登录。
 */
@Composable
fun ClipboardOAuthDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    WindowDialog(
        title = strings.dialogClipboardOAuthTitle,
        summary = strings.dialogClipboardOAuthSummary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        TextButton(
            text = strings.cancel,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = strings.btnContinueLogin,
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
