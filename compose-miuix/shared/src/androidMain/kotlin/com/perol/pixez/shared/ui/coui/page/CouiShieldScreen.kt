package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.perol.pixez.shared.data.repository.BanRepository
import com.perol.pixez.shared.ui.LocalBanRepository
import com.perol.pixez.shared.ui.LocalSettingsRepository
import com.perol.pixez.shared.ui.LocalUserRepository
import com.perol.pixez.shared.ui.components.LocalBottomBarContentPadding
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiAssistChip
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiNavigationRow
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiTextAction
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPrimaryActionColors
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.i18n.format
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/** 删除目标：标签、画师或作品。 */
private sealed class CouiShieldDeleteTarget {
    data class Tag(val tag: BanRepository.BanTag) : CouiShieldDeleteTarget()
    data class User(val user: BanRepository.BanUser) : CouiShieldDeleteTarget()
    data class Illust(val illust: BanRepository.BanIllust) : CouiShieldDeleteTarget()
}

/**
 * COUI 屏蔽设置页：固定顶栏 + 分组卡片。
 *
 * 敏感内容/AI 过滤开关、AI 显示设置入口、屏蔽标签的添加与删除、
 * 屏蔽画师与作品的展示与删除全部保留；差异只有皮肤层：
 * 开关改用 COUI `Switch`，添加标签与删除确认改用 COUI `Dialog`，
 * 分组与列表收敛到平板可读宽度。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CouiShieldScreen(
    onBack: () -> Unit,
    onAISettingClick: (showAI: Boolean) -> Unit,
) {
    val settingsRepository = LocalSettingsRepository.current
    val banRepository = LocalBanRepository.current
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
                text = strings.settingShield,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
                // 1. 分级与敏感内容过滤
                item(key = "coui_shield_privacy", contentType = "section") {
                    CouiSettingSection(title = strings.settingPrivacy) {
                        CouiSwitchRow(
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
                        CouiSwitchRow(
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
                        CouiSwitchRow(
                            title = strings.banAIIllust,
                            summary = strings.banAIIllustSummary,
                            checked = banAIIllust,
                            onCheckedChange = { checked ->
                                banAIIllust = checked
                                settingsRepository.banAIIllust = checked
                            },
                        )
                        CouiSwitchRow(
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
                        CouiNavigationRow(
                            title = strings.userAISettings,
                            summary = if (isLoadingAISetting) strings.loading else strings.userAISettings,
                            onClick = {
                                if (isLoadingAISetting) return@CouiNavigationRow
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
                            CouiTextAction(
                                text = strings.btnAdd,
                                enabled = !isLoading && !isAddingTag,
                                color = CouiTheme.colorScheme.accent,
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
                    CouiSettingSection(title = strings.includedWorks) {
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
                            showAddDialog = false
                            loadAll()
                        }.onFailure { e ->
                            Napier.e("添加屏蔽标签失败", e)
                            toastSink(
                                ToastData(
                                    "${strings.btnAdd}${strings.failedSuffix}: " +
                                        PixivApiErrorFormatter.format(e, strings),
                                    ToastType.Error,
                                ),
                            )
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
                                loadAll()
                            }.onFailure { e ->
                                Napier.e("删除屏蔽项失败", e)
                                toastSink(
                                    ToastData(
                                        "${strings.btnDelete}${strings.failedSuffix}: " +
                                            PixivApiErrorFormatter.format(e, strings),
                                        ToastType.Error,
                                    ),
                                )
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

/** 添加屏蔽标签弹窗：COUI 无文本输入原语，用 `BasicTextField` 套官方容器样式自绘。 */
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
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                kind = CouiTextKind.ItemSummary,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalCouiTextStyle.current.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
