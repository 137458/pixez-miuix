package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.ui.AppConstants

/** 可读列宽：平板与桌面把正文限制在最大阅读宽度内居中，避免长行伤眼。 */

/**
 * COUI 阅读列：内容居中并限制到 [AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP]。
 *
 * 用于阅读型内容（评论列表、用户资料区）与占位/空态，避免平板上的超长文本行；
 * 画廊型页面不套这一层，列宽交给自适应网格。
 */
@Composable
internal fun CouiReadableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
            content = content,
        )
    }
}
