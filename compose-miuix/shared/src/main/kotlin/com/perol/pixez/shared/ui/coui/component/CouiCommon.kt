package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.theme.CouiTheme

/** COUI 共用交互基元：按压反馈 Modifier 是这里唯一剩下的共享工具，其余控件已按职责拆到同包各文件。 */

/**
 * COUI 自绘可点表面的统一按压反馈：配合 `clickable(indication = null)` 使用，
 * 去掉 Compose 默认涟漪，只保留库的 hover/focus/press 蒙层。
 * [list] 保持默认的列表行口径：整卡/整行不做顶栏按钮那档强反馈。
 */
@Composable
internal fun Modifier.couiPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    list: Boolean = true,
): Modifier = couiButtonFeedback(
    dark = CouiTheme.isDark,
    interactionSource = interactionSource,
    enabled = enabled,
    list = list,
)
