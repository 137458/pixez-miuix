package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.AccountPersist
import com.perol.pixez.shared.platform.IllustClipboard
import com.perol.pixez.shared.ui.AppConstants
import com.perol.pixez.shared.ui.LocalAccountRepository
import com.perol.pixez.shared.ui.components.LocalToastSink
import com.perol.pixez.shared.ui.components.PixivApiErrorFormatter
import com.perol.pixez.shared.ui.components.ToastData
import com.perol.pixez.shared.ui.components.ToastType
import com.perol.pixez.shared.ui.components.runClipboardShare
import com.perol.pixez.shared.ui.coui.component.CouiActionButton
import com.perol.pixez.shared.ui.coui.component.CouiCard
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.couiDialogMaterialOrNull
import com.perol.pixez.shared.ui.coui.component.couiDialogPlacement
import com.perol.pixez.shared.ui.coui.component.couiPressFeedback
import com.perol.pixez.shared.ui.i18n.LocalStrings
import com.perol.pixez.shared.ui.screens.EMAIL_REGEX
import com.perol.pixez.shared.ui.utils.openSafeUrl
import com.perol.pixez.shared.utils.suspendRunCatchingNonCancel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Show

/**
 * COUI 账号编辑页：账号信息、密码/邮箱修改、Refresh Token 复制与账号注销入口。
 *
 * 校验顺序、成功/失败提示、密码可见性切换与敏感字段不写入 Saved Instance State 的约定
 * 都与 MIUIX 版一致；差异只有皮肤层：输入框与保存按钮按 COUI 令牌自绘，
 * 注销二次确认改用 COUI `Dialog`，内容收敛到平板可读宽度。
 */
@Composable
internal fun CouiAccountEditScreen(
    onBack: () -> Unit,
) {
    val accountRepository = LocalAccountRepository.current
    val strings = LocalStrings.current
    val toastSink = LocalToastSink.current
    val coroutineScope = rememberCoroutineScope()
    val clipboard = remember { IllustClipboard() }

    var account by remember { mutableStateOf<AccountPersist?>(null) }

    // 密码使用 remember：避免明文密码进入 Saved Instance State。
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // 邮箱属于账号敏感信息，同样不写入 Saved Instance State。
    var email by remember { mutableStateOf("") }
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeletionConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        suspendRunCatchingNonCancel {
            accountRepository.currentAccount()
        }
            .onSuccess { active ->
                account = active
                email = active?.mailAddress.orEmpty()
            }
            .onFailure { e ->
                Napier.e("加载账号信息失败", e)
                toastSink(ToastData(PixivApiErrorFormatter.format(e, strings), ToastType.Error))
            }
    }

    fun save() {
        if (isSaving) return
        if (currentPassword.isBlank()) {
            toastSink(ToastData(strings.accountEditInputCurrentPassword, ToastType.Error))
            return
        }
        if (email.isBlank()) {
            toastSink(ToastData(strings.accountEditInputEmail, ToastType.Error))
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            toastSink(ToastData(strings.accountEditEmailFormatError, ToastType.Error))
            return
        }

        coroutineScope.launch {
            try {
                isSaving = true
                suspendRunCatchingNonCancel {
                    accountRepository.editAccount(
                        currentPassword = currentPassword,
                        newPassword = newPassword.takeIf { it.isNotBlank() },
                        newMailAddress = email.takeIf { it.isNotBlank() },
                    )
                }.onSuccess {
                    toastSink(ToastData(strings.accountEditSaveSuccess, ToastType.Success))
                    currentPassword = ""
                    newPassword = ""
                }.onFailure { e ->
                    toastSink(
                        ToastData(
                            "${strings.accountEditSaveFailed}：${PixivApiErrorFormatter.format(e, strings)}",
                            ToastType.Error,
                        ),
                    )
                    Napier.e("保存账号信息失败", e)
                }
            } finally {
                isSaving = false
            }
        }
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
                text = strings.accountEditTitle,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        CouiReadableColumn(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = rememberCouiOverscrollEffect(),
            ) {
                item(key = "account_info") {
                    CouiCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = account?.name.orEmpty(),
                                kind = CouiTextKind.ItemTitle,
                                color = CouiTheme.colorScheme.onSurface,
                            )
                            CouiAccountEditField(
                                value = account?.account.orEmpty(),
                                onValueChange = {},
                                placeholder = strings.pixivId,
                                enabled = false,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                item(key = "account_form") {
                    CouiAccountEditSectionTitle(strings.accountEditSectionInfo)
                    CouiCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CouiAccountEditPasswordField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                placeholder = strings.accountEditCurrentPassword,
                                visible = currentPasswordVisible,
                                onToggleVisibility = { currentPasswordVisible = !currentPasswordVisible },
                            )
                            CouiAccountEditPasswordField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                placeholder = strings.accountEditNewPassword,
                                visible = newPasswordVisible,
                                onToggleVisibility = { newPasswordVisible = !newPasswordVisible },
                            )
                            CouiAccountEditField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = strings.accountEditEmail,
                                enabled = true,
                            )
                            CouiAccountEditButton(
                                text = if (isSaving) strings.accountEditSaving else strings.accountEditSave,
                                enabled = !isSaving,
                                primary = true,
                                onClick = ::save,
                            )
                        }
                    }
                }

                // 仅已邮箱认证账号展示 Refresh Token 复制入口。
                if (account?.isMailAuthorized == 1) {
                    item(key = "account_security") {
                        CouiAccountEditSectionTitle(strings.accountEditSectionSecurity)
                        CouiCard {
                            CouiAccountEditRow(
                                title = strings.accountEditCopyRefreshToken,
                                summary = strings.accountEditCopyRefreshTokenSummary,
                                onClick = {
                                    account?.refreshToken?.let { token ->
                                        runClipboardShare(
                                            success = ToastData(strings.copiedToClipboard, ToastType.Success),
                                            failurePrefix = "${strings.copy}${strings.failedSuffix}",
                                            onToast = toastSink,
                                        ) { clipboard.copy(token) }.onFailure { e ->
                                            Napier.e("复制 refresh token 失败", e)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                item(key = "account_danger") {
                    CouiAccountEditSectionTitle(strings.accountEditSectionDanger)
                    CouiCard {
                        CouiAccountEditRow(
                            title = strings.accountEditLogoutTitle,
                            summary = strings.accountEditLogoutSummary,
                            onClick = { showDeletionConfirm = true },
                        )
                    }
                }
            }
        }
    }

    if (showDeletionConfirm) {
        Dialog(
            onDismissRequest = { showDeletionConfirm = false },
            visible = true,
            material = couiDialogMaterialOrNull(),
            title = strings.accountEditLogoutTitle,
            message = strings.accountEditLogoutConfirm,
            placement = couiDialogPlacement(),
            confirmAction = CouiDialogAction(
                text = strings.confirm,
                style = CouiDialogActionStyle.Primary,
                onClick = {
                    showDeletionConfirm = false
                    openSafeUrl(AppConstants.Urls.PIXIV_LEAVE_ACCOUNT, strings, onError = { message ->
                        toastSink(ToastData(message, ToastType.Error))
                    })
                },
            ),
            dismissAction = CouiDialogAction(
                text = strings.cancel,
                onClick = { showDeletionConfirm = false },
            ),
        )
    }
}

/** 分组小标题。 */
@Composable
private fun CouiAccountEditSectionTitle(text: String) {
    Text(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = CouiTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
    )
}

/** 只读/普通文本输入框：容器胶囊 + 占位文字。 */
@Composable
private fun CouiAccountEditField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) scheme.container else scheme.card)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
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
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(scheme.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 密码输入框：视觉变换 + 可见性切换。 */
@Composable
private fun CouiAccountEditPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.container)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
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
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(scheme.accent),
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val pressInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .couiPressFeedback(pressInteraction)
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null,
                    onClick = onToggleVisibility,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = rememberVectorPainter(if (visible) MiuixIcons.Hide else MiuixIcons.Show),
                contentDescription = if (visible) {
                    strings.accountEditHidePassword
                } else {
                    strings.accountEditShowPassword
                },
                colorFilter = ColorFilter.tint(scheme.onSurfaceVariant),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 主要动作按钮：COUI 无按钮原语，按令牌自绘胶囊。 */
@Composable
private fun CouiAccountEditButton(
    text: String,
    enabled: Boolean,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    !enabled -> scheme.container
                    primary -> scheme.accent
                    else -> scheme.container
                },
            )
            .couiPressFeedback(interactionSource, enabled = enabled, list = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = when {
                !enabled -> scheme.onSurfaceVariant
                primary -> scheme.onAccent
                else -> scheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** 设置式行：标题 + 摘要，整行可点。 */
@Composable
private fun CouiAccountEditRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = CouiTheme.colorScheme.onSurface,
        )
        Text(
            text = summary,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
