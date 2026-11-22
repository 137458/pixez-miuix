package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.repository.HistoryItem
import com.perol.pixez.shared.data.repository.NovelHistoryItem
import com.perol.pixez.shared.ui.LocalHistoryRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.actionFailureToast
import com.perol.pixez.shared.ui.components.rememberIllustGridColumns
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.component.CouiTopBarSegmentedTabs
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.core.CouiErrorPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiLoadingPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiPlaceholder
import com.perol.pixez.shared.ui.coui.core.CouiSecondaryScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.coui.icon.CouiIcons
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.forActiveHistory
import com.perol.pixez.shared.ui.screens.formatNovelHistoryTime
import com.perol.pixez.shared.ui.utils.filterHistoryByQuery
import com.perol.pixez.shared.ui.utils.rememberHistoryResult
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch

/**
 * COUI 浏览历史页：折叠顶栏 + 搜索 + 插画/小说页签。
 *
 * 业务编排不变：两页签各自加载本地历史，搜索按 ID/标题/作者本地过滤，
 * 单条删除与清空各走一次 COUI 确认弹窗。表现层统一交给 [CouiSecondaryScaffold]：
 * 顶栏、护栏与底栏留白都由骨架代付，搜索框与页签作为滚动容器的首项，
 * 正文因此能滚进顶栏的模糊覆盖区。
 */
@Composable
internal fun CouiHistoryScreen(
    onBack: () -> Unit,
    // 历史记录中的 illust_id 以 Long 保存，避免数据库大 ID 转 Int 溢出。
    onIllustClick: (Long) -> Unit,
    onNovelClick: (Long) -> Unit,
) {
    val repository = LocalHistoryRepository.current
    val novelRepository = LocalNovelHistoryRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    // 历史类型：0 = 插画，1 = 小说。
    var selectedType by rememberSaveable { mutableIntStateOf(0) }
    // 删除/清空后自增，触发两页签重新加载。
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    // 待删除记录的主键，为 null 时不显示删除确认。
    var itemToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    // 进行中标志用 remember：进程恢复后协程不会恢复，避免弹窗按钮被永久禁用。
    var isProcessing by remember { mutableStateOf(false) }

    val historyResult = rememberHistoryResult(repository, refreshToken) {
        repository.getAll()
    }
    val novelHistoryResult = rememberHistoryResult(novelRepository, refreshToken) {
        novelRepository.getAllDesc()
    }

    val filteredItems by remember(historyResult, query) {
        derivedStateOf {
            filterHistoryByQuery(
                items = historyResult.value?.getOrNull().orEmpty(),
                query = query,
            ) { item, keyword ->
                item.illustId.toString().contains(keyword) ||
                    item.title?.contains(keyword, ignoreCase = true) == true
            }
        }
    }
    val filteredNovelItems by remember(novelHistoryResult, query) {
        derivedStateOf {
            filterHistoryByQuery(
                items = novelHistoryResult.value?.getOrNull().orEmpty(),
                query = query,
            ) { item, keyword ->
                item.novelId.toString().contains(keyword) ||
                    item.title.contains(keyword, ignoreCase = true) ||
                    item.userName.contains(keyword, ignoreCase = true)
            }
        }
    }

    val hasHistory = (if (selectedType == 0) historyResult.value else novelHistoryResult.value)
        ?.getOrNull().orEmpty().isNotEmpty()
    val emptyMessage = if (query.isBlank()) strings.historyEmpty else strings.historyNoMatch
    val typeLabels = remember(strings) {
        listOf(strings.historyTypeIllust, strings.historyTypeNovel)
    }
    // 搜索框作为列表首项；插画/小说分段页签吸顶在顶栏下方并采样正文毛玻璃。
    val header: @Composable () -> Unit = {
        CouiHistorySearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = strings.tabSearch,
            clearDescription = strings.actionClear,
        )
    }

    CouiSecondaryScaffold(
        title = strings.settingHistory,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = { showClearConfirm = true },
                icon = CouiIcons.Delete,
                contentDescription = strings.actionClear,
                enabled = hasHistory,
            )
        },
        topBarBottom = {
            CouiTopBarSegmentedTabs(
                items = typeLabels,
                selectedIndex = selectedType,
                onSelect = { selectedType = it },
            )
        },
    ) { padding ->
        if (selectedType == 0) {
            CouiHistoryIllustTab(
                padding = padding,
                result = historyResult.value,
                items = filteredItems,
                emptyMessage = emptyMessage,
                header = header,
                onRetry = { refreshToken++ },
                onIllustClick = onIllustClick,
                onDelete = { itemToDelete = it.id },
            )
        } else {
            CouiHistoryNovelTab(
                padding = padding,
                result = novelHistoryResult.value,
                items = filteredNovelItems,
                emptyMessage = emptyMessage,
                header = header,
                onRetry = { refreshToken++ },
                onNovelClick = onNovelClick,
                onDelete = { itemToDelete = it.id },
            )
        }
    }

    if (showClearConfirm) {
        val clearing = isProcessing
        Dialog(
            onDismissRequest = { if (!clearing) showClearConfirm = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.actionClear,
            message = strings.historyClearConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                enabled = !clearing,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    isProcessing = true
                    coroutineScope.launch {
                        try {
                            suspendRunCatchingNonCancel {
                                forActiveHistory(
                                    selectedType = selectedType,
                                    onIllust = { repository.clearAll() },
                                    onNovel = { novelRepository.clearAll() },
                                )
                            }
                                .onSuccess { refreshToken++ }
                                .onFailure {
                                    toastSink(actionFailureToast(strings.actionClear, it, strings))
                                }
                        } finally {
                            isProcessing = false
                            showClearConfirm = false
                        }
                    }
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { if (!isProcessing) showClearConfirm = false },
            ),
        )
    }

    val deleteId = itemToDelete
    if (deleteId != null) {
        val deleting = isProcessing
        Dialog(
            onDismissRequest = { if (!deleting) itemToDelete = null },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.btnDelete,
            message = strings.historyDeleteConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                enabled = !deleting,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    isProcessing = true
                    coroutineScope.launch {
                        try {
                            suspendRunCatchingNonCancel {
                                forActiveHistory(
                                    selectedType = selectedType,
                                    onIllust = { repository.deleteById(deleteId) },
                                    onNovel = { novelRepository.deleteById(deleteId) },
                                )
                            }
                                .onSuccess { refreshToken++ }
                                .onFailure {
                                    toastSink(actionFailureToast(strings.btnDelete, it, strings))
                                }
                        } finally {
                            isProcessing = false
                            itemToDelete = null
                        }
                    }
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { if (!isProcessing) itemToDelete = null },
            ),
        )
    }
}

/** 搜索输入：统一走 CouiTextInput，搜索图标与清空按钮走前后插槽。 */
@Composable
private fun CouiHistorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
) {
    val scheme = CouiTheme.colorScheme
    // 清空按钮只在有输入时出现；自带 32dp 命中区，交给 CouiTextInput 的尾部插槽。
    val trailingSlot: (@Composable () -> Unit)? = if (query.isEmpty()) {
        null
    } else {
        {
            val pressInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .couiPressFeedback(pressInteraction)
                    .clickable(
                        interactionSource = pressInteraction,
                        indication = null,
                    ) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = rememberVectorPainter(CouiIcons.Close),
                    contentDescription = clearDescription,
                    colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
    CouiTextInput(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        // 搜索框保持胶囊观感：50% 圆角 + 14dp 横向内边距。
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 14.dp),
        leading = {
            Image(
                painter = rememberVectorPainter(CouiIcons.Search),
                contentDescription = null,
                colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                modifier = Modifier.size(18.dp),
            )
        },
        trailing = trailingSlot,
    )
}

/** 三态占位的高度：列表首项之后留出可见块，不把占位压成一条线。 */
private val StatePlaceholderMinHeight = 320.dp

/** 插画历史页签：瀑布流列表，搜索框与页签是整行首项。 */
@Composable
private fun CouiHistoryIllustTab(
    padding: PaddingValues,
    result: Result<List<HistoryItem>>?,
    items: List<HistoryItem>,
    emptyMessage: String,
    header: @Composable () -> Unit,
    onRetry: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onDelete: (HistoryItem) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(padding),
    ) {
        val columns = rememberIllustGridColumns(isLandscape = maxWidth > maxHeight)
        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = 8.dp,
                bottom = padding.calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            overscrollEffect = rememberCouiOverscrollEffect(),
        ) {
        item(key = "coui_history_header", contentType = "header", span = StaggeredGridItemSpan.FullLine) {
            header()
        }
        when {
            result == null -> item(
                key = "coui_history_state",
                contentType = "state",
                span = StaggeredGridItemSpan.FullLine,
            ) {
                CouiLoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = StatePlaceholderMinHeight),
                )
            }

            !result.isSuccess -> item(
                key = "coui_history_state",
                contentType = "state",
                span = StaggeredGridItemSpan.FullLine,
            ) {
                CouiErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = StatePlaceholderMinHeight),
                )
            }

            items.isEmpty() -> item(
                key = "coui_history_state",
                contentType = "state",
                span = StaggeredGridItemSpan.FullLine,
            ) {
                CouiPlaceholder(
                    message = emptyMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = StatePlaceholderMinHeight),
                )
            }

            else -> items(
                items = items,
                // 使用自增主键作为唯一 key；同一作品可多次浏览，illustId 会重复。
                key = { it.id },
                contentType = { "coui_history_illust" },
            ) { item ->
                CouiHistoryIllustCard(
                    item = item,
                    onClick = { onIllustClick(item.illustId) },
                    onLongClick = { onDelete(item) },
                )
            }
        }
        }
    }
}

/** 插画历史卡：3:4 竖图 + 标题/作者，长按触发删除确认。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CouiHistoryIllustCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val hapticFeedback = LocalHapticFeedback.current
    val pressInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
            .couiPressFeedback(pressInteraction)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                customActions = listOf(
                    CustomAccessibilityAction(label = strings.btnDelete) {
                        onLongClick()
                        true
                    },
                )
            }
            .combinedClickable(
                interactionSource = pressInteraction,
                indication = null,
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
                .aspectRatio(3f / 4f),
        ) {
            PixivAsyncImage(
                model = item.pictureUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = item.title ?: strings.historyNoTitle,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.userName.orEmpty(),
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** 小说历史页签：单列卡片列表，搜索框与页签是首项。 */
@Composable
private fun CouiHistoryNovelTab(
    padding: PaddingValues,
    result: Result<List<NovelHistoryItem>>?,
    items: List<NovelHistoryItem>,
    emptyMessage: String,
    header: @Composable () -> Unit,
    onRetry: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onDelete: (NovelHistoryItem) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(padding),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = padding.calculateTopPadding() + 8.dp,
            end = 12.dp,
            bottom = padding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        item(key = "coui_history_header", contentType = "header") {
            header()
        }
        when {
            result == null -> item(key = "coui_history_state", contentType = "state") {
                CouiLoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = StatePlaceholderMinHeight),
                )
            }

            !result.isSuccess -> item(key = "coui_history_state", contentType = "state") {
                CouiErrorPlaceholder(
                    error = result.exceptionOrNull(),
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = StatePlaceholderMinHeight),
                )
            }

            items.isEmpty() -> item(key = "coui_history_state", contentType = "state") {
                CouiPlaceholder(
                    message = emptyMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = StatePlaceholderMinHeight),
                )
            }

            else -> items(
                count = items.size,
                // id 为导入时的 null 会按旧语义折叠为 0L 单行，故 ?: 0L 不会产生重复 key。
                key = { items[it].id ?: 0L },
                contentType = { "coui_history_novel" },
            ) { index ->
                val item = items[index]
                CouiHistoryNovelRow(
                    item = item,
                    onClick = { onNovelClick(item.novelId) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

/** 小说历史行：方形封面 + 标题/作者/浏览时间 + 删除按钮。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CouiHistoryNovelRow(
    item: NovelHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    val hapticFeedback = LocalHapticFeedback.current
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
            .couiPressFeedback(pressInteraction)
            .combinedClickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixivAsyncImage(
            model = item.pictureUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.userName,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = formatNovelHistoryTime(item.time),
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        CouiActionButton(
            icon = CouiIcons.Delete,
            contentDescription = strings.btnDelete,
            onClick = onDelete,
        )
    }
}
