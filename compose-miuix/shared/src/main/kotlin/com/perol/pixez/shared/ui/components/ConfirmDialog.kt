package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 通用二次确认弹窗，供收藏标签、屏蔽、历史、下载历史、追更、账号管理等页面复用。
 *
 * @param show 是否展示。
 * @param title 标题。
 * @param summary 摘要。
 * @param confirmText 确认按钮文案。
 * @param onConfirm 确认回调。
 * @param onDismiss 关闭回调。
 * @param cancelText 取消按钮文案，默认「取消」。
 * @param inProgress 处理中：禁用确认按钮。
 * @param dismissibleWhileInProgress 处理中是否允许关闭，默认不允许，避免重复提交。
 * @param confirmFirst 确认按钮是否排在取消按钮之前（历史页既有交互）。
 */
@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelText: String? = null,
    inProgress: Boolean = false,
    dismissibleWhileInProgress: Boolean = false,
    confirmFirst: Boolean = false,
) {
    val strings = LocalStrings.current
    val dismiss: () -> Unit = {
        if (!inProgress || dismissibleWhileInProgress) onDismiss()
    }

    OverlayDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = dismiss,
    ) {
        val confirmButton: @Composable () -> Unit = {
            TextButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = !inProgress,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val cancelButton: @Composable () -> Unit = {
            TextButton(
                text = cancelText ?: strings.cancel,
                onClick = dismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (confirmFirst) {
            confirmButton()
            cancelButton()
        } else {
            cancelButton()
            confirmButton()
        }
    }
}
