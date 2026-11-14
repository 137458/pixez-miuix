package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
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
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiErrorMessage
import com.perol.pixez.shared.ui.coui.component.CouiFilterChip
import com.perol.pixez.shared.ui.coui.component.CouiFilterChipBar
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiStatePlaceholder
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalHistoryRepository
import com.perol.pixez.shared.ui.LocalNovelHistoryRepository
import com.perol.pixez.shared.ui.screens.filterHistoryByQuery
import com.perol.pixez.shared.ui.screens.forActiveHistory
import com.perol.pixez.shared.ui.screens.formatNovelHistoryTime
import com.perol.pixez.shared.ui.screens.rememberHistoryResult
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.MiuixIcons

/**
 * COUI 浏览历史页：固定顶栏 + 搜索 + 插画/小说页签。
 *
 * 业务编排与 MIUIX 版一致：两页签各自加载本地历史，搜索按 ID/标题/作者本地过滤，
 * 单条删除与清空全部各走一次 COUI 确认弹窗。皮肤差异：
 * 搜索框与页签收敛到平板可读宽度，小说列表不再带 MIUIX 的纵向滚动条，
 * 删除确认使用 COUI Dialog（宽屏居中、手机底部）。
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
    val typeChips = remember(strings) {
        listOf(
            CouiFilterChip(key = "illust", label = strings.historyTypeIllust),
            CouiFilterChip(key = "novel", label = strings.historyTypeNovel),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingHistory,
            onBack = onBack,
        ) {
            CouiTextAction(
                text = strings.actionClear,
                enabled = hasHistory,
                color = if (hasHistory) CouiTheme.colorScheme.accent else null,
                onClick = { showClearConfirm = true },
            )
        }

        // 搜索框与页签收敛到平板可读宽度，与 MIUIX 版一致。
        CouiReadableColumn(modifier = Modifier.fillMaxWidth()) {
            CouiHistorySearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = strings.tabSearch,
                clearDescription = strings.actionClear,
            )
            CouiFilterChipBar(
                chips = typeChips,
                selectedKey = if (selectedType == 0) "illust" else "novel",
                onSelect = { key -> selectedType = if (key == "illust") 0 else 1 },
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedType == 0) {
                CouiHistoryIllustTab(
                    result = historyResult.value,
                    items = filteredItems,
                    emptyMessage = emptyMessage,
                    onRetry = { refreshToken++ },
                    onIllustClick = onIllustClick,
                    onDelete = { itemToDelete = it.id },
                )
            } else {
                CouiHistoryNovelTab(
                    result = novelHistoryResult.value,
                    items = filteredNovelItems,
                    emptyMessage = emptyMessage,
                    onRetry = { refreshToken++ },
                    onNovelClick = onNovelClick,
                    onDelete = { itemToDelete = it.id },
                )
            }
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
                                    toastSink(
                                        ToastData(
                                            "${strings.actionClear}${strings.failedSuffix}: " +
                                                PixivApiErrorFormatter.format(it, strings),
                                            ToastType.Error,
                                        )
                                    )
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
                                    toastSink(
                                        ToastData(
                                            "${strings.btnDelete}${strings.failedSuffix}: " +
                                                PixivApiErrorFormatter.format(it, strings),
                                            ToastType.Error,
                                        )
                                    )
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

/** 搜索输入：COUI 无文本输入原语，用 `BasicTextField` 套官方容器样式自绘胶囊。 */
@Composable
private fun CouiHistorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
) {
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.container)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = rememberVectorPainter(MiuixIcons.Search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(scheme.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
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
                    painter = rememberVectorPainter(MiuixIcons.Close),
                    contentDescription = clearDescription,
                    colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CouiHistoryIllustTab(
    result: Result<List<HistoryItem>>?,
    items: List<HistoryItem>,
    emptyMessage: String,
    onRetry: () -> Unit,
    onIllustClick: (Long) -> Unit,
    onDelete: (HistoryItem) -> Unit,
) {
    when {
        result == null -> CouiHistoryReadableState(LocalStrings.current.loading)
        !result.isSuccess -> CouiHistoryReadableState(
            message = couiErrorMessage(result.exceptionOrNull()),
            actionText = LocalStrings.current.retry,
            onAction = onRetry,
        )
        items.isEmpty() -> CouiHistoryReadableState(emptyMessage)
        else -> CouiHistoryIllustGrid(
            items = items,
            onIllustClick = onIllustClick,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun CouiHistoryNovelTab(
    result: Result<List<NovelHistoryItem>>?,
    items: List<NovelHistoryItem>,
    emptyMessage: String,
    onRetry: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onDelete: (NovelHistoryItem) -> Unit,
) {
    when {
        result == null -> CouiHistoryReadableState(LocalStrings.current.loading)
        !result.isSuccess -> CouiHistoryReadableState(
            message = couiErrorMessage(result.exceptionOrNull()),
            actionText = LocalStrings.current.retry,
            onAction = onRetry,
        )
        items.isEmpty() -> CouiHistoryReadableState(emptyMessage)
        else -> CouiHistoryNovelList(
            items = items,
            onNovelClick = onNovelClick,
            onDelete = onDelete,
        )
    }
}

/** 加载/错误/空态：统一收敛到平板可读宽度。 */
@Composable
private fun CouiHistoryReadableState(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
        CouiStatePlaceholder(
            message = message,
            actionText = actionText,
            onAction = onAction,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 插画历史瀑布流：本地列表一次成型，无分页。 */
@Composable
private fun CouiHistoryIllustGrid(
    items: List<HistoryItem>,
    onIllustClick: (Long) -> Unit,
    onDelete: (HistoryItem) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = LocalBottomBarContentPadding.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        items(
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

/** 小说历史列表：卡片行 + 右侧删除按钮。 */
@Composable
private fun CouiHistoryNovelList(
    items: List<NovelHistoryItem>,
    onNovelClick: (Long) -> Unit,
    onDelete: (NovelHistoryItem) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 8.dp,
            end = 12.dp,
            bottom = LocalBottomBarContentPadding.current,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        overscrollEffect = rememberCouiOverscrollEffect(),
    ) {
        // id 为导入时的 null 会按旧语义折叠为 0L 单行，故 ?: 0L 不会产生重复 key。
        items(
            count = items.size,
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
            icon = MiuixIcons.Delete,
            contentDescription = strings.btnDelete,
            onClick = onDelete,
        )
    }
}
