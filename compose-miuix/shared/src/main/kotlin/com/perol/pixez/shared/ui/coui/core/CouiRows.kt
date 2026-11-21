package com.perol.pixez.shared.ui.coui.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageColors
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Item
import com.itos.coui.radiobutton.RadioButton
import com.itos.coui.switch.Switch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/**
 * 列表行层：全部基于官方的 Group(卡片) + Item(行) + Divider(分隔线)。
 *
 * 交互口径按官方文档：Item 只负责版式，点击/开关语义由宿主的 toggleable / selectable
 * 提供，两者共用同一个 InteractionSource，按压反馈走 couiButtonFeedback 的 list 档。
 * 因此这里的行都是"语义挂在行上、控件只做绘制"，避免行与滑块各自响应造成双重触发。
 */

/** 设置分组卡片：官方 Group，标题与页脚由官方版式渲染。 */
@Composable
internal fun CouiSettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Group(modifier = modifier, title = title, footer = footer, content = content)
}

/** 行间分隔线：组内相邻两行之间调用，首行不加。 */
@Composable
internal fun CouiRowDivider(
    modifier: Modifier = Modifier,
    startIndent: androidx.compose.ui.unit.Dp = CouiPageDefaults.rowPadding,
) {
    Divider(modifier = modifier, startIndent = startIndent, endIndent = CouiPageDefaults.rowPadding)
}

/** 行标题文字。 */
@Composable
internal fun CouiItemTitle(
    text: String,
    enabled: Boolean = true,
    colors: CouiPageColors = CouiPageDefaults.themeColors(),
) {
    Text(
        text = text,
        kind = CouiTextKind.ItemTitle,
        color = if (enabled) colors.title else colors.disabled,
        textAlign = TextAlign.Start,
    )
}

/** 行摘要文字。 */
@Composable
internal fun CouiItemSummary(
    text: String,
    enabled: Boolean = true,
    colors: CouiPageColors = CouiPageDefaults.themeColors(),
) {
    Text(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = if (enabled) colors.summary else colors.disabled,
        textAlign = TextAlign.Start,
    )
}

/** 尾部箭头：跟随主题描边色自绘，不引入图标依赖。 */
@Composable
internal fun CouiChevron(
    modifier: Modifier = Modifier,
    color: Color = CouiTheme.colorScheme.onSurfaceVariant,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 1.6.dp.toPx()
        val tailX = if (isRtl) size.width * 0.64f else size.width * 0.36f
        val tipX = if (isRtl) size.width * 0.34f else size.width * 0.66f
        drawLine(
            color = color,
            start = Offset(tailX, size.height * 0.28f),
            end = Offset(tipX, size.height * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(tipX, size.height * 0.5f),
            end = Offset(tailX, size.height * 0.72f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** 开关行：整行可点，滑块只绘制。 */
@Composable
internal fun CouiToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val colors = CouiPageDefaults.themeColors()
    val source = remember { MutableInteractionSource() }
    val summaryContent: (@Composable () -> Unit)? = summary?.let { text ->
        { CouiItemSummary(text = text, enabled = enabled, colors = colors) }
    }
    Item(
        modifier = modifier
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = source, enabled = enabled, list = true),
        enabled = enabled,
        title = { CouiItemTitle(text = title, enabled = enabled, colors = colors) },
        summary = summaryContent,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                interactionSource = source,
            )
        },
    )
}

/** 值行：右侧展示当前值，可点时补箭头。 */
@Composable
internal fun CouiValueRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = CouiPageDefaults.themeColors()
    val summaryContent: (@Composable () -> Unit)? = summary?.let { text ->
        { CouiItemSummary(text = text, enabled = enabled, colors = colors) }
    }
    Item(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        title = { CouiItemTitle(text = title, enabled = enabled, colors = colors) },
        summary = summaryContent,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        kind = CouiTextKind.ItemValue,
                        color = if (enabled) colors.summary else colors.disabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                }
                if (onClick != null) CouiChevron()
            }
        },
    )
}

/** 单选项行：整行可选中，圆形按钮只绘制。 */
@Composable
internal fun CouiRadioOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val colors = CouiPageDefaults.themeColors()
    val source = remember { MutableInteractionSource() }
    val summaryContent: (@Composable () -> Unit)? = summary?.let { text ->
        { CouiItemSummary(text = text, enabled = enabled, colors = colors) }
    }
    Item(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = source, enabled = enabled, list = true),
        enabled = enabled,
        title = { CouiItemTitle(text = title, enabled = enabled, colors = colors) },
        summary = summaryContent,
        trailing = { RadioButton(selected = selected, enabled = enabled) },
    )
}

/** 纯动作行：无尾部控件，标题即动作。 */
@Composable
internal fun CouiActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val colors = CouiPageDefaults.themeColors()
    val summaryContent: (@Composable () -> Unit)? = summary?.let { text ->
        { CouiItemSummary(text = text, enabled = enabled, colors = colors) }
    }
    Item(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        title = {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = when {
                    !enabled -> colors.disabled
                    destructive -> CouiDialogDefaults.colors(CouiTheme.isDark).destructive
                    else -> colors.title
                },
                textAlign = TextAlign.Start,
            )
        },
        summary = summaryContent,
    )
}

/** 只读信息行：左标题右值，不可点。 */
@Composable
internal fun CouiInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    CouiValueRow(title = title, modifier = modifier, value = value)
}

/** 开关行的整卡外层：单行分组不加分隔线时的薄封装。 */
@Composable
internal fun CouiSingleGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Group(modifier = modifier.fillMaxWidth(), content = content)
}