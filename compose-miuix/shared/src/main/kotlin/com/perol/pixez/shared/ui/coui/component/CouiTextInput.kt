package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
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
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle

/**
 * COUI 文本输入原语：容器 + 官方版式令牌 + 空值占位。
 *
 * 库内没有可复用的输入框：`coui/input/` 只有 IME 定位、触觉与指针内部件
 * （CouiHaptics / PointerEvents / CouiMovingActionClick / CouiSearchRelocationBoundary），
 * SearchBar 与 InputDialog 的输入区都内嵌在各自组件里取不出来，所以这里继续用 BasicTextField 自绘。
 * 容器版式按官方分组卡片同源：
 * - 底色取 `CouiTheme.colorScheme.card`，与官方 Group 卡片同一令牌（CouiPageComponents.kt:92-93）；
 * - 圆角取 `CouiPageDefaults.shape`，即 CouiShape(24.dp, 3f)（CouiPageComponents.kt:54）；
 * - 内边距取 `CouiPageDefaults.rowPadding`(16) 与 `contentPadding`(10)（CouiPageComponents.kt:50、134-135）；
 * - 前置图标与文字的间距取 `CouiPageDefaults.iconSpacing`，尾部插槽按官方 Item 贴边放置（CouiPageComponents.kt:147、152）；
 * - 文字走官方 Text + CouiTextKind.ItemTitle，占位与禁用色取 `themeColors()` 的 summary/disabled
 *   （CouiPageComponents.kt:66-67）；
 * - 光标色取 `colorScheme.accent`。
 *
 * 公开签名（参数名、顺序、类型）与改造前逐字一致，仅默认值改为官方常量；
 * 调用方仍可覆盖 shape / containerColor / contentPadding / minHeight 做搜索胶囊等特化。
 * 容器本身不是可点表面：官方 Item 在无 onClick 时同样不安装点击与反馈（CouiPageComponents.kt:138-142），
 * 焦点、光标与 IME 由 BasicTextField 承担，尾部按钮的按压反馈由插槽自带。
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
    shape: Shape = CouiPageDefaults.shape,
    containerColor: Color = CouiTheme.colorScheme.card,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = CouiPageDefaults.rowPadding,
        vertical = CouiPageDefaults.contentPadding,
    ),
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
    shape: Shape = CouiPageDefaults.shape,
    containerColor: Color = CouiTheme.colorScheme.card,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = CouiPageDefaults.rowPadding,
        vertical = CouiPageDefaults.contentPadding,
    ),
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
 * TextFieldState 重载：草稿由单一状态源承载时使用（评论输入栏在 API 34+ 走 COUI `MessageInputBar`、
 * 低版本走自绘胶囊，两条渲染路径共用同一份草稿，不能退化成 String 往返以免光标跳回行首）。
 * 状态源重载没有 visualTransformation / keyboardActions：文本变换改由 InputTransformation 承担，
 * IME 动作由库在装饰层内部处理，调用方无需也不该再传。
 */
@Composable
internal fun CouiTextInput(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: Shape = CouiPageDefaults.shape,
    containerColor: Color = CouiTheme.colorScheme.card,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = CouiPageDefaults.rowPadding,
        vertical = CouiPageDefaults.contentPadding,
    ),
    minHeight: Dp = 44.dp,
    textStyle: TextStyle? = null,
    cursorColor: Color? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    CouiTextInputFrame(
        isEmpty = state.text.isEmpty(),
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
            state = state,
            enabled = enabled,
            textStyle = couiTextInputTextStyle(textStyle, enabled),
            cursorBrush = SolidColor(cursorColor ?: CouiTheme.colorScheme.accent),
            keyboardOptions = keyboardOptions,
            lineLimits = if (singleLine) {
                TextFieldLineLimits.SingleLine
            } else {
                TextFieldLineLimits.MultiLine(maxHeightInLines = maxLines)
            },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 容器外壳：圆角、底色、内边距、前后插槽与空值占位。
 * 圆角裁切后铺底色，顺序与官方 Group 卡片一致（CouiPageComponents.kt:92-93）；
 * 最小高度用 heightIn 硬下限，保持改造前 [minHeight] 的「不低于」语义。
 * 占位与输入框叠在同一个 Box 里，靠 [isEmpty] 决定是否绘制，位置随行数对齐
 * （官方 SearchBar 同样把占位与 field 叠在同一格，SearchBar.kt:255-258）。
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
    val colors = CouiPageDefaults.themeColors()
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
            // 图标与文字的间距沿用官方行版式图标间距，避免各页各写一个 Spacer。
            Spacer(modifier = Modifier.width(CouiPageDefaults.iconSpacing))
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (isEmpty && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    kind = CouiTextKind.ItemTitle,
                    color = if (enabled) colors.summary else colors.disabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            field()
        }
        // 官方 Item 的尾部控件贴边放置，间隙由控件自带命中区提供（CouiPageComponents.kt:152）。
        if (trailing != null) trailing()
    }
}

/**
 * 未显式给样式时统一取 COUI 正文样式（LocalCouiTextStyle，即官方 body）并叠加颜色：
 * 启用态用 themeColors().title（= onSurface），禁用态用官方 disabled 令牌（CouiPageComponents.kt:66-67）。
 */
@Composable
private fun couiTextInputTextStyle(textStyle: TextStyle?, enabled: Boolean): TextStyle {
    if (textStyle != null) return textStyle
    val colors = CouiPageDefaults.themeColors()
    return LocalCouiTextStyle.current.copy(color = if (enabled) colors.title else colors.disabled)
}
