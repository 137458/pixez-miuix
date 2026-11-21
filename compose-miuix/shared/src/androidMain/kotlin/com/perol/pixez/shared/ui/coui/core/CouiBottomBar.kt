package com.perol.pixez.shared.ui.coui.core

import android.graphics.BlendMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.backdrop.drawCouiBackdrop
import com.itos.coui.effect.CouiFloatingMaterial
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.CouiFloatingShadow
import com.itos.coui.effect.CouiFloatingSurface
import com.itos.coui.effect.LocalCouiContourGlowEnabled
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.effect.couiFloatingSurface
import com.itos.coui.effect.couiNavigationFeedback
import com.itos.coui.effect.rememberCouiSpotlight
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.skin.CouiNavigationBarMetrics
import kotlin.math.roundToInt

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
 * 不直接用官方 NavigationBar：那一版是按「3 个条目 + 左右各一个圆形附件」设计的，
 * 胶囊宽度固定按 `可用宽度 - 136dp` 计算并给两个 56dp 附件预留位置，没有附件时左右会留下大片空底。
 * 本工程是 5 个等分条目、没有附件按钮，因此这里取库内公开的浮动玻璃原语自装配：
 * [couiFloatingSurface] + [CouiFloatingMaterial] + [CouiFloatingShadow] + [CouiCapsuleShape]，
 * 材质、投影、条目度量与选中/未选中的文字颜色全部照抄官方底栏的取值，只是把胶囊铺满可用宽度，
 * 条目按可用宽度等分。
 *
 * 未读红点画在条目图标右上角（库条目没有内容槽，不能再叠一层复算位置）。
 *
 * API 31-33 官方玻璃材质不可用（依赖 API 34 的图形能力），退回 [drawCouiBackdrop] + 均匀模糊，
 * 版式与标注完全一致，采样源同样是被遮挡的正文。
 */
@Composable
internal fun CouiBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val backdrop = LocalCouiFloatingBackdrop.current
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && backdrop != null) {
        CouiGlassBottomBar(items, safeIndex, onSelect, backdrop, modifier)
    } else {
        CouiPlainBottomBar(items, safeIndex, onSelect, backdrop, modifier)
    }
}

/**
 * 底栏版式常量。
 *
 * 尺寸全部取自官方浮动底栏（[CouiNavigationBarMetrics] 是它的唯一记录处），本工程只改「胶囊铺满
 * 可用宽度、条目等分」这一点，其余度量与官方逐项一致。
 */
internal object CouiBottomBarMetrics {
    /** 胶囊高 56dp，与官方菜单行同高。 */
    val capsuleHeight = CouiNavigationBarMetrics.rowHeight

    /** 条目高 48dp，在胶囊内垂直居中。 */
    val itemHeight = CouiNavigationBarMetrics.itemHeight

    /** 条目图标 24dp。 */
    val iconSize = CouiNavigationBarMetrics.iconSize

    /** 条目内容顶部内缩 4dp。 */
    val iconTopInset = CouiNavigationBarMetrics.iconTopInset

    /** 图标与标题间距 2dp。 */
    val iconLabelGap = 2.dp

    /** 标题 10sp / 行高 12sp / Medium，与官方底栏一致。 */
    val labelSize = 10.sp
    val labelLineHeight = 12.sp

    /** 胶囊左右内边距 12dp。 */
    val menuHorizontalPadding = CouiNavigationBarMetrics.menuHorizontalPadding

    /** 条目之间的间距 8dp。 */
    val itemSpacing = CouiNavigationBarMetrics.itemSpacing

    /** 外框左右与顶部内边距 16dp。 */
    val contentPadding = CouiNavigationBarMetrics.contentPadding

    /** 系统手势条不存在时补的悬浮留白，避免胶囊贴住屏幕底边。 */
    val floatingMargin = 8.dp

    /** 未读红点直径 7dp。 */
    val badgeSize = CouiNavigationBarMetrics.badgeSize

    /** 官方材质的模糊半径（像素），与 CouiFloatingMaterial.blurRadius 默认值对齐。 */
    const val BLUR_RADIUS_PX = 25f
}

/** 选中态与未选中态的文字/图标颜色，透明度照抄官方底栏。 */
private fun couiBarLabelColor(dark: Boolean, selected: Boolean): Color {
    val base = if (dark) Color.White else Color.Black
    return base.copy(alpha = if (selected) 230f / 255f else 184f / 255f)
}

/**
 * 玻璃底栏（API 34+）。
 *
 * 结构与库内自身浮层一致：一个绘制节点承载玻璃材质，内容录进 [androidx.compose.ui.graphics.GraphicsLayer]
 * 后由材质在其上方合成；条目自身只负责点击、无障碍选中语义与悬停反馈。
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun CouiGlassBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: CouiBackdrop,
    modifier: Modifier,
) {
    // 原生 shader 在构造时编译，失败就整体退回降级栏而不是抛给界面。
    val renderer = remember { runCatching { CouiFloatingRenderer.standalone() }.getOrNull() }
    if (renderer == null) {
        CouiPlainBottomBar(items, selectedIndex, onSelect, null, modifier)
        return
    }
    val dark = CouiTheme.isDark
    val densityScale = LocalDensity.current.density
    val contourGlow = LocalCouiContourGlowEnabled.current
    val themeTint = CouiTheme.materialTintFor(dark)
    val material = remember(dark, densityScale, themeTint) {
        CouiFloatingMaterial(
            firstColor = if (dark) Color(0x80666666) else Color(0xA3666666),
            firstBlendMode = if (dark) BlendMode.OVERLAY else BlendMode.COLOR_DODGE,
            secondColor = if (dark) Color(0xB3262626) else Color(0x80F0F0F0),
            edgeAlpha = if (dark) 0.8f else 0.25f,
            innerGlowAlpha = if (dark) 0.03f else 0.1f,
            index = 1,
            extension = (4f * densityScale).roundToInt(),
            lightColor = Color.White,
            themeTint = themeTint,
        )
    }
    val shadow = remember(dark, contourGlow, densityScale) {
        fun pixels(dp: Float): Float = (dp * densityScale).roundToInt().toFloat()
        fun offset(dp: Float): Float = (dp * densityScale).toInt().toFloat()
        CouiFloatingShadow(
            elevation = pixels(if (contourGlow) 24f else 350f),
            color = when {
                contourGlow -> Color(0x2D000000)
                dark -> Color(0x78000000)
                else -> Color(0x28000000)
            },
            lightY = pixels(if (contourGlow) -320f else -160f),
            lightZ = if (contourGlow) 0f else pixels(10000f),
            lightRadius = pixels(if (contourGlow) 1600f else 2000f),
            causticColor = Color.White.copy(alpha = 0x42 / 255f),
            layout = Rect(0f, 0f, 0f, offset(4.9f)),
            sdf = offset(10.4f),
            smoothCorner = true,
        )
    }
    val contentLayer = rememberGraphicsLayer()
    val (lightInput, drawLight) = rememberCouiSpotlight(
        interactive = true,
        preset = SpotlightPreset.OpaqueSmall2,
        dark = dark,
    )
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    val glassSurface = Modifier.couiFloatingSurface(
        backdrop = backdrop,
        renderer = renderer,
        material = material,
        shadow = shadow,
        shape = CouiCapsuleShape,
        contentLayer = contentLayer,
        contourGlowEnabled = contourGlow,
        foregroundComposition = true,
    ) {
        CouiFloatingSurface(
            bounds = Rect(Offset.Zero, Size(barSize.width.toFloat(), barSize.height.toFloat())),
            cornerWeight = 1f,
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(
                start = CouiBottomBarMetrics.contentPadding,
                end = CouiBottomBarMetrics.contentPadding,
                top = CouiBottomBarMetrics.contentPadding,
                bottom = CouiBottomBarMetrics.contentPadding + couiBottomFloatingMargin(),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CouiBottomBarMetrics.capsuleHeight)
                .onSizeChanged { barSize = it }
                .then(lightInput)
                .then(glassSurface),
        ) {
            CouiBarRow(
                items = items,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                dark = dark,
                modifier = Modifier
                    .drawWithContent {
                        contentLayer.record { this@drawWithContent.drawContent() }
                    }
                    .clip(CouiCapsuleShape)
                    .drawBehind { drawLight?.invoke(this) },
            )
        }
    }
}

/**
 * 降级浮动底栏（API 31-33）。
 *
 * 版式与玻璃栏逐项一致（外层 16dp、胶囊 56dp、条目 48dp、图标 24dp、标题 10sp）。
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
    val surfaceTint = scheme.container.copy(alpha = if (dark) 0.52f else 0.66f)
    val background = if (backdrop != null) {
        Modifier.drawCouiBackdrop(
            backdrop = backdrop,
            shape = { CouiCapsuleShape },
            effects = { blur(CouiBottomBarMetrics.BLUR_RADIUS_PX) },
            onDrawSurface = { drawRect(surfaceTint) },
        )
    } else {
        Modifier.clip(CouiCapsuleShape).background(surfaceTint)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(
                start = CouiBottomBarMetrics.contentPadding,
                end = CouiBottomBarMetrics.contentPadding,
                top = CouiBottomBarMetrics.contentPadding,
                bottom = CouiBottomBarMetrics.contentPadding + couiBottomFloatingMargin(),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CouiBottomBarMetrics.capsuleHeight)
                .then(background),
        ) {
            CouiBarRow(
                items = items,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                dark = dark,
            )
        }
    }
}

/** 条目行：铺满胶囊宽度并按条目数等分，左右 12dp、间距 8dp，与官方底栏一致。 */
@Composable
private fun CouiBarRow(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CouiBottomBarMetrics.menuHorizontalPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(CouiBottomBarMetrics.itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            CouiBarCell(
                item = item,
                selected = index == selectedIndex,
                dark = dark,
                badgeColor = CouiTheme.colorScheme.accent,
                onClick = { onSelect(index) },
            )
        }
    }
}

/**
 * 单个条目：图标在上、标题在下，整体上对齐 4dp。
 *
 * 图标与标题同色（选中 230/255、未选中 184/255），宿主只提供一张图标，因此选中态靠这两档透明度表达，
 * 与官方底栏在没有填充图标时的表现一致。
 */
@Composable
private fun RowScope.CouiBarCell(
    item: CouiBarItem,
    selected: Boolean,
    dark: Boolean,
    badgeColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = couiBarLabelColor(dark, selected)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(CouiBottomBarMetrics.itemHeight)
            .couiNavigationFeedback(dark = dark, interactionSource = interactionSource)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = CouiBottomBarMetrics.iconTopInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Image(
                    painter = rememberVectorPainter(item.icon),
                    contentDescription = null,
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
