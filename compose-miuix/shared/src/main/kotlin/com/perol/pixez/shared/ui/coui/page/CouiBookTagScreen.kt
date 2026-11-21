package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.layout.Item
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTopBarIconButton
import com.perol.pixez.shared.ui.i18n.format
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.coui.icon.CouiIcons

/**
 * COUI 收藏标签页：设置页骨架 + 标签分组。
 *
 * 收藏标签的展示、点击搜索、上移/下移排序、添加与删除确认全部保留；
 * 差异只有表现层：骨架收敛到 [CouiSettingsScaffold]，行改用官方 Group/Item 版式，
 * 添加与删除确认沿用 COUI `Dialog`。
 */
@Composable
internal fun CouiBookTagScreen(
    onBack: () -> Unit,
    onTagSearch: (String) -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val strings = LocalStrings.current

    var bookTags by remember { mutableStateOf(settingsRepository.bookTagList) }
    val updateTags: (List<String>) -> Unit = { newTags ->
        bookTags = newTags
        settingsRepository.bookTagList = newTags
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    // 排序操作期间禁用按钮，与 MIUIX 版逻辑一致。
    var isReordering by remember { mutableStateOf(false) }

    CouiSettingsScaffold(
        title = strings.settingBookTags,
        onBack = onBack,
        actions = {
            CouiTopBarIconButton(
                onClick = { showAddDialog = true },
                icon = CouiIcons.Add,
                contentDescription = strings.btnAdd,
            )
        },
    ) {
        item(key = "coui_book_tag_list", contentType = "section") {
            CouiSettingSection(title = strings.tags) {
                if (bookTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                    ) {
                        CouiItemSummary(text = strings.bookTagsEmpty)
                    }
                } else {
                    bookTags.forEachIndexed { index, tag ->
                        if (index > 0) {
                            CouiRowDivider()
                        }
                        CouiBookTagRow(
                            tag = tag,
                            canMoveUp = index > 0 && !isReordering,
                            canMoveDown = index < bookTags.lastIndex && !isReordering,
                            onSearch = { onTagSearch(tag) },
                            onMoveUp = {
                                isReordering = true
                                updateTags(
                                    bookTags.toMutableList().apply {
                                        add(index - 1, removeAt(index))
                                    },
                                )
                                isReordering = false
                            },
                            onMoveDown = {
                                isReordering = true
                                updateTags(
                                    bookTags.toMutableList().apply {
                                        add(index + 1, removeAt(index))
                                    },
                                )
                                isReordering = false
                            },
                            onDelete = { tagToDelete = tag },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CouiBookTagAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotBlank() && !bookTags.contains(trimmed)) {
                    updateTags(bookTags + trimmed)
                }
                showAddDialog = false
            },
        )
    }

    tagToDelete?.let { tag ->
        Dialog(
            onDismissRequest = { tagToDelete = null },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.dialogDeleteConfirm,
            message = strings.bookTagsDeleteConfirm.format(tag),
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.btnDelete,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    updateTags(bookTags.filter { it != tag })
                    tagToDelete = null
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { tagToDelete = null },
            ),
        )
    }
}

/** 标签行：点击标题搜索，尾部上移/下移/删除三个图标按钮。 */
@Composable
private fun CouiBookTagRow(
    tag: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSearch: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Item(
        title = {
            Text(
                text = tag,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        // 标题区整行可点即触发搜索，与底部三个排序/删除按钮不冲突。
        onClick = onSearch,
        trailing = {
            CouiActionButton(
                icon = CouiIcons.ExpandLess,
                contentDescription = LocalStrings.current.bookTagsMoveUp,
                onClick = onMoveUp,
                enabled = canMoveUp,
            )
            CouiActionButton(
                icon = CouiIcons.ExpandMore,
                contentDescription = LocalStrings.current.bookTagsMoveDown,
                onClick = onMoveDown,
                enabled = canMoveDown,
            )
            CouiActionButton(
                icon = CouiIcons.Delete,
                contentDescription = LocalStrings.current.btnDelete,
                onClick = onDelete,
            )
        },
    )
}

/** 添加标签弹窗：输入框统一走 CouiTextInput，与全仓其它输入框保持同一观感。 */
@Composable
private fun CouiBookTagAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.dialogAddTag,
        message = strings.dialogAddTagSummary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = strings.btnAdd,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) onConfirm(trimmed)
            },
        ),
        dismissAction = CouiDialogAction(
            text = strings.cancel,
            onClick = onDismiss,
        ),
    ) {
        CouiTextInput(
            value = name,
            onValueChange = { name = it },
            placeholder = strings.tags,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
