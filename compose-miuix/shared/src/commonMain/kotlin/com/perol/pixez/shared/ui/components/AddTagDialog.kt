package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 添加标签弹窗：单个输入框 + 取消/添加按钮，供收藏标签与屏蔽标签页面复用。
 *
 * @param show 是否展示。
 * @param title 标题。
 * @param summary 摘要。
 * @param isLoading 提交中：按钮显示「添加中」并禁用。
 * @param onDismiss 关闭回调。
 * @param onConfirm 确认回调，入参为去掉首尾空白后的标签名。
 */
@Composable
fun AddTagDialog(
    show: Boolean,
    title: String,
    summary: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var name by remember(show) { mutableStateOf("") }

    OverlayDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = strings.tags,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        TextButton(
            text = strings.cancel,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            text = if (isLoading) strings.btnAdding else strings.btnAdd,
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    onConfirm(trimmed)
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
