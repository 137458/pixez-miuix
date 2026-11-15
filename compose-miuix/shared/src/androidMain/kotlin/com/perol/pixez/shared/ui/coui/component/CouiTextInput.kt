package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle

/**
 * COUI 文本输入原语：圆角容器 + 主题令牌 + 空值占位。
 *
 * COUI 皮肤库只提供展示控件，没有文本输入控件，各页此前各自克隆一份
 * BasicTextField + 容器 + 占位的写法，圆角、内边距、占位色逐渐漂移。
 * 这里把容器与占位收敛到一处，调用方只需要决定尺寸与令牌：
 * - shape / containerColor / contentPadding / minHeight 由调用方传入，
 *   因为搜索框（胶囊）、设置项（14dp 圆角）、导出路径（12dp 圆角）观感不同；
 * - textStyle 传 null 时统一取 LocalCouiTextStyle 并叠加 onSurface/禁用色，
 *   传值则完全交给调用方（个别页面需要 14sp/15sp 与行高）。
 *
 * 保留 BasicTextField 而不是换成 COUI 控件：COUI 无输入控件可用，
 * 且需要继续支持 input 内部的 emoji 按钮、清空按钮、密码可见性切换等尾部插槽。
 */
@Composable
internal fun CouiTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = CouiTheme.colorScheme.container,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    minHeight: Dp = 44.dp,
    textStyle: TextStyle? = null,
    cursorColor: Color? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    CouiTextInputFrame(
        isEmpty = value.isEmpty(),
        placeholder = placeholder,
        enabled = enabled,
        singleLine = singleLine,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentPadding = contentPadding,
        minHeight = minHeight,
        leading = leading,
        trailing = trailing,
    ) {
        // 用 BasicTextField 的 String 重载：若先包成 TextFieldValue 再回写，
        // 每次重组都会重建 selection，光标会被顶回行首。
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = couiTextInputTextStyle(textStyle, enabled),
            cursorBrush = SolidColor(cursorColor ?: CouiTheme.colorScheme.accent),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * TextFieldValue 重载：复制文本设置页要在光标处插入占位符、下载设置页要按选区替换，
 * 这类调用方自己持有 selection，不能降级成纯 String。
 */
@Composable
internal fun CouiTextInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = CouiTheme.colorScheme.container,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    minHeight: Dp = 44.dp,
    textStyle: TextStyle? = null,
    cursorColor: Color? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    CouiTextInputFrame(
        isEmpty = value.text.isEmpty(),
        placeholder = placeholder,
        enabled = enabled,
        singleLine = singleLine,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentPadding = contentPadding,
        minHeight = minHeight,
        leading = leading,
        trailing = trailing,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = couiTextInputTextStyle(textStyle, enabled),
            cursorBrush = SolidColor(cursorColor ?: CouiTheme.colorScheme.accent),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 容器外壳：负责圆角、底色、内边距、前后插槽与空值占位。
 * 占位与输入框叠在同一个 Box 里，靠 [isEmpty] 决定是否绘制，位置随行数对齐。
 */
@Composable
private fun CouiTextInputFrame(
    isEmpty: Boolean,
    placeholder: String,
    enabled: Boolean,
    singleLine: Boolean,
    modifier: Modifier,
    shape: Shape,
    containerColor: Color,
    contentPadding: PaddingValues,
    minHeight: Dp,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    field: @Composable () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(containerColor)
            .padding(contentPadding),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leading != null) {
            leading()
            // 图标与文字之间统一 8dp，避免各页各写一个 Spacer。
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (isEmpty && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            field()
        }
        if (trailing != null) {
            // 尾部按钮自带 32/40dp 命中区，这里只留 4dp 视觉间隙。
            Spacer(modifier = Modifier.width(4.dp))
            trailing()
        }
    }
}

/**
 * 未显式给样式时统一取 COUI 正文样式并叠加颜色：
 * 禁用态用 onSurfaceVariant 表示不可编辑，与启用态区分。
 * 返回类型交给推断，避免把 LocalCouiTextStyle 的具体类型写死在签名里。
 */
@Composable
private fun couiTextInputTextStyle(textStyle: TextStyle?, enabled: Boolean) =
    textStyle ?: LocalCouiTextStyle.current.copy(
        color = if (enabled) CouiTheme.colorScheme.onSurface else CouiTheme.colorScheme.onSurfaceVariant,
    )
