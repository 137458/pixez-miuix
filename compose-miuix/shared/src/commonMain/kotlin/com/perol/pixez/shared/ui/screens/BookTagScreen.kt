package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.ui.components.BlurredBar
import com.perol.pixez.shared.ui.components.rememberBlurBackdrop

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.perol.pixez.shared.ui.components.LocalBackdrop
import com.perol.pixez.shared.ui.components.blurBackdropSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.DelayedClearEffect
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.perol.pixez.shared.ui.AppConstants
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 收藏标签管理页：展示用户收藏的标签列表，支持添加、删除与点击搜索。
 *
 * @param settingsRepository 设置仓库，用于读写 `book_tag_list`。
 * @param onBack 返回上一级页面。
 * @param onTagSearch 跳转搜索指定标签。
 */
@Composable
fun BookTagScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onTagSearch: (String) -> Unit,
) {
    // 当前标签列表，变更后同步写回设置。
    var bookTags by remember { mutableStateOf(settingsRepository.bookTagList) }
    val updateTags: (List<String>) -> Unit = { newTags ->
        bookTags = newTags
        settingsRepository.bookTagList = newTags
    }

    // 添加 / 删除对话框状态。
    var showAddDialog by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // 弹窗关闭后延迟清理引用，保证退场动画期间数据完整并不泄漏引用
    DelayedClearEffect(showDeleteDialog, tagToDelete) {
        tagToDelete = null
    }

    // 排序操作期间禁用按钮。
    var isReordering by remember { mutableStateOf(false) }

    // 空列表占位提示。
    val isEmpty = bookTags.isEmpty()
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = strings.settingBookTags,
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
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
                            onClick = { showAddDialog = true },
                            enabled = !isAdding && !isReordering,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = strings.btnAdd,
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
            item {
                SmallTitle(text = strings.tags)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (isEmpty) {
                        Text(
                            text = strings.bookTagsEmpty,
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    } else {
                        bookTags.forEachIndexed { index, tag ->
                            BasicComponent(
                                title = tag,
                                onClick = { onTagSearch(tag) },
                                endActions = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    isReordering = true
                                                    updateTags(
                                                        bookTags.toMutableList().apply {
                                                            add(index - 1, removeAt(index))
                                                        }
                                                    )
                                                    isReordering = false
                                                }
                                            },
                                            enabled = index > 0 && !isReordering,
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.ExpandLess,
                                                contentDescription = strings.bookTagsMoveUp,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < bookTags.lastIndex) {
                                                    isReordering = true
                                                    updateTags(
                                                        bookTags.toMutableList().apply {
                                                            add(index + 1, removeAt(index))
                                                        }
                                                    )
                                                    isReordering = false
                                                }
                                            },
                                            enabled = index < bookTags.lastIndex && !isReordering,
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.ExpandMore,
                                                contentDescription = strings.bookTagsMoveDown,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                tagToDelete = tag
                                                showDeleteDialog = true
                                            },
                                            enabled = !isReordering,
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Delete,
                                                contentDescription = strings.btnDelete,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

        AddTagDialog(
            show = showAddDialog,
            isLoading = isAdding,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                isAdding = true
                val trimmed = name.trim()
                if (trimmed.isNotBlank() && !bookTags.contains(trimmed)) {
                    updateTags(bookTags + trimmed)
                }
                isAdding = false
                showAddDialog = false
            },
        )

        DeleteConfirmationDialog(
            show = showDeleteDialog && tagToDelete != null,
            title = strings.dialogDeleteConfirm,
            summary = tagToDelete?.let { strings.bookTagsDeleteConfirm.format(it) } ?: "",
            isLoading = isDeleting,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                val targetTag = tagToDelete
                if (targetTag != null) {
                    isDeleting = true
                    updateTags(bookTags.filter { it != targetTag })
                    isDeleting = false
                }
                showDeleteDialog = false
            },
        )
    }
}

/**
 * 添加标签对话框。
 */
@Composable
private fun AddTagDialog(
    show: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var name by remember(show) { mutableStateOf("") }

    OverlayDialog(
        title = strings.dialogAddTag,
        summary = strings.dialogAddTagSummary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = strings.tags,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = strings.cancel,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = if (isLoading) strings.btnAdding else strings.btnAdd,
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) {
                        onConfirm(trimmed)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/**
 * 删除确认对话框。
 */
@Composable
private fun DeleteConfirmationDialog(
    show: Boolean,
    title: String,
    summary: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    OverlayDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = strings.cancel,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = if (isLoading) strings.btnDeleting else strings.btnDelete,
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
