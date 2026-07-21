package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.runCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 屏蔽设置页：管理本地屏蔽相关开关与列表入口。
 *
 * M48 已实现 AI 作品过滤开关；M49 新增屏蔽标签的展示、添加与删除。
 */
@Composable
fun ShieldScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository,
    banRepository: BanRepository,
) {
    val coroutineScope = rememberCoroutineScope()

    // AI 作品过滤开关状态。
    var banAIIllust by remember { mutableStateOf(settingsRepository.banAIIllust) }

    // 屏蔽标签列表，进入页面时加载一次。
    var banTags by remember { mutableStateOf<List<BanRepository.BanTag>>(emptyList()) }
    var isLoadingTags by remember { mutableStateOf(false) }
    var isAddingTag by remember { mutableStateOf(false) }
    var isDeletingTag by remember { mutableStateOf(false) }

    // 添加 / 删除对话框状态。
    var showAddDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<BanRepository.BanTag?>(null) }

    // 提示信息。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * 加载全部屏蔽标签，并按名称字典序排序。
     */
    fun loadTags() {
        coroutineScope.launch {
            isLoadingTags = true
            runCatchingNonCancel { banRepository.getAllBanTags() }
                .onSuccess {
                    banTags = it.sortedBy { tag -> tag.name.lowercase() }
                }
                .onFailure { e ->
                    Napier.e("加载屏蔽标签失败", e)
                    toastMessage = "加载标签失败：${e.message}"
                }
            isLoadingTags = false
        }
    }

    LaunchedEffect(banRepository) {
        loadTags()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "屏蔽设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
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
                SmallTitle(text = "AI 作品")
            }
            item {
                BasicComponent(
                    title = "使带有 AI 生成标记的作品不可见",
                    summary = if (banAIIllust) "已开启" else "已关闭",
                    endActions = {
                        Switch(
                            checked = banAIIllust,
                            onCheckedChange = { checked ->
                                banAIIllust = checked
                                settingsRepository.banAIIllust = checked
                            },
                        )
                    },
                )
            }

            item {
                SmallTitle(text = "标签")
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "已屏蔽 ${banTags.size} 个标签",
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    )
                    IconButton(
                        onClick = { showAddDialog = true },
                        enabled = !isLoadingTags && !isAddingTag,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "添加标签",
                        )
                    }
                }
            }
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    banTags.forEach { tag ->
                        TagChip(
                            name = tag.name,
                            onClick = { tagToDelete = tag },
                        )
                    }
                }
            }
        }

        // 添加标签对话框。
        AddTagDialog(
            show = showAddDialog,
            isLoading = isAddingTag,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                coroutineScope.launch {
                    isAddingTag = true
                    runCatchingNonCancel {
                        banRepository.insertBanTag(name, translateName = "")
                    }.onSuccess {
                        showAddDialog = false
                        loadTags()
                    }.onFailure { e ->
                        Napier.e("添加屏蔽标签失败", e)
                        toastMessage = "添加失败：${e.message}"
                    }
                    isAddingTag = false
                }
            },
        )

        // 删除标签确认对话框。
        val pendingDelete = tagToDelete
        if (pendingDelete != null) {
            SuperDialog(
                title = "删除屏蔽标签",
                summary = "确定删除标签「${pendingDelete.name}」吗？",
                show = true,
                onDismissRequest = { tagToDelete = null },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        text = "取消",
                        onClick = { tagToDelete = null },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "删除",
                        onClick = {
                            coroutineScope.launch {
                                isDeletingTag = true
                                runCatchingNonCancel {
                                    banRepository.deleteBanTag(pendingDelete.id)
                                }.onSuccess {
                                    tagToDelete = null
                                    loadTags()
                                }.onFailure { e ->
                                    Napier.e("删除屏蔽标签失败", e)
                                    toastMessage = "删除失败：${e.message}"
                                }
                                isDeletingTag = false
                            }
                        },
                        enabled = !isDeletingTag,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

/**
 * 标签 chip：使用次要按钮样式，点击触发删除确认。
 */
@Composable
private fun TagChip(
    name: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(),
        minHeight = 32.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = name,
            style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote2,
        )
    }
}

/**
 * 添加标签对话框，包含标签名输入框与确认/取消按钮。
 */
@Composable
private fun AddTagDialog(
    show: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(show) { mutableStateOf("") }

    SuperDialog(
        title = "添加屏蔽标签",
        summary = "支持普通标签或正则表达式，正则请以 r' 开头、以 ' 结尾。",
        show = show,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = "标签名",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = if (isLoading) "添加中…" else "添加",
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
