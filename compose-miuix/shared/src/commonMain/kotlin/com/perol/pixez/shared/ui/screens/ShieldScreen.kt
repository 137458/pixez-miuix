package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.data.repository.UserRepository
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.components.ToastMessage
import com.perol.pixez.shared.ui.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/**
 * 删除目标：标签、画师或作品。
 */
private sealed class DeleteTarget {
    data class Tag(val tag: BanRepository.BanTag) : DeleteTarget()
    data class User(val user: BanRepository.BanUser) : DeleteTarget()
    data class Illust(val illust: BanRepository.BanIllust) : DeleteTarget()
}

/**
 * 屏蔽设置页：管理本地屏蔽相关开关与列表入口。
 *
 * M48 已实现 AI 作品过滤开关；M49 新增屏蔽标签的展示、添加与删除；
 * M50 补齐被屏蔽画师与作品的展示、删除。
 */
@Composable
fun ShieldScreen(
    onBack: () -> Unit,
    onAISettingClick: (showAI: Boolean) -> Unit,
    settingsRepository: SettingsRepository,
    banRepository: BanRepository,
    userRepository: UserRepository,
) {
    val coroutineScope = rememberCoroutineScope()

    // AI 作品过滤开关状态。
    var banAIIllust by remember { mutableStateOf(settingsRepository.banAIIllust) }

    // AI 作品显示设置入口加载态，防止重复点击。
    var isLoadingAISetting by remember { mutableStateOf(false) }

    // 屏蔽列表：标签、画师、作品。
    var banTags by remember { mutableStateOf<List<BanRepository.BanTag>>(emptyList()) }
    var banUsers by remember { mutableStateOf<List<BanRepository.BanUser>>(emptyList()) }
    var banIllusts by remember { mutableStateOf<List<BanRepository.BanIllust>>(emptyList()) }

    // 初始加载态与分组操作加载态。
    var isLoading by remember { mutableStateOf(false) }
    var isAddingTag by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // 添加 / 删除对话框状态。
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current

    // 提示信息。
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * 同时加载标签、画师、作品三类屏蔽数据，并按名称字典序排序。
     */
    fun loadAll() {
        coroutineScope.launch {
            isLoading = true
            // 当前处于协程 launch 挂起上下文，需要调用挂起函数，使用 suspendRunCatchingNonCancel 捕获异常并保留取消语义。
            suspendRunCatchingNonCancel {
                Triple(
                    banRepository.getAllBanTags(),
                    banRepository.getAllBanUsers(),
                    banRepository.getAllBanIllusts(),
                )
            }.onSuccess { (tags, users, illusts) ->
                banTags = tags.sortedBy { it.name.lowercase() }
                banUsers = users.sortedBy { it.name.lowercase() }
                banIllusts = illusts.sortedBy { it.name.lowercase() }
            }.onFailure { e ->
                Napier.e("加载屏蔽数据失败", e)
                toastMessage = "${strings.loadFailed}: ${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAll()
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = strings.settingShield,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = strings.back,
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
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = AppConstants.Layout.TABLET_CONTENT_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    SmallTitle(text = strings.filterAi)
                    top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        top.yukonga.miuix.kmp.preference.SwitchPreference(
                            title = strings.banAIIllust,
                            summary = if (banAIIllust) strings.saveAfterStarSummaryOn else strings.saveAfterStarSummaryOff,
                            checked = banAIIllust,
                            onCheckedChange = { checked ->
                                banAIIllust = checked
                                settingsRepository.banAIIllust = checked
                            },
                        )
                        top.yukonga.miuix.kmp.preference.ArrowPreference(
                            title = strings.userAISettings,
                            summary = if (isLoadingAISetting) strings.loading else strings.userAISettings,
                            onClick = {
                                if (isLoadingAISetting) return@ArrowPreference
                                coroutineScope.launch {
                                    isLoadingAISetting = true
                                    suspendRunCatchingNonCancel {
                                        userRepository.getUserAISettings()
                                    }.onSuccess { response ->
                                        onAISettingClick(response.showAI)
                                    }.onFailure { e ->
                                        Napier.e("加载 AI 显示设置失败", e)
                                        toastMessage = "${strings.loadFailed}: ${e.message}"
                                    }
                                    isLoadingAISetting = false
                                }
                            },
                        )
                    }
                }

            // 标签分组：展示、添加、删除。
            item {
                SmallTitle(text = strings.tags)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${strings.banTagCount.replace("%d", banTags.size.toString())}",
                            style = MiuixTheme.textStyles.body2,
                        )
                        IconButton(
                            onClick = { showAddDialog = true },
                            enabled = !isLoading && !isAddingTag,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = strings.btnAdd,
                            )
                        }
                    }
                    ChipFlowRow(
                        items = banTags,
                        label = { it.name },
                        onClick = { deleteTarget = DeleteTarget.Tag(it) },
                    )
                }
            }

            // 画师分组：仅展示与删除。
            item {
                SmallTitle(text = strings.author)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        text = "${strings.banUserCount.replace("%d", banUsers.size.toString())}",
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    ChipFlowRow(
                        items = banUsers,
                        label = { it.name },
                        onClick = { deleteTarget = DeleteTarget.User(it) },
                    )
                }
            }

            // 作品分组：仅展示与删除。
            item {
                SmallTitle(text = strings.includedWorks)
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        text = "${strings.banIllustCount.replace("%d", banIllusts.size.toString())}",
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    ChipFlowRow(
                        items = banIllusts,
                        label = { it.name },
                        onClick = { deleteTarget = DeleteTarget.Illust(it) },
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
                    suspendRunCatchingNonCancel {
                        banRepository.insertBanTag(name, translateName = "")
                    }.onSuccess {
                        showAddDialog = false
                        loadAll()
                    }.onFailure { e ->
                        Napier.e("添加屏蔽标签失败", e)
                        toastMessage = "${strings.btnAdd}${strings.loadFailed}：${e.message}"
                    }
                    isAddingTag = false
                }
            },
        )

        // 通用删除确认对话框。
        val pendingDelete = deleteTarget
        if (pendingDelete != null) {
            val (title, summary) = when (pendingDelete) {
                is DeleteTarget.Tag -> strings.shieldDeleteTagTitle to strings.shieldDeleteTagConfirm.format(pendingDelete.tag.name)
                is DeleteTarget.User -> strings.shieldDeleteUserTitle to strings.shieldDeleteUserConfirm.format(pendingDelete.user.name)
                is DeleteTarget.Illust -> strings.shieldDeleteIllustTitle to strings.shieldDeleteIllustConfirm.format(pendingDelete.illust.name)
            }
            DeleteConfirmationDialog(
                title = title,
                summary = summary,
                isLoading = isDeleting,
                onDismiss = { deleteTarget = null },
                onConfirm = {
                    coroutineScope.launch {
                        isDeleting = true
                        val result = suspendRunCatchingNonCancel {
                            when (pendingDelete) {
                                is DeleteTarget.Tag -> banRepository.deleteBanTag(pendingDelete.tag.id)
                                is DeleteTarget.User -> banRepository.deleteBanUser(pendingDelete.user.id)
                                is DeleteTarget.Illust -> banRepository.deleteBanIllust(pendingDelete.illust.id)
                            }
                        }
                        result.onSuccess {
                            deleteTarget = null
                            loadAll()
                        }.onFailure { e ->
                            Napier.e("删除屏蔽项失败", e)
                            toastMessage = "${strings.btnDelete}${strings.loadFailed}：${e.message}"
                        }
                        isDeleting = false
                    }
                },
            )
        }

        ToastMessage(
            message = toastMessage,
            onDismiss = { toastMessage = null },
        )
    }
}

/**
 * 通用 chip 流式布局：按给定标签函数展示列表项，点击触发回调。
 */
@Composable
private fun <T> ChipFlowRow(
    items: List<T>,
    label: (T) -> String,
    onClick: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Chip(
                name = label(item),
                onClick = { onClick(item) },
            )
        }
    }
}

/**
 * 通用 chip：使用次要按钮样式，点击触发删除确认。
 */
@Composable
private fun Chip(
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
            style = MiuixTheme.textStyles.footnote2,
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
    val strings = com.perol.pixez.shared.ui.i18n.LocalStrings.current
    var name by remember(show) { mutableStateOf("") }

    OverlayDialog(
        title = strings.shieldAddTagTitle,
        summary = strings.shieldAddTagSummary,
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
 * 通用删除确认对话框。
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
