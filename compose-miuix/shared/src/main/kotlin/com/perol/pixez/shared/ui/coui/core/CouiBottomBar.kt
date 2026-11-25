package com.perol.pixez.shared.ui.coui.core

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.couiNavigationFeedback
import com.itos.coui.navigation.CouiNavigationBarAction
import com.itos.coui.navigation.CouiNavigationBarDefaults
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.COUI_NAVIGATION_BAR_MIN_SDK

/**
 * 浮动底栏的玻璃采样源（正文视口）。
 *
 * COUI 官方 [NavigationBar] 的材质会把「被遮挡的正文」作为背景源采样后重建出来，
 * 因此由皮肤宿主 [com.perol.pixez.shared.ui.skin.CouiSkinHost] 创建并下发；
 * 皮肤未开启或未提供采样源时为 null，底栏退回贴底简化栏。
 */
internal val LocalCouiFloatingBackdrop = compositionLocalOf<CouiBackdrop?> { null }

/** 底栏条目：标题、未选中图标、选中图标与未读标记。 */
internal data class CouiBarItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val showBadge: Boolean = false,
)

/** 底栏左右两侧圆形附件配置。 */
internal data class CouiBarAccessory(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
)

/** 判定是否具备走官方三主体浮动 [NavigationBar] 的条件。 */
internal fun shouldUseOfficialCouiNavigationBar(
    sdkInt: Int,
    isFloating: Boolean,
    hasBackdrop: Boolean,
    hasRenderer: Boolean,
): Boolean = sdkInt >= COUI_NAVIGATION_BAR_MIN_SDK && isFloating && hasBackdrop && hasRenderer

/**
 * 计算当前环境下是否实际以浮动底栏形态呈现（用于底栏内容留白与皮肤底栏模式对齐）：
 * COUI 皮肤在非宽屏且低于 API 34 时强制退回贴底栏，此时不再多预留浮动底栏高度。
 */
internal fun resolveCouiEffectiveFloatingBottomBar(
    isCouiSkin: Boolean,
    sdkInt: Int,
    isWideScreen: Boolean,
    useFloatingBottomBar: Boolean,
): Boolean {
    if (!useFloatingBottomBar) return false
    if (!isCouiSkin || isWideScreen) return true
    return sdkInt >= COUI_NAVIGATION_BAR_MIN_SDK
}

/**
 * COUI 底栏。
 *
 * API 34+ 且处于浮动模式且背景源与三主体 [CouiFloatingRenderer] 就绪时，直接装配官方
 * [NavigationBar]（左设置玻璃圆钮 + 中间 4 项胶囊 + 右主题色搜索圆钮）；
 * API 31-33、Shader 初始化失败或关闭「浮动底栏」时，退回官方 Demo 同款简洁贴底栏。
 */
@Composable
internal fun CouiBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isFloating: Boolean = true,
    leftAccessory: CouiBarAccessory? = null,
    rightAccessory: CouiBarAccessory? = null,
    badgeOverlay: (@Composable BoxScope.(itemCount: Int) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val backdrop = LocalCouiFloatingBackdrop.current
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val renderer = remember {
        if (Build.VERSION.SDK_INT >= COUI_NAVIGATION_BAR_MIN_SDK) {
            runCatching { CouiFloatingRenderer() }.getOrNull()
        } else {
            null
        }
    }
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        shouldUseOfficialCouiNavigationBar(
            sdkInt = Build.VERSION.SDK_INT,
            isFloating = isFloating,
            hasBackdrop = backdrop != null,
            hasRenderer = renderer != null,
        ) &&
        backdrop != null &&
        renderer != null
    ) {
        CouiOfficialNavigationBar(
            items = items,
            selectedIndex = safeIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            renderer = renderer,
            leftAccessory = leftAccessory,
            rightAccessory = rightAccessory,
            badgeOverlay = badgeOverlay,
            modifier = modifier,
        )
    } else {
        CouiPlainDockedBottomBar(
            items = items,
            selectedIndex = safeIndex,
            onSelect = onSelect,
            leftAccessory = leftAccessory,
            rightAccessory = rightAccessory,
            modifier = modifier,
        )
    }
}

/** 官方三主体浮动底栏（API 34+）：材质、融球、拖拽预选与系统手势安全区全部交由 [NavigationBar]。 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun CouiOfficialNavigationBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: CouiBackdrop,
    renderer: CouiFloatingRenderer,
    leftAccessory: CouiBarAccessory?,
    rightAccessory: CouiBarAccessory?,
    badgeOverlay: (@Composable BoxScope.(itemCount: Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val navItems = remember(items) {
        items.map { item ->
            CouiNavigationItem(
                title = item.title,
                icon = item.icon,
                selectedIcon = item.selectedIcon,
            )
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            backdrop = backdrop,
            renderer = renderer,
            items = navItems,
            selectedIndex = selectedIndex,
            leftAction = leftAccessory?.let {
                CouiNavigationBarAction(
                    icon = it.icon,
                    contentDescription = it.description,
                    onClick = it.onClick,
                    style = CouiNavigationBarDefaults.transparentActionStyle(),
                )
            },
            rightAction = rightAccessory?.let {
                CouiNavigationBarAction(
                    icon = it.icon,
                    contentDescription = it.description,
                    onClick = it.onClick,
                    style = CouiNavigationBarDefaults.coloredActionStyle(),
                )
            },
            dark = CouiTheme.isDark,
            lightColor = Color.White,
            onSelection = onSelect,
        )
        if (badgeOverlay != null && items.any { it.showBadge }) {
            badgeOverlay(items.size)
        }
    }
}

/** 官方 Demo 同款简洁贴底栏：供关闭浮动底栏或 API 31-33 降级时使用，首尾对称保留设置与搜索入口。 */
@Composable
private fun CouiPlainDockedBottomBar(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    leftAccessory: CouiBarAccessory?,
    rightAccessory: CouiBarAccessory?,
    modifier: Modifier = Modifier,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(scheme.outlineVariant),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leftAccessory != null) {
                CouiDockedAccessoryItem(accessory = leftAccessory, dark = dark)
            }
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val source = remember { MutableInteractionSource() }
                val tint = if (selected) scheme.accent else scheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CouiCapsuleShape)
                        .couiNavigationFeedback(dark = dark, interactionSource = source)
                        .selectable(
                            selected = selected,
                            interactionSource = source,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Image(
                            painter = rememberVectorPainter(if (selected) item.selectedIcon else item.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(tint),
                            modifier = Modifier.size(24.dp),
                        )
                        if (item.showBadge) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(scheme.accent),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = item.title,
                        style = TextStyle(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = tint,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (rightAccessory != null) {
                CouiDockedAccessoryItem(accessory = rightAccessory, dark = dark)
            }
        }
    }
}

@Composable
private fun RowScope.CouiDockedAccessoryItem(
    accessory: CouiBarAccessory,
    dark: Boolean,
) {
    val scheme = CouiTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val tint = scheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(CouiCapsuleShape)
            .couiNavigationFeedback(dark = dark, interactionSource = source)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = accessory.onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberVectorPainter(accessory.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = accessory.description,
            style = TextStyle(
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                color = tint,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
