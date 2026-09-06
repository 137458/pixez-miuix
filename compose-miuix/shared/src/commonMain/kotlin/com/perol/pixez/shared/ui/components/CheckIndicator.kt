package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 选中指示器：当前项显示对勾。
 *
 * 用于设置页中互斥单选列表的右侧指示，与 MIUIX 原生风格保持一致。
 */
@Composable
fun CheckIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (selected) {
        Icon(
            imageVector = MiuixIcons.Ok,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.primary,
            modifier = modifier
                .size(20.dp)
                .semantics {
                    role = Role.Checkbox
                },
        )
    }
}

