package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 选中指示器：当前项显示对勾。
 *
 * 用于设置页中互斥单选或复选列表的右侧指示，与 MIUIX 原生风格保持一致。
 * 保持固定尺寸并保留完整无障碍语义树节点（包括未选中状态），确保无障碍服务正确播报。
 *
 * @param selected 当前项是否选中
 * @param modifier 外部修饰符
 * @param contentDescription 无障碍辅助文本说明
 * @param role 无障碍角色，单选列表默认为 [Role.RadioButton]，也可指定为 [Role.Checkbox]
 */
@Composable
fun CheckIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    role: Role = Role.RadioButton,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .semantics {
                this.role = role
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = MiuixIcons.Ok,
                contentDescription = contentDescription,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

