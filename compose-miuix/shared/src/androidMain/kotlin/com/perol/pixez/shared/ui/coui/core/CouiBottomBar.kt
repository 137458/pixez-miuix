package com.perol.pixez.shared.ui.coui.core

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.backdrop.drawCouiBackdrop
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme

/**
 * 浮动底栏的玻璃采样源（正文视口）。
 *
 * COUI 底栏的材质是把「被遮挡的正文」作为背景源采样后重建出来的，源对象属于 Android 库类型，
 * 无法在 commonMain 创建，因此由皮肤宿主 [com.perol.pixez.shared.ui.skin.CouiSkinHost] 创建并下发；
 * 皮肤未开启时为 null，底栏退回实心材质而不是留出一块透明空洞。
 */
internal val LocalCouiFloatingBackdrop = compositionLocalOf<CouiBackdrop?> { null }

/** 底栏条目：标题、图标与未读标记；图标由宿主提供，库只按选中态换色。 */
internal data class CouiBarItem(
    val title: String,
    val icon: ImageVector,
    val showBadge: Boolean = false,
)

/**
 * 浮动底栏。
 *
 * API 34+ 直接用官方 [NavigationBar]：库内部的浮动材质、融球与系统手势区内边距全部交给官方实现，
 * 宿主只提供正文采样源与条目数据。库的条目只有图标与标题、没有内容槽，未读红点只能由
 * [badgeOverlay] 在底栏之上另叠一层（只绘制、不接手势）。
 *
 * API 31-33 官方底栏不可用（材质依赖 API 34 的图形能力），这里用库内公开的 [drawCouiBackdrop]
 * 与均匀模糊自绘同版式胶囊：采样的同样是正文，不是纯色底板。
 */
@Composable
internal fun CouiBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badgeOverlay: (@Composable BoxScope.(itemCount: Int) -> Unit)? = null,
) {
    val backdrop = LocalCouiFloatingBackdrop.current
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && backdrop != null) {
        CouiFloatingBottomBar(items, safeIndex, onSelect, backdrop, modifier, badgeOverlay)
    } else {
        CouiPlainBottomBar(items, safeIndex, onSelect, backdrop, modifier)
    }
}

/** 官方浮动底栏（API 34+）：材质、融球与手势区内边距都由库处理，宿主不叠加任何留白。 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun CouiFloatingBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: CouiBackdrop,
    modifier: Modifier,
    badgeOverlay: (@Composable BoxScope.(itemCount: Int) -> Unit)?,
) {
    Box(modifier = modifier) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            backdrop = backdrop,
            items = items.map { CouiNavigationItem(title = it.title, icon = it.icon) },
            selectedIndex = selectedIndex,
            dark = CouiTheme.isDark,
            onSelection = onSelect,
        )
        if (badgeOverlay != null && items.any { it.showBadge }) {
            badgeOverlay(items.size)
        }
    }
}

/** 底栏版式常量，取值与官方浮动底栏一致，供降级栏与红点叠层共用。 */
internal object CouiBottomBarMetrics {
    /** 胶囊高 56dp。 */
    val capsuleHeight = 56.dp

    /** 条目高 48dp，在胶囊内垂直居中。 */
    val itemHeight = 48.dp

    /** 图标 24dp。 */
    val iconSize = 24.dp

    /** 图标与标题间距 2dp。 */
    val iconLabelGap = 2.dp

    /** 标题 10sp / 行高 12sp / Medium，与官方底栏一致。 */
    val labelSize = 10.sp
    val labelLineHeight = 12.sp

    /** 外框左右与顶部内边距 16dp。 */
    val contentPadding = 16.dp

    /** 系统手势条不存在时补的悬浮留白，避免胶囊贴住屏幕底边。 */
    val floatingMargin = 8.dp

    /** 未读红点直径 7dp。 */
    val badgeSize = 7.dp

    /** 官方材质的模糊半径（像素），与 CouiFloatingMaterial.blurRadius 默认值对齐。 */
    const val BLUR_RADIUS_PX = 25f
}

/**
 * 降级浮动底栏（API 31-33）。
 *
 * 版式照官方底的胶囊：外层 16dp、胶囊 56dp、条目 48dp、图标 24dp、标题 10sp。
 * 背景不是纯色底板——用 [drawCouiBackdrop] 采样正文并做均匀模糊，再叠一层主题色半透明面，
 * 保证在页面底色与图片上都有玻璃感且正文仍可辨认。
 */
@Composable
private fun CouiPlainBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: CouiBackdrop?,
    modifier: Modifier,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val selectedColor = (if (dark) Color.White else Color.Black).copy(alpha = 230f / 255f)
    val unselectedColor = (if (dark) Color.White else Color.Black).copy(alpha = 184f / 255f)
    val surfaceTint = scheme.container.copy(alpha = if (dark) 0.52f else 0.66f)
    val bottomMargin = couiBottomFloatingMargin()
    val shape = CouiCapsuleShape
    val background = if (backdrop != null) {
        Modifier.drawCouiBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = { blur(CouiBottomBarMetrics.BLUR_RADIUS_PX) },
            onDrawSurface = { drawRect(surfaceTint) },
        )
    } else {
        Modifier.clip(shape).background(surfaceTint)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(
                start = CouiBottomBarMetrics.contentPadding,
                end = CouiBottomBarMetrics.contentPadding,
                top = CouiBottomBarMetrics.contentPadding,
                bottom = CouiBottomBarMetrics.contentPadding + bottomMargin,
            )
            .height(CouiBottomBarMetrics.capsuleHeight)
            .then(background)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        items.forEachIndexed { index, item ->
            CouiPlainBottomBarItem(
                item = item,
                selected = index == selectedIndex,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                badgeColor = scheme.accent,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun CouiPlainBottomBarItem(
    item: CouiBarItem,
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val tint = if (selected) selectedColor else unselectedColor
    Column(
        modifier = modifier
            .height(CouiBottomBarMetrics.itemHeight)
            .couiButtonFeedback(dark = CouiTheme.isDark, interactionSource = source, list = false)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Image(
                painter = rememberVectorPainter(item.icon),
                contentDescription = item.title,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(CouiBottomBarMetrics.iconSize),
            )
            if (item.showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(CouiBottomBarMetrics.badgeSize)
                        .clip(CircleShape)
                        .background(badgeColor),
                )
            }
        }
        Spacer(Modifier.height(CouiBottomBarMetrics.iconLabelGap))
        BasicText(
            text = item.title,
            style = TextStyle(
                fontSize = CouiBottomBarMetrics.labelSize,
                lineHeight = CouiBottomBarMetrics.labelLineHeight,
                fontWeight = FontWeight.Medium,
                color = tint,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 系统手势条已提供底部可点击区时不再补悬浮留白，避免胶囊被顶高一截。 */
@Composable
private fun couiBottomFloatingMargin() = if (hasTappableBottomInset()) {
    0.dp
} else {
    CouiBottomBarMetrics.floatingMargin
}

@Composable
private fun hasTappableBottomInset(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.tappableElement.getBottom(density) > 0
}
