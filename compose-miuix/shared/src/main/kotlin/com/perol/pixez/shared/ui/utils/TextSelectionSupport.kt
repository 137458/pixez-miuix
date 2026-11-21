package com.perol.pixez.shared.ui.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * 在 TextFieldValue 的当前选区位置插入文本。
 * 若存在选区则替换选区内容，否则在光标处插入。
 * 插入完成后将光标置于插入文本末尾。
 *
 * 落位说明：分享文案与下载设置两处页面共用本纯函数，且 COUI 页面同样调用，
 * 属皮肤无关的共享工具，因此放在 ui/utils 中立位置，不再由具体页面文件充当宿主
 * （否则 COUI 页面需要 import CopyTextSettingScreen.kt 才能复用）。
 * 签名与可见性保持迁移前不变。
 */
internal fun insertTextAtSelection(
    textFieldValue: TextFieldValue,
    insertText: String,
): TextFieldValue {
    val selection = textFieldValue.selection
    val newText = textFieldValue.text.replaceRange(
        selection.start,
        selection.end,
        insertText,
    )
    val newCursor = selection.start + insertText.length
    return textFieldValue.copy(
        text = newText,
        selection = TextRange(newCursor, newCursor),
    )
}
