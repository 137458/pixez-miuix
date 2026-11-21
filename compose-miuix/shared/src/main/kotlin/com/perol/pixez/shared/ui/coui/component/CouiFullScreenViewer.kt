package com.perol.pixez.shared.ui.coui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ViewerTopOverlayState
import com.perol.pixez.shared.ui.components.buildViewerMoreMenuEntries
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.IllustDetailFullScreenOverlay
import com.perol.pixez.shared.ui.utils.accessibleTouchTarget

/**
 * COUI 全屏看图器覆盖层：复用共享缩放、多 P 翻页与相邻页预加载内核，
 * 顶栏控制按钮、页码胶囊与更多操作菜单全部替换为 COUI 原生组件与线性 [CouiIcons]，
 * 消除 MIUIX 液态玻璃按钮与 OverlayDropdownPopup 残留。
 */
@Composable
internal fun CouiIllustFullScreenOverlay(
    illust: Illust,
    pageIndex: Int,
    settings: SettingsRepository?,
    repository: IllustRepository,
    downloadRepository: DownloadRepository,
    onToast: (ToastData) -> Unit,
    onDismiss: () -> Unit,
) {
    IllustDetailFullScreenOverlay(
        illust = illust,
        pageIndex = pageIndex,
        settings = settings,
        repository = repository,
        downloadRepository = downloadRepository,
        detailBackdrop = null,
        onToast = onToast,
        onDismiss = onDismiss,
        topOverlay = { overlayState, modifier ->
            CouiViewerTopOverlay(state = overlayState, modifier = modifier)
        },
    )
}

@Composable
private fun CouiViewerTopOverlay(
    state: ViewerTopOverlayState,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val context = LocalPlatformContext.current
    val visible by state.visibleState
    val currentDisplayPage by state.currentPageState
    val menuAnchor = rememberCouiMenuAnchor()
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            CouiViewerCircleButton(
                icon = CouiIcons.Back,
                contentDescription = strings.back,
                onClick = state.onDismiss,
                modifier = Modifier.align(Alignment.CenterStart),
            )

            if (state.pageCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CouiCapsuleShape)
                        .background(Color.Black.copy(alpha = 0.56f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), CouiCapsuleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${currentDisplayPage + 1} / ${state.pageCount}",
                        kind = CouiTextKind.ItemSummary,
                        color = Color.White,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.pageCount > 1) {
                    val switchTooltip = if (state.isVerticalScrollMode) {
                        strings.viewerModeHorizontal
                    } else {
                        strings.viewerModeVertical
                    }
                    val switchIcon = if (state.isVerticalScrollMode) {
                        CouiIcons.HorizontalPages
                    } else {
                        CouiIcons.VerticalScroll
                    }
                    CouiViewerCircleButton(
                        icon = switchIcon,
                        contentDescription = switchTooltip,
                        onClick = state.onToggleScrollMode,
                    )
                }

                CouiViewerCircleButton(
                    icon = CouiIcons.Download,
                    contentDescription = strings.download,
                    onClick = state.onDownloadClick,
                )

                Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                    CouiViewerCircleButton(
                        icon = CouiIcons.More,
                        contentDescription = strings.menuMoreActions,
                        onClick = { menuAnchor.open { showMoreMenu = true } },
                    )
                    val entries = buildViewerMoreMenuEntries(
                        strings = strings,
                        illust = state.illust,
                        pageCount = state.pageCount,
                        currentDisplayPage = currentDisplayPage,
                        context = context,
                        coroutineScope = state.coroutineScope,
                        onToast = state.onToast,
                    )
                    val flatItems = entries.flatMap { it.items }
                    val menuItems = buildList {
                        var index = 0
                        entries.forEachIndexed { entryIndex, entry ->
                            entry.items.forEachIndexed { itemIndex, item ->
                                add(
                                    CouiMenuItem(
                                        value = index.toString(),
                                        text = item.text,
                                        dividerBefore = itemIndex == 0 && entryIndex > 0,
                                    ),
                                )
                                index += 1
                            }
                        }
                    }
                    Menu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        anchor = menuAnchor,
                        headerText = strings.menuMoreActions,
                        items = menuItems,
                        onItemClick = { clicked ->
                            showMoreMenu = false
                            flatItems.getOrNull(menuItems.indexOfFirst { it.value == clicked.value })
                                ?.onClick
                                ?.invoke()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiViewerCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .accessibleTouchTarget(44.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.56f))
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .couiButtonFeedback(dark = true, interactionSource = source, list = false)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(20.dp),
        )
    }
}
