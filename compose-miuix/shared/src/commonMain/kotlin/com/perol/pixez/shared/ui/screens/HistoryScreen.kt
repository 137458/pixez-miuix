package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.FrostedTopAppBar

import androidx.compose.ui.graphics.Color
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.topAppBarBlur
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.LocalHistoryRepository
import com.perol.pixez.shared.data.repository.HistoryItem
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.components.ErrorPlaceholder
import com.perol.pixez.shared.ui.components.LoadingPlaceholder
import com.perol.pixez.shared.ui.components.PixivAsyncImage
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 浏览历史页：展示本地插画浏览历史网格，支持搜索、单条删除与清空全部。
 *
 * @param onBack 返回上一级页面。
 * @param onIllustClick 点击作品时传入作品 ID。
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    // 历史记录中的 illust_id 以 Long 保存，避免数据库大 ID 转 Int 溢出。
    onIllustClick: (Long) -> Unit,
) {
    // 通过 CompositionLocal 获取历史仓库，避免修改 RootContent 签名。
    val repository = LocalHistoryRepository.current
    val coroutineScope = rememberCoroutineScope()

    // 搜索关键词；使用 rememberSaveable 在配置变更后保留。
    var query by rememberSaveable { mutableStateOf("") }
    // refreshToken 在删除/清空后自增，触发 produceState 重新加载。
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    // 是否显示清空全部确认栏。
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    // 待删除记录的主键，为 null 时不显示删除确认栏。
    var itemToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    // Toast 提示文本。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    // 删除/清空操作进行中标志，用于禁用确认栏的确定按钮，防止重复提交。
    var isProcessing by remember { mutableStateOf(false) }

    // 异步加载全部历史记录；以 refreshToken 作为 key 实现刷新。
    // 数据库查询切到 IO 调度器，避免阻塞主线程。
    val historyResult = produceState<Result<List<HistoryItem>>?>(
        initialValue = null,
        repository,
        refreshToken,
    ) {
        value = suspendRunCatchingNonCancel {
            withContext(Dispatchers.Default) { repository.getAll() }
        }
    }

    // 使用 derivedStateOf 缓存过滤结果：搜索输入频繁变化时不会重复计算完整列表，
    // 仅当 query 或历史列表实际变化时才重新过滤。
    val filteredItems by remember {
        derivedStateOf {
            val allItems = historyResult.value?.getOrNull().orEmpty()
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                allItems
            } else {
                allItems.filter { item ->
                    item.illustId.toString().contains(trimmed) ||
                        item.title?.contains(trimmed, ignoreCase = true) == true
                }
            }
        }
    }

    // 是否有可清空的历史记录，用于控制右上角按钮可用状态。
    val hasHistory = historyResult.value?.getOrNull().orEmpty().isNotEmpty()

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FrostedTopAppBar(
                title = strings.settingHistory,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        enabled = hasHistory,
                    ) {
                        Text(
                            text = strings.actionClear,
                            style = MiuixTheme.textStyles.body1,
                            color = if (hasHistory) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部搜索框：按作品 ID 或标题过滤本地历史。
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = strings.tabSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = MiuixIcons.Close,
                                    contentDescription = strings.actionClear,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                )

                // 根据加载状态展示不同内容。
                Box(modifier = Modifier.weight(1f)) {
                    when (val result = historyResult.value) {
                        null -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                        else -> if (result.isSuccess) {
                            if (filteredItems.isEmpty()) {
                                EmptyPlaceholder(
                                    message = if (query.isBlank()) {
                                        strings.historyEmpty
                                    } else {
                                        strings.historyNoMatch
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                HistoryGrid(
                                    items = filteredItems,
                                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                                    onIllustClick = onIllustClick,
                                    onLongClick = { itemToDelete = it.id },
                                )
                            }
                        } else {
                            ErrorPlaceholder(
                                error = result.exceptionOrNull(),
                                onRetry = { refreshToken++ },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // 底部清空全部确认栏。
            if (showClearConfirm) {
                ConfirmBar(
                    message = strings.historyClearConfirm,
                    confirmEnabled = !isProcessing,
                    onConfirm = {
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                suspendRunCatchingNonCancel {
                                    // 数据库清空是同步写操作，切到后台调度器避免阻塞主线程。
                                    withContext(Dispatchers.Default) { repository.clearAll() }
                                }
                                    .onSuccess { refreshToken++ }
                                    .onFailure { toastMessage = "${strings.actionClear}${strings.loadFailed}: ${it.message}" }
                            } finally {
                                // 协程取消或异常时也必须重置状态，避免确认栏/按钮永久禁用。
                                isProcessing = false
                                showClearConfirm = false
                            }
                        }
                    },
                    onCancel = {
                        if (!isProcessing) showClearConfirm = false
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // 底部单条删除确认栏。
            val deleteId = itemToDelete
            if (deleteId != null) {
                ConfirmBar(
                    message = strings.historyDeleteConfirm,
                    confirmEnabled = !isProcessing,
                    onConfirm = {
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                suspendRunCatchingNonCancel {
                                    // 数据库删除是同步写操作，切到后台调度器避免阻塞主线程。
                                    withContext(Dispatchers.Default) { repository.deleteById(deleteId) }
                                }
                                    .onSuccess { refreshToken++ }
                                    .onFailure { toastMessage = "${strings.btnDelete}${strings.loadFailed}: ${it.message}" }
                            } finally {
                                // 协程取消或异常时也必须重置状态，避免确认栏/按钮永久禁用。
                                isProcessing = false
                                itemToDelete = null
                            }
                        }
                    },
                    onCancel = {
                        if (!isProcessing) itemToDelete = null
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
            )
        }
    }
}

/**
 * 历史记录瀑布流网格，复用 LazyVerticalStaggeredGrid 保持与首页一致的网格体验。
 */
@Composable
private fun HistoryGrid(
    items: List<HistoryItem>,
    onIllustClick: (Long) -> Unit,
    onLongClick: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(
            items = items,
            // 使用自增主键作为唯一 key；同一作品可多次浏览，illustId 会重复。
            key = { it.id },
            contentType = { "history_item" },
        ) { item ->
            HistoryCard(
                item = item,
                onClick = { onIllustClick(item.illustId) },
                onLongClick = { onLongClick(item) },
            )
        }
    }
}

/**
 * 历史记录卡片：展示封面缩略图、标题与画师名；长按触发删除确认。
 *
 * 由于本地历史表不保存作品宽高，使用固定的 3:4 竖图比例，兼顾大多数插画。
 */
@Composable
private fun HistoryCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PixivAsyncImage(
                model = item.pictureUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = item.title ?: strings.historyNoTitle,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
        )
        Text(
            text = item.userName ?: "",
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
    }
}

/**
 * 底部确认栏：用于清空全部与单条删除的二次确认，避免引入 AlertDialog。
 */
@Composable
private fun ConfirmBar(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    // 确定按钮是否可用；执行耗时操作时应设为 false，防止重复提交。
    confirmEnabled: Boolean = true,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
        )
        TextButton(
            text = strings.cancel,
            onClick = onCancel,
        )
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
        ) {
            Text(strings.confirm)
        }
    }
}
