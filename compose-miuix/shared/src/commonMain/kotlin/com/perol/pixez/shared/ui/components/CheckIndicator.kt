package com.perol.pixez.shared.ui.components

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.Text

/**
 * 选中指示器：当前项显示对勾。
 *
 * 用于设置页中互斥单选列表的右侧指示，与 MIUIX 原生风格保持一致。
 */
@Composable
fun CheckIndicator(selected: Boolean) {
    if (selected) {
        Text(text = "✓")
    }
}
