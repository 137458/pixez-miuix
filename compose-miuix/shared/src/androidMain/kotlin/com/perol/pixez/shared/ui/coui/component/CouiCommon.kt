package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.annotation.RequiresApi
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.dialog.CouiDialogActionColors
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.CouiDialogMaterial
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.CouiMenuAnchor
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.rememberIllustQuickActions
import com.perol.pixez.shared.ui.components.resolveIllustCoverUrl
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.state.PagingFooterPhase
import com.perol.pixez.shared.ui.state.PagingFooterState
import com.perol.pixez.shared.ui.state.shouldPreloadMore

/**
 * COUI 一级页面骨架：折叠大标题顶栏 + 动作槽，正文使用 COUI 页面底色。
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                colors = colors,
            )
        },
    ) { padding ->
        content(padding)
    }
}

/**
 * COUI 占位区的错误文案：统一走 [PixivApiErrorFormatter] 本地化，
 * 避免 `Throwable.message` 这类原始异常文本直透 UI（与 MIUIX 占位组件同口径）。
 */
@Composable
internal fun couiErrorMessage(error: Throwable?): String {
    val strings = LocalStrings.current
    return error?.let { PixivApiErrorFormatter.format(it, strings) } ?: strings.retry
}

/** 顶栏动作图标：40dp 圆形触区，不依赖 COUI 原厂按钮的 API 34 窗口特性。 */
@Composable
internal fun CouiActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(CouiTheme.colorScheme.onSurface),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** COUI 插画卡片：封面 + 标题/作者，支持点击进入详情与长按操作菜单。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CouiIllustCard(
    illust: Illust,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val hapticFeedback = LocalHapticFeedback.current
    val settings = LocalSettingsRepository.current
    val changeVersion = settings.changeVersion.collectAsState().value
    val previewUrl = remember(illust, settings.feedPreviewQuality, changeVersion) {
        resolveIllustCoverUrl(illust.imageUrls, settings.feedPreviewQuality)
    }
    val isAI = remember(illust.illustAIType) { illust.illustAIType == 2 }
    val showAIBadge = remember(isAI, settings.feedAIBadge) { settings.feedAIBadge && isAI }
    val isNsfw = remember(illust, settings.nsfwMask, changeVersion) {
        settings.nsfwMask && illust.isR18()
    }
    val ratio = remember(illust.width, illust.height) {
        if (illust.width > 0 && illust.height > 0) {
            (illust.width.toFloat() / illust.height.toFloat()).coerceIn(0.5f, 2.0f)
        } else {
            1.0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                customActions = listOf(
                    CustomAccessibilityAction(label = strings.menuMoreActions) {
                        onLongClick()
                        true
                    },
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio),
        ) {
            if (isNsfw) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CouiTheme.colorScheme.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "R-18",
                        kind = CouiTextKind.ItemTitle,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                PixivAsyncImage(
                    model = previewUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (!isNsfw && illust.pageCount > 1) {
                CouiCardBadge(
                    text = "${illust.pageCount}P",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
            if (!isNsfw && showAIBadge) {
                CouiCardBadge(
                    text = "AI",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = illust.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = illust.user.name,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CouiCardBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, kind = CouiTextKind.ItemSummary, color = Color.White, maxLines = 1)
    }
}

/** COUI 瀑布流：自适应列宽、触底预加载与三态 footer。 */
@Composable
internal fun CouiIllustGrid(
    illusts: List<Illust>,
    state: LazyStaggeredGridState,
    onIllustClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: Throwable? = null,
    onLoadMore: (() -> Unit)? = null,
) {
    if (onLoadMore != null) {
        val shouldLoadMore by remember(hasMore, isLoadingMore, loadMoreError, illusts, state) {
            derivedStateOf {
                shouldPreloadMore(
                    itemCount = illusts.size,
                    hasMore = hasMore,
                    isLoadingMore = isLoadingMore,
                    hasLoadMoreError = loadMoreError != null,
                    lastVisibleItemIndex = state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1,
                    thresholdItems = AppConstants.Layout.PRELOAD_THRESHOLD_STAGGERED_ITEMS,
                )
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) onLoadMore()
        }
    }

    // 长按菜单用官方锚点菜单贴着被按卡片弹出：入栈到条目自身，随卡片回收一起销毁。
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        items(items = illusts, key = { it.id }, contentType = { "coui_illust" }) { illust ->
            val menuAnchor = rememberCouiMenuAnchor()
            var menuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.couiMenuAnchor(menuAnchor)) {
                CouiIllustCard(
                    illust = illust,
                    onClick = { onIllustClick(illust.id) },
                    onLongClick = { menuAnchor.open { menuExpanded = true } },
                )
                CouiIllustActionMenu(
                    illust = illust,
                    expanded = menuExpanded,
                    anchor = menuAnchor,
                    onDismiss = { menuExpanded = false },
                )
            }
        }
        if (onLoadMore != null && (hasMore || isLoadingMore || loadMoreError != null)) {
            item(
                key = "coui_load_more_footer",
                span = StaggeredGridItemSpan.FullLine,
                contentType = "coui_load_more_footer",
            ) {
                CouiLoadMoreFooter(
                    isLoading = isLoadingMore,
                    error = loadMoreError,
                    onRetry = onLoadMore,
                )
            }
        }
    }
}

/**
 * COUI 插画长按菜单：官方锚点菜单贴着卡片弹出，动作编排复用
 * [rememberIllustQuickActions]，与 MIUIX 卡片保持同一套行为与反馈。
 */
@Composable
private fun CouiIllustActionMenu(
    illust: Illust,
    expanded: Boolean,
    anchor: CouiMenuAnchor,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val actions = rememberIllustQuickActions(illust, onDismiss)
    Menu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        anchor = anchor,
        headerText = strings.menuMoreActions,
        items = listOf(
            CouiMenuItem(value = "download", text = strings.download),
            CouiMenuItem(value = "copy_info", text = strings.menuCopyInfo),
            CouiMenuItem(value = "copy_image", text = strings.menuCopyImage),
            CouiMenuItem(value = "copy_link", text = strings.menuCopyLink),
            CouiMenuItem(value = "share", text = strings.menuShareLink),
            CouiMenuItem(value = "ban", text = strings.menuBanWork, dividerBefore = true),
        ),
        onItemClick = { item ->
            onDismiss()
            when (item.value) {
                "download" -> actions.onDownload()
                "copy_info" -> actions.onCopyInfo()
                "copy_image" -> actions.onCopyImage()
                "copy_link" -> actions.onCopyLink()
                "share" -> actions.onShareLink()
                "ban" -> actions.onBan()
            }
        },
    )
}

/** COUI 弹窗材质：API 33+ 且窗口具备背景采样时启用玻璃，否则退回不透明。 */
@Composable
internal fun couiDialogMaterialOrNull(): CouiDialogMaterial? =
    if (Build.VERSION.SDK_INT >= 33 && rememberCouiWindowBackdrop() != null) {
        CouiDialogDefaults.material(CouiTheme.isDark)
    } else {
        null
    }

/** COUI 弹窗排布：宽屏居中，手机底部弹出。 */
@Composable
internal fun couiDialogPlacement(): CouiDialogPlacement =
    if (LocalConfiguration.current.screenWidthDp >= 600) {
        CouiDialogPlacement.Center
    } else {
        CouiDialogPlacement.Bottom
    }

/** 主操作配色：破坏性操作使用库内 destructive 色。 */
@Composable
internal fun couiPrimaryActionColors(danger: Boolean = false): CouiDialogActionColors {
    val accent = if (danger) {
        CouiDialogDefaults.colors(CouiTheme.isDark).destructive
    } else {
        CouiTheme.colorScheme.accent
    }
    return CouiDialogDefaults.primaryActionColors(accent)
}

@Composable
private fun CouiLoadMoreFooter(
    isLoading: Boolean,
    error: Throwable?,
    onRetry: () -> Unit,
) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> Text(
                text = strings.loading,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            error != null -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = strings.retry,
                    kind = CouiTextKind.ItemTitle,
                    color = CouiTheme.colorScheme.accent,
                )
            }
        }
    }
}

/** COUI 首屏加载/错误/空态：单卡片提示，保留重试入口。 */
@Composable
internal fun CouiStatePlaceholder(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CouiTheme.colorScheme.container)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = message,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
            )
            if (actionText != null && onAction != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = actionText,
                        kind = CouiTextKind.ItemTitle,
                        color = CouiTheme.colorScheme.accent,
                    )
                }
            }
        }
    }
}

/** COUI 可滚动筛选胶囊的数据项。 */
internal data class CouiFilterChip(
    val key: String,
    val label: String,
)

/** COUI 库 ChipTabRow 的系统门槛。 */
private const val COUI_CHIP_TAB_MIN_SDK = 34

/**
 * COUI 筛选胶囊栏：API 34+ 使用原厂 ChipTabRow（滚动胶囊 + 凝光材质），
 * 低版本按能力边界清单退回同版式自绘胶囊栏。
 */
@Composable
internal fun CouiFilterChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= COUI_CHIP_TAB_MIN_SDK) {
        CouiLibraryChipBar(chips, selectedKey, onSelect, modifier)
    } else {
        CouiPlainChipBar(chips, selectedKey, onSelect, modifier)
    }
}

@RequiresApi(COUI_CHIP_TAB_MIN_SDK)
@Composable
private fun CouiLibraryChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val items = remember(chips) {
        chips.map { chip -> CouiChipTabItem(key = chip.key, label = chip.label) }
    }
    ChipTabRow(
        modifier = modifier.fillMaxWidth(),
        items = items,
        selectedKey = selectedKey,
        onSelectedKeyChange = { key -> (key as? String)?.let(onSelect) },
        backdrop = rememberCouiBackdrop(),
    )
}

/** 低版本平替：与 ChipTabRow 同版式的横向滚动胶囊栏。 */
@Composable
private fun CouiPlainChipBar(
    chips: List<CouiFilterChip>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val scheme = CouiTheme.colorScheme
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips, key = { it.key }) { chip ->
            val selected = chip.key == selectedKey
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) scheme.accent else scheme.container)
                    .clickable { onSelect(chip.key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chip.label,
                    kind = CouiTextKind.ItemSummary,
                    color = if (selected) scheme.onAccent else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * COUI 卡片容器：卡片底色 + 连续圆角裁切，列表分组与结果行共用。
 */
@Composable
internal fun CouiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card),
        content = content,
    )
}

/**
 * COUI 辅助胶囊：可独立选中，用于互不排斥的快捷筛选（排序、AI 过滤、筛选入口）。
 * 与 [CouiFilterChipBar] 的区别是不做互斥选择，选中态只由调用方给定。
 */
@Composable
internal fun CouiAssistChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.accent else scheme.container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemSummary,
            color = if (selected) scheme.onAccent else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** COUI 翻页 footer：与 [PagingFooterState] 三态对齐的纯文本实现。 */
@Composable
internal fun CouiPagingFooter(
    state: PagingFooterState,
    onRetry: () -> Unit,
) {
    val strings = LocalStrings.current
    val phase = state.phase
    if (phase == PagingFooterPhase.Hidden) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            PagingFooterPhase.Loading -> Text(
                text = strings.loadingMore,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            PagingFooterPhase.Retry -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = strings.retry,
                    kind = CouiTextKind.ItemTitle,
                    color = CouiTheme.colorScheme.accent,
                )
            }
            PagingFooterPhase.NoMore -> Text(
                text = strings.noMoreData,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            PagingFooterPhase.Hidden -> Unit
        }
    }
}

/** COUI Spotlight 文章卡片：16:9 封面 + 标题/副标题。 */
@Composable
internal fun CouiSpotlightCard(
    article: SpotlightArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
            .clickable(onClick = onClick),
    ) {
        PixivAsyncImage(
            model = article.thumbnail,
            contentDescription = article.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = article.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            if (article.pureTitle.isNotBlank() && article.pureTitle != article.title) {
                Text(
                    text = article.pureTitle,
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
