package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.itos.coui.layout.Scaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar

/** COUI 页面骨架：大标题顶栏与随折叠收缩的正文留白在这一层收口，页面不必各自重复挂 nestedScroll。 */

/**
 * COUI 一级页面骨架：折叠大标题顶栏 + 动作槽，正文使用 COUI 页面底色。
 *
 * 折叠行为与正文留白在这一层收口：nestedScroll 只在这里挂一次，传给 [content] 的
 * [PaddingValues] 顶部留白会随顶栏折叠位移实时收缩。页面直接用 `Modifier.padding(padding)`
 * 或交给列表的 contentPadding 即可；若在组合期把它算成字面量，首帧的顶栏高度就会被冻结，
 * 顶栏折叠后正文不再跟随。
 */
@Composable
internal fun CouiPageScaffold(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = CouiTopAppBarDefaults.colors(
        backgroundColor = CouiTheme.colorScheme.background,
        dark = CouiTheme.isDark,
    )
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    BasicText(
                        text = title,
                        style = LocalCouiTextStyle.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = colors,
            )
        },
    ) { innerPadding ->
        val density = LocalDensity.current
        // 返回值是活对象：顶部留白在测量期读取折叠位移，组合期不会冻结首帧高度。
        val contentPadding = remember(scrollBehavior, innerPadding, density) {
            scrollBehavior.state.contentPadding(innerPadding, density)
        }
        content(contentPadding)
    }
}
