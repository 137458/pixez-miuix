package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import android.os.Build
import com.itos.coui.dialog.CouiDialogActionColors
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.CouiDialogMaterial
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme

/** 卡片与对话框表面：卡片底色圆角，以及对话框的背景材质、落位与按键配色统一在此。 */

/**
 * COUI 卡片容器：卡片底色 + 连续圆角裁切，列表分组与结果行共用。
 */
@Composable
internal fun CouiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card),
        content = content,
    )
}

/**
 * COUI 弹窗玻璃材质的系统门槛：库内 CouiDialogDefaults 自身以 API 33 为界（javap 字节码
 * 中 SDK_INT 与 33 比较后决定材质），低于该版本库就退回不透明，所以这里与库保持一致，
 * 不能借用 ChipTabRow 的 34 门槛，否则 API 33 设备会无故丢失玻璃材质。
 */
/** COUI 弹窗材质：达到库的门槛且窗口具备背景采样时才启用玻璃，否则退回不透明。 */
@Composable
internal fun couiDialogMaterialOrNull(): CouiDialogMaterial? =
    if (Build.VERSION.SDK_INT >= COUI_DIALOG_MATERIAL_MIN_SDK && rememberCouiWindowBackdrop() != null) {
        CouiDialogDefaults.material(CouiTheme.isDark)
    } else {
        null
    }

/** COUI 弹窗排布：宽屏居中，手机底部弹出。 */
@Composable
internal fun couiDialogPlacement(): CouiDialogPlacement =
    if (LocalConfiguration.current.screenWidthDp >= 600) {
        CouiDialogPlacement.Center
    } else {
        CouiDialogPlacement.Bottom
    }

/** 主操作配色：破坏性操作使用库内 destructive 色。 */
@Composable
internal fun couiPrimaryActionColors(danger: Boolean = false): CouiDialogActionColors {
    val accent = if (danger) {
        CouiDialogDefaults.colors(CouiTheme.isDark).destructive
    } else {
        CouiTheme.colorScheme.accent
    }
    return CouiDialogDefaults.primaryActionColors(accent)
}
