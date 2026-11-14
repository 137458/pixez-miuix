package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiTextStyle
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.format
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore

/**
 * COUI 收藏标签页：固定顶栏 + 标签卡片。
 *
 * 收藏标签的展示、点击搜索、上移/下移排序、添加与删除确认全部保留；
 * 差异只有皮肤层：顶栏图标按钮、添加/删除确认改用 COUI Dialog，
 * 列表收敛到平板可读宽度。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CouiTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CouiActionButton(
                icon = MiuixIcons.Back,
                contentDescription = strings.back,
                onClick = onBack,
            )
            Text(
                text = strings.settingBookTags,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CouiActionButton(
                icon = MiuixIcons.Add,
                contentDescription = strings.btnAdd,
                onClick = { showAddDialog = true },
            )
        }

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = LocalBottomBarContentPadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "coui_book_tag_list", contentType = "section") {
                    CouiSettingSection(title = strings.tags) {
                        if (bookTags.isEmpty()) {
                            Text(
                                text = strings.bookTagsEmpty,
                                kind = CouiTextKind.ItemSummary,
                                color = CouiTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            )
                        } else {
                            bookTags.forEachIndexed { index, tag ->
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
    val pressInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .couiPressFeedback(pressInteraction)
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null,
                    onClick = onSearch,
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = tag,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CouiActionButton(
            icon = MiuixIcons.ExpandLess,
            contentDescription = LocalStrings.current.bookTagsMoveUp,
            onClick = onMoveUp,
            enabled = canMoveUp,
        )
        CouiActionButton(
            icon = MiuixIcons.ExpandMore,
            contentDescription = LocalStrings.current.bookTagsMoveDown,
            onClick = onMoveDown,
            enabled = canMoveDown,
        )
        CouiActionButton(
            icon = MiuixIcons.Delete,
            contentDescription = LocalStrings.current.btnDelete,
            onClick = onDelete,
        )
    }
}

/** 添加标签弹窗：COUI 无文本输入原语，用 `BasicTextField` 套官方容器样式自绘。 */
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
        val scheme = CouiTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.container)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (name.isEmpty()) {
                Text(
                    text = strings.tags,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
