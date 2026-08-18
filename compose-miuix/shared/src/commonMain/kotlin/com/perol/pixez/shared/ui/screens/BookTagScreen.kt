package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.settings.SettingsRepository
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 收藏标签页：管理本地收藏标签列表，点击标签可跳转搜索。
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
    var isDeleting by remember { mutableStateOf(false) }

    // 排序操作期间禁用按钮。
    var isReordering by remember { mutableStateOf(false) }

    // 空列表占位提示。
    val isEmpty = bookTags.isEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "收藏标签",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
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
                            contentDescription = "添加标签",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(text = "标签列表")
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    if (isEmpty) {
                        Text(
                            text = "暂无收藏标签，点击右上角添加",
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
                                                contentDescription = "上移",
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
                                                contentDescription = "下移",
                                            )
                                        }
                                        IconButton(
                                            onClick = { tagToDelete = tag },
                                            enabled = !isReordering,
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Delete,
                                                contentDescription = "删除",
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

        val pendingDelete = tagToDelete
        if (pendingDelete != null) {
            DeleteConfirmationDialog(
                title = "删除收藏标签",
                summary = "确定删除标签「$pendingDelete」吗？",
                isLoading = isDeleting,
                onDismiss = { tagToDelete = null },
                onConfirm = {
                    isDeleting = true
                    updateTags(bookTags.filter { it != pendingDelete })
                    isDeleting = false
                    tagToDelete = null
                },
            )
        }
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
        show = true,
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
