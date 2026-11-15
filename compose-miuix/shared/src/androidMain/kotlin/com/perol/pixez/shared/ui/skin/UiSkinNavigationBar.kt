package com.perol.pixez.shared.ui.skin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.COUI_LIBRARY_NAVIGATION_BAR_MIN_SDK
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback

/** 底栏入口：API 34+ 用 COUI 官方浮动底栏，API 31-33 用同版式自绘平替。 */
@Composable
internal fun CouiSkinNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    if (Build.VERSION.SDK_INT >= COUI_LIBRARY_NAVIGATION_BAR_MIN_SDK) {
        CouiLibraryNavigationBar(items, selectedIndex, onSelect, modifier)
    } else {
        CouiPlainNavigationBar(items, selectedIndex, onSelect, modifier)
    }
}

/**
 * COUI 官方浮动底栏（API 34+）。
 *
 * 库的条目只接受图标与标题、没有内容槽，红点塞不进条目内部，
 * 因此这里在底栏之上叠一层只负责绘制、不接手势的图层，见 [CouiLibraryBadgeLayer]。
 */
@RequiresApi(COUI_LIBRARY_NAVIGATION_BAR_MIN_SDK)
@Composable
private fun CouiLibraryNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    val backdrop = rememberCouiBackdrop()
    val renderer = remember { CouiFloatingRenderer() }
    // 叠层要求外层有确定的尺寸，故用 Box 包住官方底栏；modifier 挂在 Box 上，底栏自身撑满宽度。
    Box(modifier = modifier) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            backdrop = backdrop,
            renderer = renderer,
            items = items.map { CouiNavigationItem(title = it.title, icon = it.icon) },
            selectedIndex = selectedIndex,
            dark = CouiTheme.isDark,
            onSelection = onSelect,
        )
        if (items.any { it.showBadge }) {
            CouiLibraryBadgeLayer(items)
        }
    }
}

/**
 * 未读红点叠层。
 *
 * 几何常量逐条抄自 COUI-Kit 0.0.1-alpha11 的 navigation/NavigationBar.kt，
 * 用来复算官方底栏每个条目的位置：外框上 16、左右 16，底部留白 16 加系统可点击区判断；
 * 内层可用宽上限 = 最大菜单宽 + 136（左右各 56 的圆钮再留 24），行高 56，条目高 48，
 * 胶囊水平内边距 12、条目间距 8、条目内图标顶部留白 4、图标 24 居中。
 * 库里没有可复用的定位接口，升级库后必须复核这些数字；本层只画点，不接管点击与无障碍语义。
 */
@RequiresApi(34)
@Composable
private fun BoxScope.CouiLibraryBadgeLayer(items: List<SkinNavItem>) {
    val scheme = CouiTheme.colorScheme
    val maximumMenuWidth = 24.dp + 70.dp * items.size + 8.dp * (items.size - 1)
    val bottomMargin = couiLibraryBottomMargin()
    Box(
        modifier = Modifier
            .matchParentSize()
            .windowInsetsPadding(
                WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomMargin),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = maximumMenuWidth + 136.dp)
                .fillMaxWidth(),
        ) {
            val menuWidth = (maxWidth - 136.dp).coerceAtLeast(0.dp)
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.width(56.dp))
                Row(
                    modifier = Modifier
                        .width(menuWidth)
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEach { item ->
                        Box(modifier = Modifier.weight(1f).height(48.dp)) {
                            if (item.showBadge) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        // 图标 24 居中，红点右边贴图标右边：中心右移 12 - 3.5 = 8.5；竖向沿用条目内图标顶部 4 的留白。
                                        .offset(x = 8.5.dp, y = 4.dp)
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(scheme.accent),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(56.dp))
            }
        }
    }
}

/** 与库一致：系统底部可点击区存在时不再额外留白，避免红点层与底栏错位。 */
@Composable
private fun couiLibraryBottomMargin(): Dp {
    val density = LocalDensity.current
    return if (WindowInsets.tappableElement.getBottom(density) > 0) 0.dp else 8.dp
}

/** API 31-33 平替：同版式的浮动胶囊，不依赖库的窗口特性。 */
@Composable
private fun CouiPlainNavigationBar(
    items: List<SkinNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val dark = CouiTheme.isDark
    val selectedColor = if (dark) Color.White else Color.Black
    val unselectedColor = selectedColor.copy(alpha = 0.55f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(scheme.container)
            .height(64.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val pressInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .couiPressFeedback(pressInteraction)
                    .selectable(
                        selected = selected,
                        interactionSource = pressInteraction,
                        indication = null,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Image(
                            painter = rememberVectorPainter(item.icon),
                            contentDescription = item.title,
                            colorFilter = ColorFilter.tint(if (selected) selectedColor else unselectedColor),
                            modifier = Modifier.size(24.dp),
                        )
                        if (item.showBadge) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(scheme.accent),
                            )
                        }
                    }
                    Text(
                        text = item.title,
                        kind = CouiTextKind.ItemSummary,
                        color = if (selected) selectedColor else unselectedColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
