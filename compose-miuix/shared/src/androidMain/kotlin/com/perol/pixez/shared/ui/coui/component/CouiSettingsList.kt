package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding

/**
 * COUI 设置页统一列表骨架：横向 12dp、顶部 8dp 留白，底部让开悬浮底栏，卡片间距 12dp。
 *
 * 这组数值此前在十余个设置页各写一遍，改一处边距就要全量跟改，且很容易漏掉
 * `rememberCouiOverscrollEffect()` 导致个别页面失去回弹。宽度约束仍由页面自己用
 * [CouiReadableColumn] 表达，两者职责不同：一个管列宽，一个管列表节奏。
 */
@Composable
internal fun CouiSettingsLazyColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 8.dp,
            end = 12.dp,
            bottom = LocalBottomBarContentPadding.current,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        content()
    }
}
