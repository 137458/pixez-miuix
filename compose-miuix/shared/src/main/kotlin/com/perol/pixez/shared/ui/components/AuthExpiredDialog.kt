package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 登录凭证失效提示：账号行仍在但 token 已被清理/吊销时，由 UI 根层统一弹出。
 *
 * 「重新登录」跳转账号管理页；「稍后」仅关闭本次提示，后续请求再次失败会重新提示。
 */
@Composable
fun AuthExpiredDialog(
    show: Boolean,
    onRelogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    // 该弹窗挂在 UI 根层，是各页面 Scaffold 的兄弟节点；Overlay* 依赖 Scaffold 内的
    // MiuixPopupHost 渲染，此处没有宿主，必须改用窗口级弹窗。
    WindowDialog(
        title = strings.dialogAuthExpiredTitle,
        summary = strings.dialogAuthExpiredSummary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        TextButton(
            text = strings.btnLater,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = strings.btnRelogin,
            onClick = onRelogin,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
