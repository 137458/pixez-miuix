package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perol.pixez.shared.ui.AppInfo
import com.perol.pixez.shared.ui.screens.ReleaseInfo
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 官方 Miuix / HyperOS 规范版本更新弹窗。
 */
@Composable
fun UpdateDialog(
    show: Boolean,
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    onUpdate: (url: String) -> Unit,
    onIgnore: ((version: String) -> Unit)? = null,
) {
    val strings = LocalStrings.current

    WindowDialog(
        show = show,
        title = strings.dialogNewVersionFound,
        summary = "v${AppInfo.VERSION_NAME} → v${releaseInfo.versionName}",
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 更新日志卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = releaseInfo.changelog.ifBlank { strings.updateChangelogTitle },
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (onIgnore != null) {
                    TextButton(
                        text = strings.btnIgnore,
                        onClick = { onIgnore(releaseInfo.versionName) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                }

                TextButton(
                    text = strings.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))

                TextButton(
                    text = strings.btnUpdate,
                    onClick = { onUpdate(releaseInfo.releaseUrl) },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
