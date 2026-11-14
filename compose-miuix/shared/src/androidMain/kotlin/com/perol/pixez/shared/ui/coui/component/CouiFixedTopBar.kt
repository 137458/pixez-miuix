package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * COUI 二级页固定顶栏：返回按钮 + 单行标题 + 尾部动作槽。
 *
 * 统一各页的 56dp 高度、状态栏避让、横向 12dp 内边距与动作间距，
 * 返回为空时保留 40dp 占位，避免标题左右跳动。
 */
@Composable
internal fun CouiFixedTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionSpacing: Dp = 6.dp,
    overlayBack: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val scheme = CouiTheme.colorScheme
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(actionSpacing),
    ) {
        when {
            onBack == null -> Spacer(modifier = Modifier.width(40.dp))
            overlayBack -> CouiOverlayActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            else -> CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
        }
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}
