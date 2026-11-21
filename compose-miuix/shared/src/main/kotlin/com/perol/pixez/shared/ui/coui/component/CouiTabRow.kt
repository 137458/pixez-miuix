package com.perol.pixez.shared.ui.coui.component

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.effect.rememberCouiSpotlight
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.motion.couiPressMotion
import com.itos.coui.motion.rememberCouiPressMotion
import com.itos.coui.segmentbutton.SegmentedControl
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.core.LocalCouiContentBackdrop

/** 将页签选中下标钳制到合法区间，列表为空时返回 -1。 */
internal fun clampCouiTabIndex(selectedIndex: Int, itemCount: Int): Int =
    if (itemCount <= 0) -1 else selectedIndex.coerceIn(0, itemCount - 1)

/** 在多项分类中校验选中的 key，缺失时回落到首项，防止官方 ChipTabRow 断言失败。 */
internal fun resolveCouiChipTabKey(selectedKey: String, chips: List<CouiFilterChip>): String? {
    if (chips.isEmpty()) return null
    return if (chips.any { it.key == selectedKey }) selectedKey else chips.first().key
}

/**
 * 胶囊分段选择（参考 jm3 `CouiSegmentedCapsules`）：
 * - API 34+ 委托给 COUI 库官方 [SegmentedControl]（支持拖选、弹性阻尼与微光）；
 * - API 31-33 退化为等分胶囊实现（带按压缩放与凝光）。
 *
 * [backdrop] 默认为空，采用官方不透明分组填色，可安全放进滚动列表、卡片或弹窗内；
 * 放在顶栏 [topBarBottom] 槽位时通过 [CouiTopBarSegmentedTabs] 传入正文兄弟源开启毛玻璃模糊。
 */
@Composable
internal fun CouiSegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    enabled: Boolean = true,
    backdrop: CouiBackdrop? = null,
) {
    val clampedIndex = clampCouiTabIndex(selectedIndex, items.size)
    if (clampedIndex < 0) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiLibrarySegmentedControl(
            items = items,
            selectedIndex = clampedIndex,
            onSelect = onSelect,
            modifier = modifier,
            small = small,
            enabled = enabled,
            backdrop = backdrop,
        )
        return
    }
    CouiFallbackSegmentedTabs(
        items = items,
        selectedIndex = clampedIndex,
        onSelect = onSelect,
        modifier = modifier,
        small = small,
        enabled = enabled,
    )
}

/**
 * 固定在顶栏下方的分段胶囊栏（参考 jm3 `CouiTopBarSegmentedTabs`）：
 * 放置在 `Scaffold` 的 `topBarBottom` 槽位内，直接消费 [LocalCouiBackdrop] 采样滚动正文，
 * API 34+ 呈现原厂透明模糊与凝光滑块，API 31-33 退化为等分胶囊。
 */
@Composable
internal fun CouiTopBarSegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val backdrop = LocalCouiContentBackdrop.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CouiPageDefaults.horizontalMargin, vertical = 6.dp),
    ) {
        CouiSegmentedTabs(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            small = small,
            enabled = enabled,
            backdrop = backdrop,
        )
    }
}

/**
 * 顶栏下方可横向滚动的分类 TabRow（参考 jm3 `CouiBrowseCategoryBar`）：
 * - API 34+ 且位于 `topBar` 采样作用域（[LocalCouiBackdrop] 非空且存在有效显示器）时，
 *   调用官方 [ChipTabRow]（含原厂凝光玻璃、边缘羽化、物理阻尼滚动与自动滚入选中项）；
 * - 低版本或无独立兄弟背景源时，退化为 32dp 轻量滚动胶囊轨，保持低高度占比与一致选中色。
 */
@Composable
internal fun CouiChipTabRow(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveKey = resolveCouiChipTabKey(selectedKey, chips) ?: return
    val backdrop = LocalCouiContentBackdrop.current
    val display = LocalView.current.display
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && backdrop != null && display != null) {
        CouiLibraryChipTabRow(
            chips = chips,
            selectedKey = effectiveKey,
            onSelect = onSelect,
            backdrop = backdrop,
            modifier = modifier,
        )
        return
    }
    CouiFallbackChipTabRow(
        chips = chips,
        selectedKey = effectiveKey,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun CouiLibrarySegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    small: Boolean,
    enabled: Boolean,
    backdrop: CouiBackdrop?,
) {
    val renderer = remember(backdrop != null) {
        if (backdrop != null) runCatching { CouiFloatingRenderer.standalone() }.getOrNull() else null
    }
    val effectiveBackdrop = if (renderer != null) backdrop else null
    SegmentedControl(
        modifier = modifier.fillMaxWidth(),
        labels = items,
        selectedIndex = selectedIndex,
        onSelected = onSelect,
        backdrop = effectiveBackdrop,
        dark = CouiTheme.isDark,
        enabled = enabled,
        small = small,
        renderer = renderer,
    )
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun CouiLibraryChipTabRow(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    backdrop: CouiBackdrop,
    modifier: Modifier,
) {
    val items = remember(chips) {
        chips.map { chip -> CouiChipTabItem(key = chip.key, label = chip.label) }
    }
    val renderers = remember(items.map { it.key }) {
        runCatching { items.map { CouiFloatingRenderer.standalone() } }.getOrNull()
    }
    if (renderers == null) {
        CouiFallbackChipTabRow(
            chips = chips,
            selectedKey = selectedKey,
            onSelect = onSelect,
            modifier = modifier,
        )
        return
    }
    ChipTabRow(
        modifier = modifier.fillMaxWidth(),
        items = items,
        selectedKey = selectedKey,
        onSelectedKeyChange = { key ->
            val targetKey = key as? String ?: return@ChipTabRow
            onSelect(targetKey)
        },
        backdrop = backdrop,
        dark = CouiTheme.isDark,
        renderers = renderers,
    )
}

@Composable
private fun CouiFallbackSegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    small: Boolean,
    enabled: Boolean,
) {
    val colors = CouiTheme.colorScheme
    val labelStyle = rememberCouiTextStyle(CouiTextKind.ItemValue)
    val capsuleHeight = if (small) 32.dp else 40.dp
    val containerPadding = if (small) 2.dp else 4.dp
    val dark = CouiTheme.isDark
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CouiCapsuleShape)
            .background(colors.container)
            .padding(horizontal = containerPadding, vertical = containerPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(containerPadding),
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val press = rememberCouiPressMotion()
            val interaction = remember { MutableInteractionSource() }
            val (lightInput, drawLight) = rememberCouiSpotlight(
                interactive = enabled,
                preset = SpotlightPreset.ChipTab,
                dark = dark,
                color = if (selected) colors.accent else colors.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(capsuleHeight)
                    .graphicsLayer {
                        scaleX = press.scale.value
                        scaleY = press.scale.value
                    }
                    .clip(CouiCapsuleShape)
                    .background(
                        if (selected) colors.accent.copy(alpha = 0.16f) else colors.container,
                    )
                    .then(lightInput)
                    .drawBehind { drawLight?.invoke(this) }
                    .couiPressMotion(press = press, enabled = enabled, cancelOnConsumedMove = false)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val textColor = if (selected) colors.accent else colors.onSurfaceVariant
                BasicText(
                    text = label,
                    style = labelStyle,
                    color = { if (enabled) textColor else textColor.copy(alpha = 0.38f) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CouiFallbackChipTabRow(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val colors = CouiTheme.colorScheme
    val labelStyle = rememberCouiTextStyle(CouiTextKind.ItemSummary)
    val dark = CouiTheme.isDark
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = CouiPageDefaults.horizontalMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips, key = { it.key }) { chip ->
            val selected = chip.key == selectedKey
            val interaction = remember { MutableInteractionSource() }
            val (lightInput, drawLight) = rememberCouiSpotlight(
                interactive = true,
                preset = SpotlightPreset.ChipTab,
                dark = dark,
                color = if (selected) colors.onAccent else colors.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CouiCapsuleShape)
                    .background(if (selected) colors.accent else colors.container)
                    .then(lightInput)
                    .drawBehind { drawLight?.invoke(this) }
                    .couiPressFeedback(interaction, list = false)
                    .selectable(
                        selected = selected,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(chip.key) },
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                val textColor = if (selected) colors.onAccent else colors.onSurfaceVariant
                BasicText(
                    text = chip.label,
                    style = labelStyle,
                    color = { textColor },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
