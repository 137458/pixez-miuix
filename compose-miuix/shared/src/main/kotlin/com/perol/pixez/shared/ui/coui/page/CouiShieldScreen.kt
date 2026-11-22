package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.actionFailureToast
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.coui.component.CouiTextInput
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiTextButton
import com.perol.pixez.shared.ui.coui.core.CouiToggleRow
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

/** 删除目标：标签、画师或作品。 */
private sealed class CouiShieldDeleteTarget {
    data class Tag(val tag: BanRepository.BanTag) : CouiShieldDeleteTarget()
    data class User(val user: BanRepository.BanUser) : CouiShieldDeleteTarget()
    data class Illust(val illust: BanRepository.BanIllust) : CouiShieldDeleteTarget()
}

/**
 * COUI 屏蔽设置页：折叠大标题顶栏 + 分级过滤 / AI 内容 / 标签 / 画师 / 作品五组。
 *
 * 骨架改用 [CouiSettingsScaffold]：居上限宽、overscroll、底部悬浮底栏留白与顶栏覆盖区
 * 补偿都由骨架代付，页面不再自铺底色、不加状态栏内边距、也不再二次扣底部安全区。
 * 组内开关行换 core 的 [CouiToggleRow]，跳转行换 [CouiValueRow]（副标题走 summary、
 * onClick 自动挂 Chevron），行间共用 [CouiRowDivider]；只有屏蔽项胶囊流、以及"计数 + 添加"
 * 这类复合头行是纯排版块，无对应行原语。COUI 库没有胶囊按钮与输入控件，添加按钮继续走
 * [CouiTextButton]，弹窗输入框继续走 CouiTextInput。
 *
 * 数据接线不动：屏蔽项仍由 BanRepository 全量读取后按 name 小写排序，增删后统一 loadAll()
 * 刷新，失败经 Napier 记录并用 PixivApiErrorFormatter / actionFailureToast 转成错误提示；
 * AI 显示设置走 UserRepository 并把 showAI 回传给宿主的 onAISettingClick。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiShieldScreen(
    onBack: () -> Unit,
    onAISettingClick: (showAI: Boolean) -> Unit,
) {
    val banRepository = LocalBanRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val userRepository = LocalUserRepository.current
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current

    var hIsNotAllow by remember { mutableStateOf(settingsRepository.hIsNotAllow) }
    var nsfwMask by remember { mutableStateOf(settingsRepository.nsfwMask) }
    var banAIIllust by remember { mutableStateOf(settingsRepository.banAIIllust) }
    var feedAIBadge by remember { mutableStateOf(settingsRepository.feedAIBadge) }

    var isLoadingAISetting by remember { mutableStateOf(false) }

    var banTags by remember { mutableStateOf<List<BanRepository.BanTag>>(emptyList()) }
    var banUsers by remember { mutableStateOf<List<BanRepository.BanUser>>(emptyList()) }
    var banIllusts by remember { mutableStateOf<List<BanRepository.BanIllust>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isAddingTag by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CouiShieldDeleteTarget?>(null) }

    /** 加载全量屏蔽数据（标签、画师、作品）。 */
    fun loadAll() {
        coroutineScope.launch {
            isLoading = true
            try {
                suspendRunCatchingNonCancel {
                    val tags = banRepository.getAllBanTags()
                    val users = banRepository.getAllBanUsers()
                    val illusts = banRepository.getAllBanIllusts()
                    Triple(tags, users, illusts)
                }.onSuccess { (tags, users, illusts) ->
                    banTags = tags.sortedBy { it.name.lowercase() }
                    banUsers = users.sortedBy { it.name.lowercase() }
                    banIllusts = illusts.sortedBy { it.name.lowercase() }
                }.onFailure { e ->
                    Napier.e("加载屏蔽数据失败", e)
                    toastSink(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAll()
    }

    CouiSettingsScaffold(
        title = strings.settingShield,
        onBack = onBack,
    ) {
        // 1. 分级与敏感内容过滤
        item(key = "coui_shield_privacy", contentType = "section") {
            CouiSettingSection(title = strings.shieldSectionContentFilter) {
                CouiToggleRow(
                    title = strings.interactionSettingHNotAllow,
                    summary = if (hIsNotAllow) {
                        strings.interactionSettingHNotAllowSummaryOn
                    } else {
                        strings.interactionSettingHNotAllowSummaryOff
                    },
                    checked = hIsNotAllow,
                    onCheckedChange = { checked ->
                        hIsNotAllow = checked
                        settingsRepository.hIsNotAllow = checked
                    },
                )
                CouiRowDivider()
                CouiToggleRow(
                    title = strings.nsfwMask,
                    summary = if (nsfwMask) strings.nsfwMaskSummaryOn else strings.nsfwMaskSummaryOff,
                    checked = nsfwMask,
                    onCheckedChange = { checked ->
                        nsfwMask = checked
                        settingsRepository.nsfwMask = checked
                    },
                )
            }
        }

        // 2. AI 生成内容控制
        item(key = "coui_shield_ai", contentType = "section") {
            CouiSettingSection(title = strings.filterAi) {
                CouiToggleRow(
                    title = strings.banAIIllust,
                    summary = strings.banAIIllustSummary,
                    checked = banAIIllust,
                    onCheckedChange = { checked ->
                        banAIIllust = checked
                        settingsRepository.banAIIllust = checked
                    },
                )
                CouiRowDivider()
                CouiToggleRow(
                    title = strings.feedSettingAiBadge,
                    summary = if (feedAIBadge) {
                        strings.feedSettingAiBadgeSummaryOn
                    } else {
                        strings.feedSettingAiBadgeSummaryOff
                    },
                    checked = feedAIBadge,
                    onCheckedChange = { checked ->
                        feedAIBadge = checked
                        settingsRepository.feedAIBadge = checked
                    },
                )
                CouiRowDivider()
                CouiValueRow(
                    title = strings.userAISettings,
                    summary = if (isLoadingAISetting) strings.loading else strings.userAISettingsSummary,
                    onClick = {
                        if (isLoadingAISetting) return@CouiValueRow
                        coroutineScope.launch {
                            isLoadingAISetting = true
                            suspendRunCatchingNonCancel {
                                userRepository.getUserAISettings()
                            }.onSuccess { response ->
                                onAISettingClick(response.showAI)
                            }.onFailure { e ->
                                Napier.e("加载 AI 显示设置失败", e)
                                toastSink(
                                    ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error),
                                )
                            }
                            isLoadingAISetting = false
                        }
                    },
                )
            }
        }

        // 3. 屏蔽标签：展示、添加、删除
        item(key = "coui_shield_tags", contentType = "section") {
            CouiSettingSection(title = strings.tags) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = strings.banTagCount.format(banTags.size),
                        kind = CouiTextKind.ItemSummary,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    CouiTextButton(
                        text = strings.btnAdd,
                        enabled = !isLoading && !isAddingTag,
                        filled = false,
                        onClick = { showAddDialog = true },
                    )
                }
                CouiShieldChips(
                    items = banTags,
                    label = { it.name },
                    onClick = { deleteTarget = CouiShieldDeleteTarget.Tag(it) },
                )
            }
        }

        // 4. 屏蔽画师
        item(key = "coui_shield_users", contentType = "section") {
            CouiSettingSection(title = strings.author) {
                Text(
                    text = strings.banUserCount.format(banUsers.size),
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                CouiShieldChips(
                    items = banUsers,
                    label = { it.name },
                    onClick = { deleteTarget = CouiShieldDeleteTarget.User(it) },
                )
            }
        }

        // 5. 屏蔽作品
        item(key = "coui_shield_illusts", contentType = "section") {
            CouiSettingSection(title = strings.shieldSectionBannedWorks) {
                Text(
                    text = strings.banIllustCount.format(banIllusts.size),
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                CouiShieldChips(
                    items = banIllusts,
                    label = { it.name },
                    onClick = { deleteTarget = CouiShieldDeleteTarget.Illust(it) },
                )
            }
        }
    }

    if (showAddDialog) {
        CouiAddBanTagDialog(
            isAdding = isAddingTag,
            onDismiss = { if (!isAddingTag) showAddDialog = false },
            onConfirm = { name ->
                coroutineScope.launch {
                    isAddingTag = true
                    try {
                        suspendRunCatchingNonCancel {
                            banRepository.insertBanTag(name, translateName = "")
                        }.onSuccess {
                            settingsRepository.notifyFilterChanged()
                            showAddDialog = false
                            loadAll()
                        }.onFailure { e ->
                            Napier.e("添加屏蔽标签失败", e)
                            toastSink(actionFailureToast(strings.btnAdd, e, strings))
                        }
                    } finally {
                        isAddingTag = false
                    }
                }
            },
        )
    }

    deleteTarget?.let { target ->
        val (title, summary) = when (target) {
            is CouiShieldDeleteTarget.Tag ->
                strings.shieldDeleteTagTitle to strings.shieldDeleteTagConfirm.format(target.tag.name)

            is CouiShieldDeleteTarget.User ->
                strings.shieldDeleteUserTitle to strings.shieldDeleteUserConfirm.format(target.user.name)

            is CouiShieldDeleteTarget.Illust ->
                strings.shieldDeleteIllustTitle to strings.shieldDeleteIllustConfirm.format(target.illust.name)
        }
        val deleting = isDeleting
        Dialog(
            onDismissRequest = { if (!deleting) deleteTarget = null },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = title,
            message = summary,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = if (deleting) strings.btnDeleting else strings.btnDelete,
                enabled = !deleting,
                style = CouiDialogActionStyle.Primary,
                colors = couiPrimaryActionColors(danger = true),
                onClick = {
                    deleteTarget = null
                    coroutineScope.launch {
                        isDeleting = true
                        try {
                            suspendRunCatchingNonCancel {
                                when (target) {
                                    is CouiShieldDeleteTarget.Tag -> banRepository.deleteBanTag(target.tag.id)
                                    is CouiShieldDeleteTarget.User -> banRepository.deleteBanUser(target.user.id)
                                    is CouiShieldDeleteTarget.Illust -> banRepository.deleteBanIllust(target.illust.id)
                                }
                            }.onSuccess {
                                settingsRepository.notifyFilterChanged()
                                loadAll()
                            }.onFailure { e ->
                                Napier.e("删除屏蔽项失败", e)
                                toastSink(actionFailureToast(strings.btnDelete, e, strings))
                            }
                        } finally {
                            isDeleting = false
                        }
                    }
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { if (!deleting) deleteTarget = null },
            ),
        )
    }
}

/** 屏蔽项胶囊流式布局：点击触发删除确认。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> CouiShieldChips(
    items: List<T>,
    label: (T) -> String,
    onClick: (T) -> Unit,
) {
    if (items.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            CouiAssistChip(
                text = label(item),
                selected = false,
                onClick = { onClick(item) },
            )
        }
    }
}

/** 添加屏蔽标签弹窗：输入框统一走 CouiTextInput，与全仓其它输入框保持同一观感。 */
@Composable
private fun CouiAddBanTagDialog(
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { if (!isAdding) onDismiss() },
        visible = true,
        material = couiDialogMaterialOrNull(),
        title = strings.shieldAddTagTitle,
        message = strings.shieldAddTagSummary,
        placement = couiDialogPlacement(),
        confirmAction = CouiDialogAction(
            text = if (isAdding) strings.btnAdding else strings.btnAdd,
            enabled = !isAdding,
            style = CouiDialogActionStyle.Primary,
            colors = couiPrimaryActionColors(),
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) onConfirm(trimmed)
            },
        ),
        dismissAction = CouiDialogAction(
            text = strings.cancel,
            onClick = { if (!isAdding) onDismiss() },
        ),
    ) {
        CouiShieldInputField(
            value = name,
            placeholder = strings.tags,
            onValueChange = { name = it },
        )
    }
}

/** 弹窗输入框：COUI 容器底色 + 胶囊圆角。 */
@Composable
private fun CouiShieldInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    CouiTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
    )
}
