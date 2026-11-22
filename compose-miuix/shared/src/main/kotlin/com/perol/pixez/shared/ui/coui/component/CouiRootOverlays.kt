package com.perol.pixez.shared.ui.coui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.couiNavigationFeedback
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.components.MarkdownTextColors
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.core.CouiBarItem
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import kotlinx.coroutines.delay

/** 将 [CouiTheme.colorScheme] 适配为 [MarkdownTextColors]，避免 COUI 页面回退读取 MIUIX 主题色。 */
@Composable
internal fun couiMarkdownColors(): MarkdownTextColors {
    val scheme = CouiTheme.colorScheme
    return MarkdownTextColors(
        accent = scheme.accent,
        onSurface = scheme.onSurface,
        newBadgeBackground = scheme.accent.copy(alpha = 0.14f),
        newBadgeText = scheme.accent,
        optBadgeBackground = scheme.container,
        optBadgeText = scheme.accent,
        fixBadgeBackground = scheme.container,
        fixBadgeText = scheme.onSurface,
        divider = scheme.outlineVariant,
        useCapsuleBadge = true,
    )
}

/**
 * COUI 根级胶囊 Toast：纯 COUI 主题色板、全胶囊圆角与线性 [CouiIcons]，
 * 替代共享的 MIUIX Squircle 液态玻璃 Toast。
 */
@Composable
internal fun CouiToastOverlay(
    toast: ToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2000L,
) {
    var visible by remember { mutableStateOf(false) }
    val message = toast?.message
    LaunchedEffect(message, toast?.seq) {
        if (!message.isNullOrBlank()) {
            visible = true
            delay(durationMillis)
            visible = false
            onDismiss()
        } else {
            visible = false
        }
    }

    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val destructiveColor = CouiDialogDefaults.colors(dark).destructive
    val surfaceColor = if (dark) scheme.card else scheme.surface
    val borderColor = if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val effectiveType = toast?.type ?: ToastType.Normal

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(dampingRatio = 0.85f)) +
                scaleIn(spring(dampingRatio = 0.72f, stiffness = 420f), initialScale = 0.88f) +
                slideInVertically(spring(dampingRatio = 0.78f, stiffness = 420f), initialOffsetY = { it / 3 }),
            exit = fadeOut(spring(dampingRatio = 0.9f)) +
                scaleOut(spring(dampingRatio = 0.9f), targetScale = 0.92f) +
                slideOutVertically(spring(dampingRatio = 0.9f), targetOffsetY = { it / 4 }),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 60.dp)
                    .shadow(elevation = 10.dp, shape = CouiCapsuleShape, clip = false)
                    .clip(CouiCapsuleShape)
                    .background(surfaceColor)
                    .border(0.5.dp, borderColor, CouiCapsuleShape)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (effectiveType) {
                    ToastType.Success -> {
                        Image(
                            painter = rememberVectorPainter(CouiIcons.Ok),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(scheme.accent),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    ToastType.Error -> {
                        Image(
                            painter = rememberVectorPainter(CouiIcons.Report),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(destructiveColor),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    ToastType.Normal -> Unit
                }
                Text(
                    text = message.orEmpty(),
                    kind = CouiTextKind.ItemSummary,
                    color = if (effectiveType == ToastType.Error) destructiveColor else scheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** COUI 根级登录凭证失效弹窗，替代 MIUIX WindowDialog。 */
@Composable
internal fun CouiAuthExpiredDialog(
    show: Boolean,
    onRelogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogAuthExpiredTitle,
        message = strings.dialogAuthExpiredSummary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.btnRelogin,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = onRelogin,
        ),
        dismissAction = CouiDialogAction(
            text = strings.btnLater,
            onClick = onDismiss,
        ),
    ) {}
}

/** COUI 根级剪贴板 OAuth 二次确认弹窗，替代 MIUIX WindowDialog。 */
@Composable
internal fun CouiClipboardOAuthDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    val strings = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogClipboardOAuthTitle,
        message = strings.dialogClipboardOAuthSummary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.btnContinueLogin,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = onConfirm,
        ),
        dismissAction = CouiDialogAction(
            text = strings.cancel,
            onClick = onDismiss,
        ),
    ) {}
}

/**
 * COUI 宽屏侧边导航栏：平板/折叠屏非浮动底栏模式下使用，
 * 避免 COUI 皮肤在宽屏场景回退到 MIUIX NavigationRail。
 */
@Composable
internal fun CouiNavigationRail(
    items: List<CouiBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onSearchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val strings = LocalStrings.current
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)

    Row(modifier = modifier.fillMaxHeight()) {
        val railInsets = WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(scheme.surface)
                .windowInsetsPadding(railInsets)
                .consumeWindowInsets(railInsets)
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == safeIndex
                val interactionSource = remember { MutableInteractionSource() }
                val tint = if (selected) scheme.accent else scheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CouiCapsuleShape)
                        .background(if (selected) scheme.accent.copy(alpha = 0.14f) else Color.Transparent)
                        .couiNavigationFeedback(dark = dark, interactionSource = interactionSource)
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box {
                        Image(
                            painter = rememberVectorPainter(if (selected) item.selectedIcon else item.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(tint),
                            modifier = Modifier.size(22.dp),
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
                    BasicText(
                        text = item.title,
                        style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onSearchClick != null) {
                val interactionSource = remember { MutableInteractionSource() }
                val tint = scheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CouiCapsuleShape)
                        .couiNavigationFeedback(dark = dark, interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onSearchClick,
                        )
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painter = rememberVectorPainter(CouiIcons.Search),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                        modifier = Modifier.size(22.dp),
                    )
                    BasicText(
                        text = strings.tabSearch,
                        style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
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
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight()
                .background(scheme.outlineVariant),
        )
    }
}
